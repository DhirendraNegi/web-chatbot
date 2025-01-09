package com.kk.springbootwebsocket.controller;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.kk.springbootwebsocket.model.ChatRequest;
import com.kk.springbootwebsocket.model.ChatResponse;
import opennlp.tools.doccat.*;
import opennlp.tools.lemmatizer.LemmatizerME;
import opennlp.tools.lemmatizer.LemmatizerModel;
import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import opennlp.tools.util.*;
import opennlp.tools.util.model.ModelUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.ResourceUtils;
import org.springframework.web.client.RestTemplate;
import simplenlg.framework.NLGFactory;
import simplenlg.phrasespec.NPPhraseSpec;
import simplenlg.phrasespec.SPhraseSpec;
import simplenlg.phrasespec.VPPhraseSpec;
import simplenlg.realiser.english.Realiser;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;


@Service
public class OpenAIService {

    private static Map<String, String> questionAnswer = new HashMap<>();
    static {
        questionAnswer.put("greeting", "Hello, how can I help you?");
        questionAnswer.put("product-inquiry",
                "Product is a TipTop mobile phone. It is a smart phone with latest features like touch screen, blutooth etc.");
        questionAnswer.put("price-inquiry", "Price is $300");
        questionAnswer.put("conversation-continue", "What else can I help you with?");
        questionAnswer.put("conversation-complete", "Nice chatting with you. Bbye.");
        questionAnswer.put("background-report-check", "Please provide your e-mail address or request id");
        questionAnswer.put("email-request", "@, Let me search your details, please wait...");

    }
    @Value("${openai.api.key}")
    private String apiKey;

    @Value("${openai.model}")
    private String model;

    @Value("${openai.api.url}")
    private String apiUrl;

    private final RestTemplate restTemplate = new RestTemplate();
    //private static final String ENDPOINT = "https://api.openai.com/v1/completions";
    private static final String ENDPOINT = "http://localhost:8083/chat/nlp";

    public String getNamedEntities(String text) {
        // Create JSON payload
        JsonObject jsonPayload = new JsonObject();
        jsonPayload.addProperty("model", "text-davinci-003"); // You can use other models
        jsonPayload.addProperty("prompt", "Extract named entities from the following text: \"" + text + "\"");
        jsonPayload.addProperty("max_tokens", 150);
        jsonPayload.addProperty("temperature", 0.5);

        // Set up headers
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + apiKey);
        headers.set("Content-Type", "application/json");

        // Create HttpEntity with headers and payload
        HttpEntity<String> entity = new HttpEntity<>(jsonPayload.toString(), headers);

        // Make HTTP POST request
        ResponseEntity<String> response = restTemplate.exchange(
            ENDPOINT,
            HttpMethod.POST,
            entity,
            String.class
        );

        // Process and return the response
        JsonObject responseJson = JsonParser.parseString(response.getBody()).getAsJsonObject();

