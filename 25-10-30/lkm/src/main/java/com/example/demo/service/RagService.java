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
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.SearchRequest.Builder;
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

  // ChatClient
  private ChatClient chatClient;
  // VectorStore
  @Autowired
  private VectorStore vectorStore;
  // JdbcTemplate
  @Autowired
  JdbcTemplate jdbcTemplate;

  // 생성자
  public RagService(ChatClient.Builder chatClientBuilder) {
    this.chatClient = chatClientBuilder.build();
  }

  // VectorStore 비우기
  public void clearVectorStore() {
    // SQL문 직접 작성
    jdbcTemplate.update("TRUNCATE TABLE vector_store");
  }

  // ETL(PDF)
  public void ragEtl(MultipartFile attach, String source, int chunkSize, int minChunkSizeChars) throws Exception {

    // 추출
    // Resource 생성
    Resource resource = new ByteArrayResource(attach.getBytes());
    // DocumentReader 생성
    DocumentReader reader = new PagePdfDocumentReader(resource);
    // Document 리스트로 추출
    List<Document> documents = reader.read();

    // 메타데이터 추가
    for (Document doc : documents) {
      doc.getMetadata().put("source", source);
    }

    // 청크로 분할하기
    TokenTextSplitter splitter = new TokenTextSplitter(
      chunkSize, minChunkSizeChars, 0, 10000, true);
    documents = splitter.apply(documents);

    // VectorStore에 적재
    vectorStore.add(documents);
  }

  // LLM과 대화(RAG)
  // 검색 -> 문맥 -> 응답
  public String ragChat(String question, double score, String source) {

    // SearchRequest 생성 - 검색 조건
    Builder builder = SearchRequest.builder()
      // 조건1: 유사도 점수가 score 이상
      .similarityThreshold(score)
      // 조건2: 최상위 3개만 가져옴
      .topK(3);
    if (source != null && !source.equals("")) {
      // source 값이 있을 경우 추가해서 검색 조건을 완성
      builder.filterExpression("source == '%s'".formatted(source));
    }
    SearchRequest searchRequest = builder.build();

    // RAG를 처리하는 Advisor 얻기
    // 검색 조건을 사용해 저장소에서 검색 -> 결과를 프롬프트에 포함
    QuestionAnswerAdvisor advisor = QuestionAnswerAdvisor.builder(vectorStore)
      .searchRequest(searchRequest)
      .build();

    // LLM에 프롬프트 전송 및 응답
    String answer = chatClient.prompt()
      .user(question)
      // advisor를 chatClient에 추가
      .advisors(advisor, new SimpleLoggerAdvisor(Ordered.LOWEST_PRECEDENCE - 1))
      .call()
      .content();
    
    return answer;
  }

  
}
