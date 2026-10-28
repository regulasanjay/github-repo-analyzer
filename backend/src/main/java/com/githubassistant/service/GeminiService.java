package com.githubassistant.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.http.HttpStatusCode;
import java.util.*;
import java.nio.charset.StandardCharsets;
import java.io.IOException;

@Service
public class GeminiService {
    private final RestClient http;
    @Value("${app.gemini-api-key:}") private String apiKey;
    @Value("${app.gemini-model:gemini-2.5-flash}") private String model;
    @Value("${app.gemini-fallback-model:gemini-3.6-flash}") private String fallbackModel;
    @Value("${app.gemini-embedding-model:gemini-embedding-2}") private String embeddingModel;
    public GeminiService(RestClient http){this.http=http;}
    public boolean configured(){return apiKey!=null&&!apiKey.isBlank();}
    public float[] embed(String text){
        requireKey();
        Map<?,?> response=http.post().uri("https://generativelanguage.googleapis.com/v1beta/models/{model}:embedContent",embeddingModel).header("x-goog-api-key",apiKey).body(Map.of("content",Map.of("parts",List.of(Map.of("text",text))),"outputDimensionality",768)).retrieve().onStatus(HttpStatusCode::isError,(req,res)->{throw providerError("embedding",embeddingModel,res.getStatusCode().toString(),res.getBody());}).body(Map.class);
        Map<?,?> embedding=(Map<?,?>)response.get("embedding");List<?> values=(List<?>)embedding.get("values");float[] out=new float[values.size()];for(int i=0;i<values.size();i++)out[i]=((Number)values.get(i)).floatValue();return out;
    }
    public List<float[]> embedAll(List<String> texts){
        requireKey();if(texts.isEmpty())return List.of();
        List<Map<String,Object>> requests=texts.stream().map(text->Map.<String,Object>of("model","models/"+embeddingModel,"content",Map.of("parts",List.of(Map.of("text",text))),"outputDimensionality",768)).toList();
        Map<?,?> response=http.post().uri("https://generativelanguage.googleapis.com/v1beta/models/{model}:batchEmbedContents",embeddingModel).header("x-goog-api-key",apiKey).body(Map.of("requests",requests)).retrieve().onStatus(HttpStatusCode::isError,(req,res)->{throw providerError("batch embedding",embeddingModel,res.getStatusCode().toString(),res.getBody());}).body(Map.class);
        List<?> embeddings=(List<?>)response.get("embeddings");if(embeddings==null||embeddings.size()!=texts.size())throw new IllegalStateException("Gemini returned an unexpected number of embeddings");
        List<float[]> result=new ArrayList<>(embeddings.size());for(Object item:embeddings){List<?> values=(List<?>)((Map<?,?>)item).get("values");float[] vector=new float[values.size()];for(int i=0;i<values.size();i++)vector[i]=((Number)values.get(i)).floatValue();result.add(vector);}return result;
    }
    public String answer(String query,String history,String context){
        requireKey();String prompt="You are a codebase assistant. Answer the user's question using only the repository context below. If evidence is insufficient, say so. Cite sources inline as [path:Lstart-Lend]. Do not invent files or line numbers.\n\nConversation:\n"+history+"\n\nRepository context:\n"+context+"\n\nQuestion: "+query;
        Map<?,?> body=Map.of("contents",List.of(Map.of("parts",List.of(Map.of("text",prompt)))),"generationConfig",Map.of("temperature",0.2,"maxOutputTokens",1200));
        Map<?,?> response;
        try{response=generate(model,body);}catch(IllegalStateException primaryError){if(!Objects.toString(primaryError.getMessage(),"").contains("(503")||fallbackModel==null||fallbackModel.isBlank()||fallbackModel.equals(model))throw primaryError;try{response=generate(fallbackModel,body);}catch(IllegalStateException fallbackError){throw new IllegalStateException("Gemini primary and fallback models are temporarily unavailable. Please retry shortly. Primary: "+primaryError.getMessage()+"; fallback: "+fallbackError.getMessage(),fallbackError);}}
        List<?> candidates=(List<?>)response.get("candidates");if(candidates==null||candidates.isEmpty())return "I couldn't generate an answer from the retrieved repository context.";
        Map<?,?> candidate=(Map<?,?>)candidates.get(0);Map<?,?> content=(Map<?,?>)candidate.get("content");List<?> parts=(List<?>)content.get("parts");return String.valueOf(((Map<?,?>)parts.get(0)).get("text"));
    }
    private Map<?,?> generate(String selectedModel,Map<?,?> body){return http.post().uri("https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent",selectedModel).header("x-goog-api-key",apiKey).body(body).retrieve().onStatus(HttpStatusCode::isError,(req,res)->{throw providerError("answer",selectedModel,res.getStatusCode().toString(),res.getBody());}).body(Map.class);}
    private void requireKey(){if(!configured())throw new IllegalStateException("GEMINI_API_KEY is not configured");}
    private IllegalStateException providerError(String operation,String model,String status,java.io.InputStream body){
        try {
            String detail=new String(body.readNBytes(1200),StandardCharsets.UTF_8).replaceAll("[\\r\\n\\t]+"," ").trim();
            return new IllegalStateException("Gemini "+operation+" request failed for model "+model+" ("+status+")"+(detail.isBlank()?"":": "+detail));
        } catch(IOException ex) {
            return new IllegalStateException("Gemini "+operation+" request failed for model "+model+" ("+status+")");
        }
    }
}
