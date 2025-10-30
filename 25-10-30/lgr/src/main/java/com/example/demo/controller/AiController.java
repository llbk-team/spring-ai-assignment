package com.example.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.service.ETLService;
import com.example.demo.service.RagService1;
import lombok.extern.slf4j.Slf4j;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@Slf4j
@RequestMapping("/ai")
public class AiController {
  @Autowired
  private  RagService1 ragService1;

  @Autowired
  private ETLService etlService;

  @PostMapping(value = "/txt-pdf-docx-etl", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.TEXT_PLAIN_VALUE)
  public String txtPdfDocx(
      @RequestParam("title") String title,
      @RequestParam("author") String author,
      @RequestParam("attach") MultipartFile attach) throws Exception {

    String result = etlService.etlFormFile(title, author, attach);

    return result;
  }
  // MULTIPART_FORM_DATA_VALU 파일은 안받지만 그냥 냅둡

  @PostMapping(value = "/html-etl", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.TEXT_PLAIN_VALUE)
  public String htmlEtl(
      @RequestParam("title") String title,
      @RequestParam("author") String author,
      @RequestParam("url") String url) throws Exception {
    String result = etlService.etlFromHtml(title, author, url);
    return result;
  }

  @PostMapping(value = "/json-etl", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.TEXT_PLAIN_VALUE)
  public String jsonEtl(
      @RequestParam("url") String url) throws Exception {
    String result = etlService.etlFromJson(url);
    return result;
  }

  @GetMapping(value = "/rag-clear", produces = MediaType.TEXT_PLAIN_VALUE)
  public String ragClear() {
    ragService1.clearVectorStore();
    return "벡터 저장소의 데이터를 모두 삭제했습니다.";
  }

  
  @PostMapping(
    value = "/rag-etl",
    consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
    produces = MediaType.TEXT_PLAIN_VALUE
  )
  public String ragEtl(
    @RequestParam("attach") MultipartFile attach,
    @RequestParam("source") String source,
    @RequestParam(value = "chunkSize", defaultValue = "200") int chunkSize, 
    @RequestParam(value = "minChunkSizeChars", defaultValue = "100") int minChunkSizeChars 
  ) throws Exception {
    ragService1.ragEtl(attach, source, chunkSize, minChunkSizeChars);
    return "PDF ETL 과정을 성공적으로 처리했습니다.";
  }   

  
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
    String answer = ragService1.ragChat(question, score, source);
    return answer;
  }




}
