package com.example.demo.controller;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.service.RagService;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;

@RestController
@RequestMapping("/ai")
@Slf4j
public class RagController {

  @Autowired
  private RagService ragService;

  // 벡터 저장소 비우기
  @GetMapping(
    value = "/rag-clear",
    produces = MediaType.TEXT_PLAIN_VALUE
  )
  public String ragClear() {
    ragService.clearVectorStore();
    return "벡터 저장소의 데이터를 모두 삭제했습니다.";
  }

  // ETL(파일 추출 -> 변환 -> 적재)
  @PostMapping(
    value = "/rag-etl",
    consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
    produces = MediaType.TEXT_PLAIN_VALUE
  )
  public String ragEtl(
    @RequestParam("attach") MultipartFile attach,
    @RequestParam("source") String source,
    @RequestParam("chunkSize") int chunkSize,
    @RequestParam("minChunkSizeChars") int minChunkSizeChars
  ) throws Exception {
    ragService.ragEtl(attach, source, chunkSize, minChunkSizeChars);
    return "PDF ETL 과정을 성공적으로 처리했습니다.";
  }

  // RAG (검색)
  @PostMapping(
    value = "/rag-chat",
    consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
    produces = MediaType.TEXT_PLAIN_VALUE
  )
  public String ragChat(
    @RequestParam("question") String question,
    @RequestParam(value = "score", defaultValue = "0.0") double score,
    @RequestParam("source") String source
  ) {
    String answer = ragService.ragChat(question, score, source);
      
    return answer;
  }
  
  
  
}
