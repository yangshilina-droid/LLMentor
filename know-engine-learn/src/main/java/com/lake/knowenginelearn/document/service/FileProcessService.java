package com.lake.knowenginelearn.document.service;

import com.alibaba.fastjson2.JSON;
import com.lake.knowenginelearn.document.constant.ContentType;
import com.lake.knowenginelearn.document.constant.DocumentStatus;
import com.lake.knowenginelearn.document.entity.KnowledgeDocument;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.util.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.io.InputStream;

/**
 * 文件处理服务 - 负责文档转换处理
 */
@Slf4j
@Service
public class FileProcessService {

    private static final String CONVERTED_FILE_DIR = "converted/";

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
     * 处理文档转换 - Markdown 格式
     * 1. 从 MinIO 下载文件
     * 2. 调用文档解析接口获取md/zip
     * 3. 转换后的文档保存在minio上
     * 3. 更新文档状态和转换后的 URL
     *
     * @param document 文档对象
     */
    //todo + distribute lock
    public void processDocument(KnowledgeDocument document, InputStream inputStream) {
        processDocumentToZip(document, inputStream);
    }

    /**
     * 处理文档转换为 Markdown 格式
     *
     * @param document 文档对象
     */
    public void processDocumentToMarkdown(KnowledgeDocument document, InputStream inputStream) {
        log.info("开始处理文档转换为 Markdown，documentId: {}", document.getDocTitle());

        // 更新状态为转换中
        document.setStatus(DocumentStatus.CONVERTING);
        boolean result = knowledgeDocumentService.updateById(document);
        Assert.isTrue(result, "文件CONVERTING状态更新失败");

        try {
            // 生成一串数字，避免文件名的中文乱码
            String docTitle = document.getDocTitle() + document.getDocTitle().hashCode();

            // 调用文档解析获取 Markdown
            String parseResult = parsePdfToMarkdown(docTitle, inputStream);

            String markdownContent = JSON.parseObject(parseResult).getJSONObject("results").getJSONObject(docTitle).getString("md_content");
            // 保存转换后的内容到 MinIO
            String convertedObjectName = CONVERTED_FILE_DIR + document.getDocTitle().substring(0, document.getDocTitle().lastIndexOf(".")) + ".md";
            String convertedUrl = fileStorageService.uploadFile(convertedObjectName, markdownContent.getBytes(), ContentType.TEXT_MARKDOWN);

            // 更新文档状态为已转换
            document.setStatus(DocumentStatus.CONVERTED);
            document.setConvertedDocUrl(convertedUrl);
            result = knowledgeDocumentService.updateById(document);
            Assert.isTrue(result, "文件CONVERTED状态更新失败");
            log.info("文档 Markdown 转换完成，documentId: {}", document.getDocTitle());
        } catch (Exception e) {
            log.error("文档 Markdown 转换失败，documentId: {}", document.getDocTitle(), e);
            // 转换失败，状态回滚为 UPLOADED
            document.setStatus(DocumentStatus.UPLOADED);
            result = knowledgeDocumentService.updateById(document);
            Assert.isTrue(result, "文件UPLOADED状态更新失败");
            throw new RuntimeException("文档 Markdown 转换失败: " + e.getMessage(), e);
        } finally {
            closeQuietly(inputStream);
        }
    }

