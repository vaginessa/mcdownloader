package mcdownloader;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import javax.crypto.CipherInputStream;
import javax.crypto.NoSuchPaddingException;


public class ChunkWriter implements Runnable {
    
    private long last_chunk_id_dispatched;
    private long last_chunk_id_written;
    private long bytes_written;
    private final long file_size;
    private final ConcurrentHashMap<Long,Chunk> chunk_queue;
    private final Downloader downloader;
    private final byte[] byte_file_key;
    private final byte[] byte_iv;
    private volatile boolean exit;
    private final ConcurrentLinkedQueue<Long> rejectedChunkIds;

  
    public byte[] getByte_file_key() {
        return byte_file_key;
    }
    
    public ConcurrentLinkedQueue getRejectedChunkIds() {
        return rejectedChunkIds;
    }
    
    public synchronized void rejectChunkId(long chunk_id)
    {
        this.rejectedChunkIds.offer(chunk_id);
    }

    public boolean isExit() {
        return exit;
    }
    
    public long getBytes_written() {
        return bytes_written;
    }
    
    public long getLast_chunk_id_written() {
        return last_chunk_id_written;
    }
   
    public ChunkWriter(Downloader downloader) throws IOException
    {
        this.file_size = downloader.getFile_Size();
        this.byte_file_key = CryptTools.initMEGALinkKey(downloader.getFile_key());
        this.byte_iv = CryptTools.initMEGALinkKeyIV(downloader.getFile_key());     
        this.chunk_queue = new ConcurrentHashMap();
        this.rejectedChunkIds = new ConcurrentLinkedQueue();
        this.downloader = downloader;
        
        if(downloader.getProg() == 0)
        {
            this.last_chunk_id_dispatched = 0;
            this.last_chunk_id_written = 0;
            this.bytes_written = 0;
        }
        else
        {
            this.last_chunk_id_written = (this.last_chunk_id_dispatched = this.calculateLastWrittenChunk(downloader.getProg()));
            this.bytes_written = downloader.getProg();
        }
        
        this.exit = false;
    }

    public ConcurrentHashMap getChunk_queue()
    {
        return this.chunk_queue;
    }
    
    @Override
    public void run()
    {
        Chunk current_chunk;
        CipherInputStream cis;
        byte[] buffer = new byte[8*1024];
        int reads;
       
            try {
                
                this.downloader.printDebug("Filewriter: let's do some work!");
                
                while(!this.exit && (!this.downloader.isExit() || this.downloader.chunkDownloadersRunning()) && this.bytes_written < this.file_size)
                {
                    while(this.chunk_queue.containsKey(this.last_chunk_id_written+1))
                    {
                        current_chunk = this.chunk_queue.get(this.last_chunk_id_written+1);
                        
                        cis = new CipherInputStream(current_chunk.getInputStream(), CryptTools.genDecrypter("AES", "AES/CTR/NoPadding", this.byte_file_key, CryptTools.forwardMEGALinkKeyIV(this.byte_iv, this.bytes_written)));

                        while((reads=cis.read(buffer))!=-1)
                        {
                            this.downloader.getOs().write(buffer, 0, reads);
                        }

                        cis.close();

                        this.bytes_written+=current_chunk.getSize();

                        this.chunk_queue.remove(current_chunk.getId());

                        this.last_chunk_id_written = current_chunk.getId();

                        this.downloader.printDebug("Chunk ["+this.last_chunk_id_written+"] written to disk!");
                    }
                    
                    if(!this.downloader.isExit() && this.downloader.chunkDownloadersRunning() && this.bytes_written < this.file_size)
                    {
                        synchronized(this.chunk_queue)
                        {
                            this.downloader.printDebug("Filewriter waiting for chunk ["+(this.last_chunk_id_written+1)+"]...");
                            this.chunk_queue.wait();
                        }
                    }
                }

            } catch (InterruptedException | IOException | NoSuchAlgorithmException | InvalidKeyException | NoSuchPaddingException | InvalidAlgorithmParameterException ex) {
                
                this.downloader.printDebug(ex.getMessage());
                
                this.downloader.emergencyStopDownloader(ex.getMessage());
            }
            
            synchronized(this.downloader.getExecutor())
            {
                this.downloader.getExecutor().notify();
            }
            
            this.exit = true;
                
            this.downloader.printDebug("Filewriter: bye bye");
    }
    
    public synchronized long nextChunkId()
    {
        Long next_id;
        
        if((next_id=(Long)this.rejectedChunkIds.poll()) != null) {
            return next_id;
        }
        else {
            return ++this.last_chunk_id_dispatched;
        }
    }
    
    private long calculateLastWrittenChunk(long temp_file_size)
    {
        if(temp_file_size > 3584*1024)
        {
            return 7 + (long)Math.ceil((temp_file_size - 3584*1024)/(1024*1024));
        }
        else
        {
            int i=0, tot=0;
            
            while(tot < temp_file_size)
            {
                i++;
                tot+=i*128*1024;
            }
            
            return i;
        }
    }
}
