package com.githubassistant.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.HttpClientErrorException;
import java.util.*;

@Service
public class PineconeService {
    private final RestClient http;
    @Value("${app.pinecone-api-key:}") private String apiKey;
    @Value("${app.pinecone-index-host:}") private String host;
    public PineconeService(RestClient http){this.http=http;}
    public boolean configured(){return apiKey!=null&&!apiKey.isBlank()&&host!=null&&!host.isBlank();}
    public void upsert(String namespace,String id,float[] vector,Map<String,Object> metadata){if(!configured())return;Map<String,Object> item=new HashMap<>(metadata);item.put("id",id);item.put("values",vector);http.post().uri(endpoint("/vectors/upsert")).header("Api-Key",apiKey).header("Content-Type","application/json").body(Map.of("namespace",namespace,"vectors",List.of(item))).retrieve().toBodilessEntity();}
    public void upsertBatch(String namespace,List<String> ids,List<float[]> vectors,List<Map<String,Object>> metadata){if(!configured()||ids.isEmpty())return;if(ids.size()!=vectors.size()||ids.size()!=metadata.size())throw new IllegalArgumentException("Vector batch fields must have matching sizes");List<Map<String,Object>> items=new ArrayList<>(ids.size());for(int i=0;i<ids.size();i++){Map<String,Object> item=new HashMap<>(metadata.get(i));item.put("id",ids.get(i));List<Double> values=new ArrayList<>(vectors.get(i).length);for(float value:vectors.get(i))values.add((double)value);item.put("values",values);items.add(item);}http.post().uri(endpoint("/vectors/upsert")).header("Api-Key",apiKey).header("Content-Type","application/json").body(Map.of("namespace",namespace,"vectors",items)).retrieve().toBodilessEntity();}
    public List<String> query(String namespace,float[] vector,int topK){if(!configured())return List.of();List<Double> values=new ArrayList<>(vector.length);for(float v:vector)values.add((double)v);Map<?,?> response=http.post().uri(endpoint("/query")).header("Api-Key",apiKey).body(Map.of("namespace",namespace,"vector",values,"topK",topK,"includeMetadata",false)).retrieve().body(Map.class);if(response==null||!(response.get("matches") instanceof List<?> matches))return List.of();return matches.stream().map(m->String.valueOf(((Map<?,?>)m).get("id"))).toList();}
    public void deleteNamespace(String namespace){if(!configured())return;try{http.post().uri(endpoint("/vectors/delete")).header("Api-Key",apiKey).body(Map.of("namespace",namespace,"deleteAll",true)).retrieve().toBodilessEntity();}catch(HttpClientErrorException.NotFound ex){if(ex.getResponseBodyAsString()==null||!ex.getResponseBodyAsString().contains("Namespace not found"))throw ex;}}
    private String endpoint(String path){String base=host.trim();if(!base.startsWith("https://")&&!base.startsWith("http://"))base="https://"+base;while(base.endsWith("/"))base=base.substring(0,base.length()-1);return base+path;}
}
