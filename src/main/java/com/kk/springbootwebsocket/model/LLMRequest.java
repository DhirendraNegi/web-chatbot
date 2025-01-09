package com.kk.springbootwebsocket.model;

import java.util.List;

public class LLMRequest {

    private String question;
    private List<String> userType;

    // Default constructor
    public LLMRequest() {
    }

    // Parameterized constructor
    public LLMRequest(String question, List<String> userType) {
        this.question = question;
        this.userType = userType;
    }

    // Getter for question
    public String getQuestion() {
        return question;
    }

    // Setter for question
    public void setQuestion(String question) {
        this.question = question;
    }

    // Getter for userType
    public List<String> getUserType() {
        return userType;
    }

    // Setter for userType
    public void setUserType(List<String> userType) {
        this.userType = userType;
    }

    @Override
    public String toString() {
        return "MyRequest{" +
                "question='" + question + '\'' +
                ", userType=" + userType +
                '}';
    }
}
