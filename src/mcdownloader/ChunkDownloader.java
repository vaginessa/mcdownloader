package mcdownloader;

import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;


public class ChunkDownloader implements Runnable {
    
    private final int id;
    private final Downloader downloader;
    private volatile boolean exit;

    public ChunkDownloader(int id, Downloader downloader)
    {
        this.id = id;
        this.downloader = downloader;
        this.exit = false;
    }
    
    public void setExit(boolean exit) {
        this.exit = exit;
    }
    
    public int getId() {
        return id;
    }
    
    @Override
    public void run()
    {
        String worker_url=null;
        URL url;
        Chunk chunk;
        long chunk_id=0;
        int reads;
        byte[] buffer = new byte[8*1024];
        URLConnection urlConn;
        InputStream is;
        boolean chunk_ok = true;
        boolean chunk_error=false;
        
        this.downloader.printDebug("Worker ["+this.id+"]: let's do some work!");
        
        try {
            
            worker_url=this.downloader.getDownloadUrlForWorker(this.id);
        
        } catch (IOException ex) {
            
            this.downloader.printDebug(ex.getMessage());
            
            this.downloader.emergencyStopDownloader(ex.getMessage());
        }

        try
        {
            while(!chunk_error && !this.exit && !this.downloader.isExit())
            {
                chunk = new Chunk((chunk_id = chunk_ok?this.downloader.getFilewriter().nextChunkId():chunk_id), this.downloader.getFile_Size(), worker_url);

                try{

                   if(this.downloader.isPause()) {

                        this.downloader.pause_worker();

                        synchronized(this.downloader.getPauseLock())
                        {
                            this.downloader.printDebug("Worker ["+this.id+"] sleeping...");
                            this.downloader.getPauseLock().wait();
                        }
                   }

                    if(!this.exit && !this.downloader.isExit()) {
                        this.downloader.printDebug("Worker ["+this.id+"] downloading chunk ["+chunk.getId()+"]...");

                        chunk_ok=false;
                        url = new URL(chunk.getUrl());
                        urlConn = url.openConnection();
                        urlConn.setConnectTimeout(Downloader.CONNECT_TIMEOUT);
                        urlConn.setReadTimeout(Downloader.CONNECT_TIMEOUT);
                        is = urlConn.getInputStream();

                        while(!this.downloader.isExit() && !this.downloader.getFilewriter().isExit() && (reads=is.read(buffer))!=-1 ) 
                        {
                            chunk.getOutputStream().write(buffer, 0, reads);

                            this.downloader.getChunkPartialReads().offer(reads);

                            synchronized(this.downloader.getChunkPartialReads())
                            {
                                this.downloader.getChunkPartialReads().notify();
                            }
                        }

                        if(!this.downloader.getFilewriter().isExit() && chunk.getOutputStream().size() == chunk.getSize())
                        {
                            this.downloader.printDebug("Worker ["+this.id+"] has downloaded chunk ["+chunk.getId()+"]!");

                            this.downloader.getFilewriter().getChunk_queue().put(chunk.getId(), chunk);

                            synchronized(this.downloader.getFilewriter().getChunk_queue())
                            {
                                this.downloader.getFilewriter().getChunk_queue().notify();
                            }

                            chunk_ok = true;
                        }
                    }
               }
               catch (InterruptedException | IOException ex) 
               {
                    this.downloader.printDebug(ex.getMessage());

                    if(chunk.getOutputStream().size() > 0)
                    {
                        this.downloader.getChunkPartialReads().offer(-1*chunk.getOutputStream().size());

                        synchronized(this.downloader.getChunkPartialReads())
                        {
                            this.downloader.getChunkPartialReads().notify();
                        }
                    }

                    if(ex instanceof SocketTimeoutException)
                    {
                         try {

                             worker_url=this.downloader.getDownloadUrlForWorker(this.id);

                         } catch (IOException ex2) {

                             this.downloader.printDebug(ex2.getMessage());

                             this.downloader.emergencyStopDownloader(ex2.getMessage());
                         }
                    }
                    else
                        chunk_error = true;
               }
            }
        
        }catch(ChunkInvalidIdException e) {}
        
        if(chunk_error && chunk_id > 0)
        {
            this.downloader.getFilewriter().rejectChunkId(chunk_id);
        }
        
        if(!this.exit) {
            MiscTools.swingSetEnabled(this.downloader.getPanel().slots, false, false);
        }

        this.downloader.stopThisSlot(this, chunk_error);

        synchronized(this.downloader.getFilewriter().getChunk_queue())
        {
            this.downloader.getFilewriter().getChunk_queue().notify();
        }
        
        this.downloader.printDebug("Worker ["+this.id+"]: bye bye");
    }

}
