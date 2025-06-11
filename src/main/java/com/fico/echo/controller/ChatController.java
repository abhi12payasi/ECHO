package com.fico.echo.controller;

import com.fico.echo.model.ChatResponse;
import com.fico.echo.service.LlamaService;
import com.fico.echo.service.LlamaWebFluxService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.Map;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    @Autowired
    //private LlamaService llamaService;
    private LlamaWebFluxService llamaService;

    @GetMapping
    public String greet(){
        return "Hello";
    }

    @PostMapping
    public ResponseEntity<ChatResponse> askBot(@RequestBody Map<String, String> request) {
        long start = System.currentTimeMillis();
        String question = request.get("question");
        String answer = llamaService.askLlama(question);
        //String answer = llamaService.askQuestion(question);
        System.out.println("Time Con : " + (System.currentTimeMillis() - start));
        return ResponseEntity.ok(new ChatResponse(answer));
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamResponse(@RequestParam String question) {
        return llamaService.streamLlamaResponse(question);
    }

}
