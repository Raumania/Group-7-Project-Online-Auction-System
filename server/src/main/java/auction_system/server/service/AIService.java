package auction_system.server.service;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.openai.OpenAiChatModel;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Base64;

public class AIService {
    // singleton for AIservice
    private static AIService instance;
    private AIService() {}
    public static AIService getInstance() {
        if(instance == null) {
            instance = new AIService();
        }
        return instance;
    }
    //core in belowww :>
    public String chat(String prompt,String imageBase64){
        OpenAiChatModel model = OpenAiChatModel.builder()
                .baseUrl("https://resurface-exert-reaffirm.ngrok-free.dev")
                .apiKey("no-key").timeout(Duration.ofMinutes(10))
                .build();
        AiMessage aiMessage;
        if(imageBase64.isEmpty()) {
            UserMessage userMessage = UserMessage.from(TextContent.from(prompt));
            aiMessage = model.generate(userMessage).content();
        }
        else {
            UserMessage userMessage = UserMessage.from(
                    TextContent.from(prompt),
                    ImageContent.from(imageBase64, "image/jpeg")
            );
            aiMessage = model.generate(userMessage).content();
        }
        return aiMessage.text();
    }
}
