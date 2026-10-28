package com.githubassistant.service;

import com.githubassistant.model.*;
import com.githubassistant.repository.*;
import org.eclipse.jgit.api.Git;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

@Service
public class RepositoryIngestionService {
    private static final Set<String> EXT=Set.of("py","js","jsx","ts","tsx","java","kt","go","rs","c","cpp","h","hpp","html","css","scss","json","xml","yaml","yml","md","sql","sh","properties","gradle","toml");
    private static final Set<String> SKIP=Set.of(".git","node_modules","venv",".venv","__pycache__","dist","build",".next","target","vendor","coverage");
    private final RepositoryRecordRepository repos;private final CodeChunkRepository chunks;private final GeminiService gemini;private final PineconeService pinecone;
    @Value("${app.clone-dir:./data/repos}") private String cloneDir;
    @Value("${app.chunk-size:40}") private int chunkSize;
    @Value("${app.chunk-overlap:10}") private int overlap;
    @Value("${app.max-file-bytes:500000}") private long maxFileBytes;
    @Value("${app.max-repository-files:5000}") private int maxRepositoryFiles;
    @Value("${app.max-repository-source-bytes:52428800}") private long maxRepositorySourceBytes;
    @Value("${app.max-repository-chunks:5000}") private int maxRepositoryChunks;
    public RepositoryIngestionService(RepositoryRecordRepository repos,CodeChunkRepository chunks,GeminiService gemini,PineconeService pinecone){this.repos=repos;this.chunks=chunks;this.gemini=gemini;this.pinecone=pinecone;}
    @Async public void index(String id){RepositoryRecord record=repos.findById(id).orElse(null);if(record==null)return;Path checkout=null;
        try{checkout=Path.of(cloneDir).resolve(id+"-"+UUID.randomUUID());Files.createDirectories(checkout.getParent());try(Git git=Git.cloneRepository().setURI(record.getUrl()).setDirectory(checkout.toFile()).setDepth(1).call()){record.setDefaultBranch(git.getRepository().getBranch());}
            chunks.deleteByRepositoryId(id);pinecone.deleteNamespace(namespace(id));int files=0,count=0;long sourceBytes=0;
            try(Stream<Path> walk=Files.walk(checkout)){var pathIterator=walk.filter(p->Files.isRegularFile(p,LinkOption.NOFOLLOW_LINKS)).iterator();while(pathIterator.hasNext()){Path file=pathIterator.next();if(ignored(checkout,file)||!supported(file))continue;long fileBytes=Files.size(file);if(fileBytes>maxFileBytes)continue;files++;sourceBytes+=fileBytes;if(files>maxRepositoryFiles)throw new IllegalStateException("Repository exceeds the indexing limit of "+maxRepositoryFiles+" source files.");if(sourceBytes>maxRepositorySourceBytes)throw new IllegalStateException("Repository exceeds the indexing limit of "+(maxRepositorySourceBytes/(1024*1024))+" MB of source files.");String source=Files.readString(file,StandardCharsets.UTF_8);if(source.indexOf('\0')>=0)continue;List<String> lines=source.lines().toList();String relative=checkout.relativize(file).toString().replace('\\','/');
                    for(int start=0;start<lines.size();start+=Math.max(1,chunkSize-overlap)){int end=Math.min(lines.size(),start+chunkSize);if(start>=end)break;if(count>=maxRepositoryChunks)throw new IllegalStateException("Repository exceeds the indexing limit of "+maxRepositoryChunks+" code chunks.");String text=String.join("\n",lines.subList(start,end));CodeChunk c=new CodeChunk(id,relative,extension(file),text,start+1,end);c=chunks.save(c);float[] vector=gemini.embed(relative+"\n"+text);String vectorId=c.getId();pinecone.upsert(namespace(id),vectorId,vector,Map.of("repository_id",id,"file",relative,"start_line",start+1,"end_line",end,"language",extension(file)));c.setVectorId(vectorId);chunks.save(c);count++;if(end==lines.size())break;}
                }}
            record.setFileCount(files);record.setChunkCount(count);record.setStatus("completed");record.setError(null);
        }catch(Exception ex){try{chunks.deleteByRepositoryId(id);}catch(Exception ignored){}try{pinecone.deleteNamespace(namespace(id));}catch(Exception ignored){}record.setStatus("failed");record.setError(ex.getMessage()==null?ex.getClass().getSimpleName():ex.getMessage());}
        finally{record.touch();repos.save(record);if(checkout!=null)try{deleteTree(checkout);}catch(Exception ignored){}}
    }
    private boolean supported(Path p){return EXT.contains(extension(p));}
    private String extension(Path p){String n=p.getFileName().toString();int dot=n.lastIndexOf('.');return dot<0?"text":n.substring(dot+1).toLowerCase(Locale.ROOT);}
    private boolean ignored(Path root,Path p){for(Path part:root.relativize(p)){if(SKIP.contains(part.toString().toLowerCase(Locale.ROOT)))return true;}String n=p.getFileName().toString().toLowerCase(Locale.ROOT);return n.endsWith(".lock")||Set.of("package-lock.json","yarn.lock","pnpm-lock.yaml","poetry.lock","cargo.lock").contains(n);}
    private void deleteTree(Path root)throws Exception{List<Path> paths;try(Stream<Path> walk=Files.walk(root)){paths=walk.sorted(Comparator.reverseOrder()).toList();}for(Path path:paths)Files.deleteIfExists(path);}
    public static String namespace(String id){return "repo-"+id;}
}
