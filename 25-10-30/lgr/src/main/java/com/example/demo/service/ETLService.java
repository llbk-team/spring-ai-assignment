package com.example.demo.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentReader;
import org.springframework.ai.document.DocumentTransformer;
import org.springframework.ai.reader.JsonMetadataGenerator;
import org.springframework.ai.reader.JsonReader;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.reader.jsoup.JsoupDocumentReader;
import org.springframework.ai.reader.jsoup.config.JsoupDocumentReaderConfig;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ETLService {

  @Autowired
  private VectorStore vectorStore;

  // LLM 이용안하니까 chatclient 받을 필요 없다. 제목, 작성자, 내용
  public String etlFormFile(String title, String author, MultipartFile attach) throws Exception {
    // E: 추출 텍스트를 도큐먼트로 생성
    List<Document> documents = extractFormFile(attach); // 1000개의 도큐먼트 메타데이터 없음
    for (Document doc : documents) {
      Map<String, Object> metadata = doc.getMetadata();
      metadata.put("title", title);
      metadata.put("author", author);
      metadata.put("source", attach.getOriginalFilename());// 어떤 파일로부터 오는 이름인지

    }
    // T: 변환 잘게 쪼개는 과정
    List<Document> splitted_documents = transform(documents); // 주의할것

    // L: 적재(vector 스토어에 저장)
    vectorStore.add(splitted_documents);
    return "%d개를 %d로 쪼개어서 Vector Store에 저장함.".formatted(documents.size(), splitted_documents.size());
  }

  private List<Document> extractFormFile(MultipartFile attach) throws Exception {
    String fileName = attach.getOriginalFilename();
    String contentType = attach.getContentType();
    log.info("contentType:{}", contentType);
    byte[] bytes = attach.getBytes();
    Resource resource = new ByteArrayResource(bytes);// 이거 만듦

    List<Document> documents = new ArrayList<>();

    if (contentType.equals("text/plain")) {
      DocumentReader reader = new TextReader(resource);// 를 위해서
      documents = reader.read();
    } else if (contentType.equals("application/pdf")) {
      DocumentReader reader = new PagePdfDocumentReader(resource);// 를 위해서
      documents = reader.read();
    } else if (contentType.contains("word")) {
      DocumentReader reader = new TikaDocumentReader(resource);// 를 위해서
      documents = reader.read();
    }
    log.info("추출된 Documents수 :{}", documents.size());

    return documents;

  }

  // 이거는 왜 매개변수 List<Document>로 주는거지?
  public List<Document> transform(List<Document> documents) {
    TokenTextSplitter tokenTextSplitter = new TokenTextSplitter(200, 50, 0, 10000, true);
    List<Document> splitted_docs = tokenTextSplitter.apply(documents);

    return splitted_docs;
  }

  public String etlFromHtml(String title, String author, String url) throws Exception {
    Resource resource = new UrlResource(url);
    // 메타데이터를 만들면서 바로 집어넣음 위에 서비스처럼 나중에 메타 데이터를 넣지 않았음
    JsoupDocumentReader reader = new JsoupDocumentReader(
        resource,
        JsoupDocumentReaderConfig.builder()
            .charset("UTF-8")
            .selector("#content")
            .additionalMetadata(Map.of(
                "title", title, "author", author, "url", url))
            .build());
    // E
    List<Document> documents = reader.read();

    // T
    DocumentTransformer transformer = new TokenTextSplitter(
        120, 20, 0, 10000, false);

    List<Document> splitted_documents = transformer.apply(documents);

    // L
    vectorStore.add(splitted_documents);

    return "%d개를 %d로 쪼개어서 Vector Store에 저장함.".formatted(documents.size(), splitted_documents.size());
    
  }
  
  // 메타데이터를 추가해서 도큐먼트를 만들거다.
  public String etlFromJson( String url) throws Exception {
    Resource resource = new UrlResource(url);
    
    JsonReader jsonReader = new JsonReader(
      resource,
      jsonMap -> 
         Map.of("title",jsonMap.get("title"),
        "author",jsonMap.get("author"),
        "url",url),
        "data","content"
        );
        
        // E 추출
        List<Document> documents = jsonReader.read();
        
        // T 쪼갠다
        DocumentTransformer transformer = new TokenTextSplitter(
          200, 50, 0, 10000, false);
          List<Document> splitted_documents = transformer.apply(documents);
          
          // 저장한다.
          vectorStore.add(splitted_documents);
          return "%d개를 %d로 쪼개어서 Vector Store에 저장함.".formatted(documents.size(), splitted_documents.size());

  }

}
