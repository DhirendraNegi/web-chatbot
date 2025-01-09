package com.kk.springbootwebsocket.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class NLPController {

    @Autowired
    private OpenAIService openAIService;

    @GetMapping("/chat/nlp")
    public String chat(@RequestParam String message) {
//
        return openAIService.chat(message);

    }
}
