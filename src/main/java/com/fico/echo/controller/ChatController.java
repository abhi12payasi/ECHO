package com.fico.echo.controller;

import com.fico.echo.service.LlamaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    @Autowired
    private LlamaService llamaService;

    @PostMapping
    public ResponseEntity<String> askBot(@RequestBody Map<String, String> request) {
        String question = request.get("question");
        String answer = llamaService.askLlama(question);
        return ResponseEntity.ok(answer);
    }
}
