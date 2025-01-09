package com.kk.springbootwebsocket.controller;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.kk.springbootwebsocket.model.LLMRequest;
import com.kk.springbootwebsocket.model.LLMResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;


@org.springframework.web.bind.annotation.RestController
@RequestMapping("/chat/response")
public class ChatRestController {

    @Autowired
    private RestTemplate restTemplate;
    private Integer callCount = 0;

    @Autowired
    private OpenAIService openAIService;

    @PostMapping(produces = "application/json")
    public String response( @RequestBody String chatMessage) {
        System.out.println("Inside response method " +chatMessage);
        String intent = openAIService.getIntent(chatMessage);
        System.out.println("Intent: " + intent);
        //if ( chatMessage.contains("Hi") || chatMessage.contains("Hello") || chatMessage.contains("Hey") ) {
        if (intent.equals("greeting")) {
            return "Hello, how can I help you?";
        }
//      else if ( chatMessage.contains("background status")
//                || chatMessage.contains("background check")
//                || chatMessage.contains("background check status")
//                || chatMessage.contains("background status check")) {
        else if(intent.equals("background-report-check")) {
            return "Please provide the candidate's name Or email address";
//        } else if (chatMessage.contains(".com")
//                || chatMessage.contains(".net")
//                || chatMessage.contains("@")) {
        } else if(intent.equals("email-request")) {
            return "Please provide you last name, first name and last 4 digits of your SSN for verification";
            // } else if ( chatMessage.contains("ssn") && chatMessage.contains("3132") ) {
        }else if (intent.equals("ssn-request") ) {
            String response = callHRG(chatMessage);
            JsonObject retJsonObject = JsonParser.parseString(callHRG(chatMessage)).getAsJsonObject();
            String ssn = retJsonObject.get("ssn").getAsString();
            String lastFourDigit = ssn.substring(ssn.length() - 4);
            if (chatMessage.contains(lastFourDigit)) {
                return openAIService.generateNLG("SSN", "Verified", "Please enter your request id");
                //return "SSN Verified, Please enter your request id ....." + response;
            } else {
                return openAIService.generateNLG("SSN", "Not Verified", "Please enter your last name, first name and last 4 digits of your SSN for verification");
            }
        } else if (intent.equals("request-id-status")) {
            String response = callHRG(chatMessage);
            JsonObject retJsonObject = JsonParser.parseString(callHRG(chatMessage)).getAsJsonObject();
            String applicationStatus = retJsonObject.get("applicationStatus").getAsString();
            String requestId = retJsonObject.get("requestId").getAsString();
            System.out.println("requestId :" + requestId);
            if (chatMessage.equalsIgnoreCase(requestId)) {
                return openAIService.generateNLG("Request ID", "Status", applicationStatus);
            } else {
                return openAIService.generateNLG("Request ID", "Not Found", "Please enter your request id");
            }
        } else {
            return callLLM(chatMessage);
        }
    }
    public String callHRG(@RequestParam String message) {
        String url = "http://localhost:8082/cs_tool/rest/v1/supersearch/request";
        System.out.println("URL: " + url);

        String jsonString = "{ \"name\": \"John Doe\", \"age\": 30, \"email\": \"john.doe@example.com\" }";

        JsonObject jsonObject = null;
        try {
            // Convert String to JSONObject
            jsonObject = JsonParser.parseString(jsonString).getAsJsonObject();

            // Print the JSONObject
            System.out.println(jsonObject.toString()); // Pretty print with an indent of 4 spaces
        } catch (Exception e) {
            e.printStackTrace();
        }
        // Create headers if needed
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");

        // Create HttpEntity with headers and body
        HttpEntity<String> requestEntity = new HttpEntity<>(jsonString, headers);

        // Perform POST request
        ResponseEntity<String> jsonResponse = restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);

        // Check response status and handle as needed
        if (jsonResponse.getStatusCode() == HttpStatus.OK) {
            System.out.println(jsonResponse);
            System.out.println(jsonResponse.getBody());
            JsonObject jsonObjectResponse = JsonParser.parseString(jsonResponse.getBody()).getAsJsonObject();

            System.out.println(jsonObjectResponse.get("lastName"));
            System.out.println(jsonObjectResponse.get("ssn"));
            System.out.println(jsonObjectResponse.get("customerName"));
            System.out.println(jsonObjectResponse.get("firstName"));

            return jsonResponse.getBody();
        } else {
            return "Request failed";
        }
    }

    public String callLLM(@RequestParam String message) {
        //http://172.21.92.7:5301/aiml-llm/api/v1
        String url = "http://172.21.92.7:5320/aiml-llm/api/v1/ChatBot";
        System.out.println("URL: " + url);

        // Create headers if needed
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        LLMRequest request = new LLMRequest();
        request.setQuestion(message);
        request.setUserType(List.of("e3876b91-bfc9-4be8-afd9-0b9293a9f544","0d5114c6-320c-4500-a79f-6a298122adce"));

        // Create HttpEntity with headers and body
        HttpEntity<LLMRequest> requestEntity = new HttpEntity<>(request, headers);

        // Perform POST request
        ResponseEntity<LLMResponse> llmresponse = restTemplate.exchange(url, HttpMethod.POST, requestEntity, LLMResponse.class);

        // Check response status and handle as needed
        if (llmresponse.getStatusCode() == HttpStatus.OK) {
            System.out.println(llmresponse.getBody());
            return ((LLMResponse)llmresponse.getBody()).getResponse();
        } else {
            return "Request failed";
        }
    }

    public String getNLGSentence( String subject, String verb, String object ) {
        return openAIService.generateNLG(subject, verb, object);
    }
}