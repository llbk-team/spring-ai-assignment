package com.example.demo.service;

import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentReader;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class RagService {
    private ChatClient chatClient;

    @Autowired
    private VectorStore vectorStore;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public RagService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    // DELETE FROM vector_stor
    public void clearVectorStore() {
        // DELETE FROM vector_stor
        jdbcTemplate.update("TRUNCATE TABLE seoul_plan");
    }

    public void ragEtl(MultipartFile attach, String source, int chunkSize, int minChunkSizeChars) throws Exception {
        String contentType = attach.getContentType();

        // 추출
        Resource resource = new ByteArrayResource(attach.getBytes()) {
            @Override
            public String getFilename() {
                return attach.getOriginalFilename();
            }
        };

        DocumentReader reader = null;

        if (contentType.equals("application/pdf")) {
            reader = new PagePdfDocumentReader(resource);
        } else if (contentType.equals("text/plain")) {
            reader = new TextReader(resource);
        } else if (contentType.contains("word")) {
            reader = new TikaDocumentReader(resource);
        }

        // E: 텍스트 추출하기
        List<Document> documents = reader.read();

        // 메타데이터 추가하기
        for (Document doc : documents) {
            doc.getMetadata().put("source", source);
        }

        TokenTextSplitter splitted = new TokenTextSplitter(
                chunkSize, minChunkSizeChars, 0, 10000, true);

        // T: 변환
        documents = splitted.transform(documents);

        // L: vectorStore에 저장하기
        vectorStore.add(documents);
    }

    public String ragChat(String question, double score, String source) {
        SearchRequest.Builder builder = SearchRequest.builder()
                .similarityThreshold(score)
                .topK(3);

        if (source != null && !source.equals("")) {
            builder.filterExpression("source == '%s'".formatted(source));
        }

        SearchRequest searchRequest = builder.build();

        // 어드바이저 생성
        QuestionAnswerAdvisor advisor = QuestionAnswerAdvisor.builder(vectorStore)
                .searchRequest(searchRequest)
                .build();

        // RAG 수행
        String answer = chatClient.prompt()
                .user(question)
                .advisors(advisor, new SimpleLoggerAdvisor(Ordered.LOWEST_PRECEDENCE - 1))
                .call()
                .content();

        return answer;
    }
}
