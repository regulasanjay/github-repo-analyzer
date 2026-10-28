package com.githubassistant.service;

import com.githubassistant.model.CodeChunk;
import com.githubassistant.repository.CodeChunkRepository;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class SearchService {
    private final CodeChunkRepository chunks;private final GeminiService gemini;private final PineconeService pinecone;
    public SearchService(CodeChunkRepository chunks,GeminiService gemini,PineconeService pinecone){this.chunks=chunks;this.gemini=gemini;this.pinecone=pinecone;}
    public List<CodeChunk> search(String repoId,String query,int limit){List<CodeChunk> all=chunks.findByRepositoryId(repoId);if(all.isEmpty())return List.of();Set<String> semantic=new LinkedHashSet<>();if(gemini.configured()&&pinecone.configured())try{semantic.addAll(pinecone.query(RepositoryIngestionService.namespace(repoId),gemini.embed(query),Math.max(limit*3,10)));}catch(Exception ignored){}
        Set<String> terms=new HashSet<>();for(String t:query.toLowerCase(Locale.ROOT).split("[^a-z0-9_$]+"))if(t.length()>1)terms.add(t);
        Map<String,Integer> rank=new HashMap<>();int i=0;for(String id:semantic)rank.put(id,i++);
        return all.stream().map(c->{String lower=c.getContent().toLowerCase(Locale.ROOT);long hits=terms.stream().filter(t->lower.contains(t)||c.getFile().toLowerCase(Locale.ROOT).contains(t)).count();double keyword=terms.isEmpty()?0:(double)hits/terms.size();double vector=rank.containsKey(c.getVectorId())?1.0/(1+rank.get(c.getVectorId())):0;double score=.7*vector+.3*keyword;return Map.entry(c,score);}).filter(e->e.getValue()>0).sorted(Map.Entry.<CodeChunk,Double>comparingByValue().reversed()).limit(limit).map(Map.Entry::getKey).toList();
    }
}
