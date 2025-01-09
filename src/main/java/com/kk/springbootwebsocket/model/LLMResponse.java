package com.kk.springbootwebsocket.model;

import java.util.List;

public class LLMResponse {

    private String response;

    // Default constructor
    public LLMResponse() {
    }

    // Parameterized constructor
    public LLMResponse(String response) {
        this.response = response;
    }

    // Getter for question
    public String getResponse() {
        return response;
    }

    // Setter for question
    public void setResponse(String response) {
        this.response = response;
    }
}