        return responseJson.getAsJsonArray("choices").getAsJsonArray().get(0).getAsJsonObject().get("text").getAsString().trim();
    }

    String getIntent(String chatMessage) {

        try {
            // Train categorizer model to the training data we created.
        DoccatModel model = null;

        model = trainCategorizerModel();

        String[] sentences = breakSentences(chatMessage);
        String answer = "";
        boolean conversationComplete = false;

        // Loop through sentences.
        for (String sentence : sentences) {

            // Separate words from each sentence using tokenizer.
            String[] tokens = tokenizeSentence(sentence);

            // Tag separated words with POS tags to understand their gramatical structure.
            String[] posTags = detectPOSTags(tokens);

            // Lemmatize each word so that its easy to categorize.
            String[] lemmas = lemmatizeTokens(tokens, posTags);

            // Determine BEST category using lemmatized tokens used a mode that we trained
            // at start.
            String category = detectCategory(model, lemmas);

            // Get predefined answer from given category & add to answer.
            //answer = answer + " " + questionAnswer.get(category);
            answer = category;

            // If category conversation-complete, we will end chat conversation.
            if ("conversation-complete".equals(category)) {
                conversationComplete = true;
            }
        }
        return answer;
        } catch (IOException e) {
            e.printStackTrace();
            return "Error processing the message";
        }
    }

    private DoccatModel trainCategorizerModel() throws FileNotFoundException, IOException {
        // faq-categorizer.txt is a custom training data with categories as per our chat
        // requirements.
        InputStreamFactory inputStreamFactory = new MarkableFileInputStreamFactory(ResourceUtils.getFile("classpath:faq-categorizer.txt"));
        ObjectStream<String> lineStream = new PlainTextByLineStream(inputStreamFactory, StandardCharsets.UTF_8);
        ObjectStream<DocumentSample> sampleStream = new DocumentSampleStream(lineStream);

        DoccatFactory factory = new DoccatFactory(new FeatureGenerator[] { new BagOfWordsFeatureGenerator() });

        TrainingParameters params = ModelUtil.createDefaultTrainingParameters();
        params.put(TrainingParameters.CUTOFF_PARAM, 0);

        // Train a model with classifications from above file.
        DoccatModel model = DocumentCategorizerME.train("en", sampleStream, params, factory);
        return model;
    }

    private static String[] breakSentences(String data) throws FileNotFoundException, IOException {
        // Better to read file once at start of program & store model in instance
        // variable. but keeping here for simplicity in understanding.
        try (InputStream modelIn = new FileInputStream( ResourceUtils.getFile("classpath:en-sent.bin"))) {

            SentenceDetectorME myCategorizer = new SentenceDetectorME(new SentenceModel(modelIn));

            String[] sentences = myCategorizer.sentDetect(data);
            System.out.println("Sentence Detection: " + Arrays.stream(sentences).collect(Collectors.joining(" | ")));

            return sentences;
        }
    }

    private static String[] tokenizeSentence(String sentence) throws FileNotFoundException, IOException {
        // Better to read file once at start of program & store model in instance
        // variable. but keeping here for simplicity in understanding.
        try (InputStream modelIn = new FileInputStream( ResourceUtils.getFile( "classpath:en-token.bin"))) {

            // Initialize tokenizer tool
            TokenizerME myCategorizer = new TokenizerME(new TokenizerModel(modelIn));

            // Tokenize sentence.
            String[] tokens = myCategorizer.tokenize(sentence);
            System.out.println("Tokenizer : " + Arrays.stream(tokens).collect(Collectors.joining(" | ")));

            return tokens;

        }
    }

    private static String[] detectPOSTags(String[] tokens) throws IOException {
        // Better to read file once at start of program & store model in instance
        // variable. but keeping here for simplicity in understanding.
        try (InputStream modelIn = new FileInputStream( ResourceUtils.getFile("classpath:en-pos-maxent.bin"))) {

            // Initialize POS tagger tool
            POSTaggerME myCategorizer = new POSTaggerME(new POSModel(modelIn));

            // Tag sentence.
            String[] posTokens = myCategorizer.tag(tokens);
            System.out.println("POS Tags : " + Arrays.stream(posTokens).collect(Collectors.joining(" | ")));

            return posTokens;

        }
    }

    private static String[] lemmatizeTokens(String[] tokens, String[] posTags)
            throws InvalidFormatException, IOException {
        // Better to read file once at start of program & store model in instance
        // variable. but keeping here for simplicity in understanding.
        try (InputStream modelIn = new FileInputStream(ResourceUtils.getFile("classpath:en-lemmatizer.bin"))) {

            // Tag sentence.
            LemmatizerME myCategorizer = new LemmatizerME(new LemmatizerModel(modelIn));
            String[] lemmaTokens = myCategorizer.lemmatize(tokens, posTags);
            System.out.println("Lemmatizer : " + Arrays.stream(lemmaTokens).collect(Collectors.joining(" | ")));

            return lemmaTokens;

        }
    }

    private static String detectCategory(DoccatModel model, String[] finalTokens) throws IOException {

        // Initialize document categorizer tool
        DocumentCategorizerME myCategorizer = new DocumentCategorizerME(model);

        // Get best possible category.
        double[] probabilitiesOfOutcomes = myCategorizer.categorize(finalTokens);
        String category = myCategorizer.getBestCategory(probabilitiesOfOutcomes);
        System.out.println("Category: " + category);

        return category;

    }

    public String chat( String prompt) {
        ChatRequest request = new ChatRequest(model, prompt);

        ChatResponse response = restTemplate.postForObject(
                apiUrl,
                request,
                ChatResponse.class);

        if (response == null || response.getChoices() == null || response.getChoices().isEmpty()) {
            return "No response";
        }

        return response.getChoices().get(0).getMessage().getContent();
    }

    public String generateNLG(String subject, String verb, String object) {
        Realiser realiser = new Realiser();

        // Create a new sentence
        NPPhraseSpec subjectLoc = new NPPhraseSpec(new NLGFactory());
        subjectLoc.setNoun("the cat");

        VPPhraseSpec verbPhrase = new VPPhraseSpec( new NLGFactory());
        verbPhrase.setVerb("chased");

        NPPhraseSpec objectLoc = new NPPhraseSpec(new NLGFactory());
        objectLoc.setNoun("the mouse");

        SPhraseSpec sentence = new SPhraseSpec(new NLGFactory());
        sentence.setSubject(subject);
        sentence.setVerbPhrase(verbPhrase);
        sentence.setObject(object);

        // Realise the sentence
        String generatedSentence = realiser.realiseSentence(sentence);

        // Print the generated sentence
        System.out.println(generatedSentence);
        return subject + " " + verb + " " + object;
    }

}
