package com.lake.knowenginelearn.document.service;

import com.alibaba.fastjson2.JSON;
import com.lake.knowenginelearn.document.constant.DocumentStatus;
import com.lake.knowenginelearn.document.entity.KnowledgeDocument;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.util.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.InputStream;

/**
 * 文件处理服务 - 负责文档转换处理
 */
@Slf4j
@Service
public class FileProcessService {

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private KnowledgeDocumentService knowledgeDocumentService;

    @Value("${file.parse.api.url:http://localhost:8000}")
    private String fileParseApiUrl;

    @Value("${file.parse.api.connectTimeout:30000}")
    private int connectTimeout;

    @Value("${file.parse.api.responseTimeout:300000}")
    private int responseTimeout;

    /**
     * 处理文档转换
     * 1. 从 MinIO 下载文件
     * 2. 调用文档解析接口
     * 3. 更新文档状态和转换后的 URL
     *
     * @param document 文档ID
     */
    public void processDocument(KnowledgeDocument document) {
        log.info("开始处理文档转换，documentId: {}", document.getDocTitle());

        // 2. 更新状态为转换中
        document.setStatus(DocumentStatus.CONVERTING);
        knowledgeDocumentService.updateById(document);

        try {
            // 3. 从 MinIO 下载文件
            String objectName = extractObjectNameFromUrl(document.getDocUrl());
            InputStream fileStream = fileStorageService.downloadFile(objectName);

            // 生成一串数字，避免文件名的中文乱码
            String docTitle = document.getDocTitle() + "" + document.getDocTitle().hashCode();

            // 4. 调用文档解析
            String parseResult = callFileParseApi(docTitle, fileStream);

            String markdownContent = JSON.parseObject(parseResult).getJSONObject("results").getJSONObject(docTitle).getString("md_content");
            // 5. 保存转换后的内容到 MinIO（这里假设解析结果是文本或 JSON）
            String convertedObjectName = "converted/" + document.getDocTitle().substring(0, document.getDocTitle().lastIndexOf(".") + 1) + ".md";
            String convertedUrl = fileStorageService.uploadFile(
                    convertedObjectName,
                    markdownContent.getBytes(),
                    "application/json"
            );

            // 6. 更新文档状态为已转换
            document.setStatus(DocumentStatus.CONVERTED);
            document.setConvertedDocUrl(convertedUrl);
            knowledgeDocumentService.updateById(document);

            log.info("文档转换完成，documentId: {}", document.getDocTitle());

        } catch (Exception e) {
            log.error("文档转换失败，documentId: {}", document.getDocTitle(), e);
            // 转换失败，状态回滚为 UPLOADED
            document.setStatus(DocumentStatus.UPLOADED);
            knowledgeDocumentService.updateById(document);
            throw new RuntimeException("文档转换失败: " + e.getMessage(), e);
        }
    }

    /**
     * 调用文件解析接口
     * 使用 Apache HttpClient 5 替代 HttpURLConnection，提供更好的超时控制和连接管理
     *
     * @param fileName   文件名
     * @param fileStream 文件输入流
     * @return 解析结果
     */
    private String callFileParseApi(String fileName, InputStream fileStream) {
        String url = fileParseApiUrl + "/file_parse";

        // 配置请求超时
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(Timeout.ofMilliseconds(connectTimeout))
                .setResponseTimeout(Timeout.ofMilliseconds(responseTimeout))
                .build();

        try (CloseableHttpClient httpClient = HttpClients.custom()
                .setDefaultRequestConfig(requestConfig)
                .build()) {

            HttpPost httpPost = new HttpPost(url);
            httpPost.setHeader("Accept", "application/json");

            // 构建 multipart 请求体
            HttpEntity multipartEntity = MultipartEntityBuilder.create()
                    .addBinaryBody("files", fileStream, ContentType.APPLICATION_OCTET_STREAM, fileName)
                    .addTextBody("backend", "pipeline")
                    .addTextBody("response_format_zip", "false")
                    .addTextBody("return_images", "false")
                    .addTextBody("return_model_output", "false")
                    .addTextBody("return_middle_json", "false")
                    .build();

            httpPost.setEntity(multipartEntity);

            log.info("开始调用文件解析接口: {}", url);

            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                int statusCode = response.getCode();
                log.info("文件解析接口响应状态码: {}", statusCode);

                HttpEntity responseEntity = response.getEntity();
                String responseBody = responseEntity != null ? EntityUtils.toString(responseEntity, "UTF-8") : "";

                if (statusCode == 200) {
                    log.info("文件解析接口调用成功，响应体长度: {}", responseBody.length());
                    return responseBody;
                } else {
                    log.error("文件解析接口调用失败，状态码: {}, 响应: {}", statusCode, responseBody);
                    throw new RuntimeException("文件解析接口调用失败: HTTP " + statusCode + ", " + responseBody);
                }
            }

        } catch (Exception e) {
            log.error("调用文件解析接口异常", e);
            throw new RuntimeException("调用文件解析接口失败: " + e.getMessage(), e);
        } finally {
            // 确保文件流被关闭
            try {
                if (fileStream != null) {
                    fileStream.close();
                }
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * 从 URL 中提取 MinIO 对象名
     */
    private String extractObjectNameFromUrl(String url) {
        // URL 格式: http://endpoint/bucketName/objectName
        // 需要提取 bucketName 之后的部分
        try {
            java.net.URL urlObj = new java.net.URL(url);
            String path = urlObj.getPath();
            // 去掉开头的 /
            if (path.startsWith("/")) {
                path = path.substring(1);
            }
            // 找到第一个 / 后的内容就是 objectName
            int firstSlash = path.indexOf('/');
            if (firstSlash > 0) {
                return path.substring(firstSlash + 1);
            }
            return path;
        } catch (Exception e) {
            log.error("解析 URL 失败: {}", url, e);
            throw new RuntimeException("解析 URL 失败: " + url);
        }
    }
}
