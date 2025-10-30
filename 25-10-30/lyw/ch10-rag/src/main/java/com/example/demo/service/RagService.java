package com.example.demo.service;

import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentReader;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SearchRequest.Builder;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class RagService {
  private ChatClient chatClient;

  @Autowired
  private VectorStore vectorStore;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  public RagService(ChatClient.Builder chatClientBuilder) {
    this.chatClient = chatClientBuilder.build();
  }

  // 저장소 비우기
  public void clearVectorStore() {
    jdbcTemplate.update("TRUNCATE TABLE vector_store");
  }

  // ETL
  public void ragEtl(MultipartFile attach, String source, int chunkSize, int minChunkSizeChars) throws Exception {
    Resource resource = new ByteArrayResource(attach.getBytes());
    DocumentReader reader = new PagePdfDocumentReader(resource);
    List<Document> documents = reader.read();

    for(Document doc : documents) {
      doc.getMetadata().put("source", source);
    }

    TokenTextSplitter splitter = new TokenTextSplitter(chunkSize, minChunkSizeChars, 0, 10000, true);
    documents = splitter.apply(documents);

    vectorStore.add(documents);
  }

  // RAG
  public String ragChat(String question, double score, String source) {

    Builder builder = SearchRequest.builder()
      .similarityThreshold(score)
      .topK(3);

    if(source != null && !source.equals("")) {
      builder.filterExpression("source == '%s'".formatted(source));
    }

    // build 해주기
    SearchRequest searchRequest = builder.build();

    QuestionAnswerAdvisor advisor = QuestionAnswerAdvisor.builder(vectorStore)
      .searchRequest(searchRequest)
      .build();

    String answer = chatClient.prompt()
      .user(question)
      .advisors(advisor, new SimpleLoggerAdvisor(Ordered.LOWEST_PRECEDENCE-1))
      .call()
      .content();

    return answer;
  }

  
}
