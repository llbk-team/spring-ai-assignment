package com.example.demo.service;

import org.springframework.ai.openai.api.OpenAiApi.ChatModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ETLService {

  // private ChatModel ChatModel;
  // private VectorStore VectorStore;
  
  // public ETLService(ChatModel chatModel, VectorStore vectorStore){
  //   this.ChatModel = chatModel;
  //   this.VectorStore = vectorStore;
  // }

  // public String etlFromFile(String title, String author, MultipartFile attach) throws Exception{

  //   List<Document> documents = extractFromFile(attach);

  //   for (Document doc : documents){
  //     Map<String, Object> metadata = doc.getMetadata();
  //     metadata.putAll(Map.of(
  //       "title", title,
  //       "author", author,
  //       "source", attach.getOriginalFilename()
  //     ));
  //   }

  // }

  // public List<Document> extractFromFile(MultipartFile attach) throws Exception {
  //   Resource resource = new ByteArrayResource(attach.getBytes());

  //   List<Document> documents = null;
  //   if(attach.getContentType().equals("text/plain")) {
  //     DocumentReader reader = new TextReader(resource);
  //     documents = reader.read();
  //   } else if (attach.getContentType().equals("application/pdf")) {
  //     DocumentReader reader = new PagePdfDocumentReader(resource);
  //   } 
  // }

}
