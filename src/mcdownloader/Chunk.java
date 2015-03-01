package mcdownloader;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;


public class Chunk {
    
    private final long id;
    private final long offset;
    private final long size;
    private final ByteArrayOutputStream data_os;
    private final String url;

    public Chunk(long id, long file_size, String file_url) throws ChunkInvalidIdException
    {
        this.id = id;
        this.offset = this.calculateChunkOffset();
        
        if(this.offset>=file_size) {
            throw new ChunkInvalidIdException(String.valueOf(id));
        }
        
        this.size = this.calculateChunkSize(file_size);
        this.url = file_url!=null?file_url+"/"+this.offset+"-"+(this.offset+this.size-1):null;
        this.data_os = new ByteArrayOutputStream((int)size);
    }
    
    public long getOffset() {
        return offset;
    }
    
    public ByteArrayOutputStream getOutputStream() {
        return this.data_os;
    }
    
    public long getId() {
        return this.id;
    }

    public long getSize() {
        return this.size;
    }

    public String getUrl() {
        return this.url;
    }
    
    public ByteArrayInputStream getInputStream() {
        return new ByteArrayInputStream(this.data_os.toByteArray());
    }
    
    private long calculateChunkSize(long file_size)
    {
        long chunk_size = (this.id>=1 && this.id<=7)?this.id*128*1024:1024*1024;
        
        if(this.offset + chunk_size > file_size) {
            chunk_size = file_size - this.offset;
        }
        
        return chunk_size;
    }
    
    private long calculateChunkOffset()
    {        
        long[] offs = {0, 128, 384, 768, 1280, 1920, 2688};
        
        return (this.id<=7?offs[(int)this.id-1]:(3584 + (this.id-8)*1024))*1024;
    }
    
    public static Chunk tryGenChunk(long id, long file_size, String file_url)
    {
        Chunk chunk=null;
        
        try
        {
            chunk = new Chunk(id, file_size, file_url);
            
        }catch(ChunkInvalidIdException exception){}
        
        return chunk;
    }
}
