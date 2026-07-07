package com.lake.knowenginelearn.rag.modules.splitter;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.segment.TextSegment;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author LAKE.YANG
 * @filename MarkdownHeaderParentTextSplitterTest
 * @date 2026-07-08 00:18
 */
class MarkdownHeaderParentTextSplitterTest {

    @Test
    void split() {

        MarkdownHeaderParentTextSplitter markdownHeaderParentTextSplitter = new MarkdownHeaderParentTextSplitter(3, false, false, 1000, 100);
        // MinerU_markdown_r7-product-manual-20250123_2028781865782407168.md
        // MinerU_markdown_r7-product-manual-for-testset.processed.md
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("MinerU_markdown_r7-product-manual-20250123_2028781865782407168.md");
        DocumentParser parser = new TextDocumentParser();
        Document parsedDocument = parser.parse(inputStream);
        List<TextSegment> segments = markdownHeaderParentTextSplitter.split(parsedDocument);

        System.out.println(segments.size());

        for (TextSegment segment : segments) {
            System.out.println(segment.text());
            System.out.println(segment.metadata());
            System.out.println("======");
        }

    }
}