    /**
     * 处理文档转换为 ZIP 格式
     * 1. 从 MinIO 下载文件
     * 2. 调用文档解析接口获取 ZIP（包含 Markdown 和图片）
     * 3. 更新文档状态和转换后的 URL
     *
     * @param document 文档对象
     */
    public void processDocumentToZip(KnowledgeDocument document, InputStream inputStream) {
        log.info("开始处理文档转换为 ZIP，documentId: {}", document.getDocTitle());

        // 更新状态为转换中
        document.setStatus(DocumentStatus.CONVERTING);
        boolean result = knowledgeDocumentService.updateById(document);
        Assert.isTrue(result, "文件CONVERTING状态更新失败");

        try {
            // 生成一串数字，避免文件名的中文乱码
            String docTitle = document.getDocTitle() + document.getDocTitle().hashCode();

            // 调用文档解析获取 ZIP 格式响应
            byte[] zipBytes = parsePdfToZip(docTitle, inputStream);

            // 保存转换后的 ZIP 到 MinIO
            String convertedObjectName = CONVERTED_FILE_DIR + document.getDocTitle().substring(0, document.getDocTitle().lastIndexOf(".")) + ".zip";
            String convertedUrl = fileStorageService.uploadFile(convertedObjectName, zipBytes, ContentType.ZIP);

            // 更新文档状态为已转换
            document.setStatus(DocumentStatus.CONVERTED);
            document.setConvertedDocUrl(convertedUrl);
            result = knowledgeDocumentService.updateById(document);
            Assert.isTrue(result, "文件CONVERTED状态更新失败");

            log.info("文档 ZIP 转换完成，documentId: {}", document.getDocTitle());
        } catch (Exception e) {
            log.error("文档 ZIP 转换失败，documentId: {}", document.getDocTitle(), e);
            // 转换失败，状态回滚为 UPLOADED
            document.setStatus(DocumentStatus.UPLOADED);
            result = knowledgeDocumentService.updateById(document);
            Assert.isTrue(result, "文件UPLOADED状态更新失败");
            throw new RuntimeException("文档 ZIP 转换失败: " + e.getMessage(), e);
        } finally {
            closeQuietly(inputStream);
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
    private String parsePdfToMarkdown(String fileName, InputStream fileStream) {
        String url = fileParseApiUrl + "/file_parse";

        // 配置请求超时
        RequestConfig requestConfig = RequestConfig.custom().setConnectionRequestTimeout(Timeout.ofMilliseconds(connectTimeout)).setResponseTimeout(Timeout.ofMilliseconds(responseTimeout)).build();

        try (CloseableHttpClient httpClient = HttpClients.custom().setDefaultRequestConfig(requestConfig).build()) {

            HttpPost httpPost = new HttpPost(url);
            httpPost.setHeader("Accept", "application/json");

            // 构建 multipart 请求体
            HttpEntity multipartEntity = MultipartEntityBuilder.create()
                    .addBinaryBody("files", fileStream, org.apache.hc.core5.http.ContentType.APPLICATION_OCTET_STREAM, fileName)
                    .addTextBody("backend", "pipeline").addTextBody("response_format_zip", "true")
                    .addTextBody("return_images", "true").addTextBody("return_model_output", "false")
                    .addTextBody("return_middle_json", "false").build();

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
            closeQuietly(fileStream);
        }
    }

    /**
     * 调用文件解析接口，获取 ZIP 格式响应
     * 使用 Apache HttpClient 5，支持流式下载大文件
     *
     * @param fileName   文件名
     * @param fileStream 文件输入流
     * @return ZIP 文件字节数组
     */
    private byte[] parsePdfToZip(String fileName, InputStream fileStream) {
        String url = fileParseApiUrl + "/file_parse";

        // 配置请求超时
        RequestConfig requestConfig = RequestConfig.custom().setConnectionRequestTimeout(Timeout.ofMilliseconds(connectTimeout)).setResponseTimeout(Timeout.ofMilliseconds(responseTimeout)).build();

        try (CloseableHttpClient httpClient = HttpClients.custom().setDefaultRequestConfig(requestConfig).build()) {

            HttpPost httpPost = new HttpPost(url);
            httpPost.setHeader("Accept", "application/json");

            // 构建 multipart 请求体，启用 ZIP 格式和返回图片
            HttpEntity multipartEntity = MultipartEntityBuilder.create().addBinaryBody("files", fileStream, org.apache.hc.core5.http.ContentType.APPLICATION_OCTET_STREAM, fileName).addTextBody("backend", "pipeline").addTextBody("response_format_zip", "true").addTextBody("return_images", "true").addTextBody("return_model_output", "false").addTextBody("return_middle_json", "false").build();

            httpPost.setEntity(multipartEntity);

            log.info("开始调用文件解析接口（ZIP 模式）: {}", url);

            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                int statusCode = response.getCode();
                log.info("文件解析接口响应状态码: {}", statusCode);

                HttpEntity responseEntity = response.getEntity();
                if (statusCode == 200 && responseEntity != null) {
                    // 读取响应体为字节数组（ZIP 文件）
                    byte[] zipBytes = EntityUtils.toByteArray(responseEntity);
                    log.info("文件解析接口调用成功，ZIP 文件大小: {} bytes", zipBytes.length);
                    return zipBytes;
                } else {
                    String responseBody = responseEntity != null ? EntityUtils.toString(responseEntity, "UTF-8") : "";
                    log.error("文件解析接口调用失败，状态码: {}, 响应: {}", statusCode, responseBody);
                    throw new RuntimeException("文件解析接口调用失败: HTTP " + statusCode + ", " + responseBody);
                }
            }

        } catch (Exception e) {
            log.error("调用文件解析接口异常", e);
            throw new RuntimeException("调用文件解析接口失败: " + e.getMessage(), e);
        } finally {
            closeQuietly(fileStream);
        }
    }

    /**
     * 安静关闭输入流，忽略异常
     *
     * @param inputStream 输入流
     */
    private void closeQuietly(InputStream inputStream) {
        if (inputStream != null) {
            try {
                inputStream.close();
            } catch (Exception ignored) {
                // 忽略关闭异常
            }
        }
    }
}
