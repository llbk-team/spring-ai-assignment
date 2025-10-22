package com.example.demo.service;

import java.util.Map;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

@Slf4j
@Service
public class AiService {
    private ChatClient chatClient;

    public AiService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    // UserMessage만 포함된 프롬프트를 LLM에 전달할 경우
    public Flux<String> promptTemplate1(String language, String statement) {
        Prompt prompt = userTemplate.create(Map.of("language", language, "statement", statement));
        Flux<String> fluxString = chatClient.prompt(prompt)
                .stream()
                .content();

        return fluxString;
    }

    // createMessage()를 사용할 경우
    public Flux<String> promptTemplate2(String language, String statement) {
        Flux<String> fluxString = chatClient.prompt()
                // 각각의 프롬프트에서 Message 객체를 얻는다
                .messages(
                        systemTemplate.createMessage(), // SystemPrompt에 대한 템플릿
                        userTemplate.createMessage(Map.of("language", language, "statement", statement))) // UserPrompt에 대한 템플릿
                .stream()
                .content();
        return fluxString;
    }

    public Flux<String> promptTemplate3(String language, String statement) {
        Flux<String> fluxString = chatClient.prompt()
                // 각각의 템플릿에서 render()를 호출하여, system()과 user()에 각각 제공한다.
                .system(systemTemplate.render())
                .user(userTemplate.render(Map.of("language", language, "statement", statement)))
                .stream()
                .content();
        return fluxString;
    }

    public Flux<String> promptTemplate4(String language, String statement) {
        String systemText = """
                답변을 생성할 때 HTML와 CSS를 사용해서 파란 글자로 출력하세요.
                <span> 태그 안에 들어갈 내용만 출력하세요.
                """;

        String userText = """
                다음 한국어 문장을 %s로 번역해 주세요. \n 문장: %s
                """.formatted(language, statement);

        Flux<String> fluxString = chatClient.prompt()
                .system(systemText)
                .user(userText)
                .stream()
                .content();

        return fluxString;
    }

    private PromptTemplate systemTemplate = SystemPromptTemplate.builder()
            .template("""
                    답변을 생성할 때 HTML와 CSS를 사용해서 파란 글자로 출력하세요.
                    <span> 태그 안에 들어갈 내용만 출력하세요.""")
            .build();

    public PromptTemplate userTemplate = PromptTemplate.builder()
            .template("다음 한국어 문장을 {language}로 번역해 주세요. \n 문장: {statement}")
            .build();
}
