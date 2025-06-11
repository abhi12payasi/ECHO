package com.fico.echo.controller;

import com.fico.echo.model.ChatResponse;
import com.fico.echo.service.LlamaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    @Autowired
    private LlamaService llamaService;

    @PostMapping
    public ResponseEntity<ChatResponse> askBot(@RequestBody Map<String, String> request) {
        String question = request.get("question");
        String answer = llamaService.askLlama(question);
        //String answer = llamaService.askQuestion(question);
        return ResponseEntity.ok(new ChatResponse(answer));
    }

}
