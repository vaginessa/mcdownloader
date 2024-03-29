package mcdownloader;

import java.awt.Color;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.FileChannel;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.swing.JSpinner;

class Downloader implements Runnable
{
    public static final int MIN_WORKERS=1;
    public static final int DEFAULT_WORKERS=4;
    public static final int MAX_WORKERS=30;
    public static final int MAX_WORKERS_URL=10;
    public static final int CONNECT_TIMEOUT = 30000;
    public static final int EXP_BACKOFF_BASE=2;
    public static final int EXP_BACKOFF_SECS_RETRY=1;
    public static final int EXP_BACKOFF_MAX_WAIT_TIME=128;
    public static final int GC_CBC_CHUNKS=10;
    public static final int ANTI_FLOOD=250;
    public static final Object cbc_lock=new Object();
    protected final DownloaderBox panel;
    protected long size;
    protected final String file_link;
    protected String file_key;
    protected String file_name;
    protected String file_pass;
    protected String file_noexpire;
    protected final String download_path;
    protected int slots;
    private volatile long prog;
    private volatile int speed;
    private File file;
    private volatile boolean exit;
    private volatile boolean pause;
    private ChunkWriter filewriter;
    private final ArrayList<ChunkDownloader> chunkdownloaders;
    private final ExecutorService executor;
    private final String[] download_urls;
    private Double progress_bar_rate;
    private OutputStream os;
    private boolean checking_cbc;
    private boolean retrying_mc_api;
    private String fatal_error;
    private final boolean debug_mode;
    private PrintWriter log_file;
    private final ConcurrentLinkedQueue<Integer> chunkPartialReads;
    private final Object pause_lock;
    private int paused_workers;
    protected boolean provision_ok;
    protected boolean download_started;
    protected boolean status_error;
    protected boolean restart;
    protected boolean downloading;

    public void setPaused_workers(int paused_workers) {
        this.paused_workers = paused_workers;
    }

    public ConcurrentLinkedQueue<Integer> getChunkPartialReads() {
        return chunkPartialReads;
    }

    public boolean isChecking_cbc() {
        return checking_cbc;
    }
    
    public boolean isRetrying_mc_api() {
        return retrying_mc_api;
    }
    
    public String[] getDownload_urls() {
        return download_urls;
    }
    
    public OutputStream getOs() {
        return os;
    }
    
    public Object getPauseLock() {
        return this.pause_lock;
    }
    
    public String getFile_key() {
        return file_key;
    }
    
    public long getFile_Size() {
        return size;
    }
    
    public ExecutorService getExecutor() {
        return executor;
    }

    public ChunkWriter getFilewriter() {
        return filewriter;
    }
  
    public ArrayList getChunkdownloaders() {
        return chunkdownloaders;
    }

    public boolean isExit() {
        return this.exit;
    }
    
    public boolean isPause() {
        return this.pause;
    }
    
    public File getFile()
    {
        return this.file;
    }
    
    public long getProg()
    {
        return this.prog;
    }
    
    public DownloaderBox getPanel()
    {
        return this.panel;
    }
    
    public void setExit(boolean value)
    {
        this.exit = value;
    }
    
    public void setPause(boolean value)
    {
        this.pause = value;
    }
    
    
    public Downloader(DownloaderBox panel, String url, String download_path, String filename, String filekey, Long filesize, String filepass, String filenoexpire, boolean restart, boolean debug_mode)
    {
        this.panel = panel;
        this.download_path = download_path;
        this.file_name = filename;
        
        if(filesize!=null) {
            this.size = filesize;
        }
        
        this.file_key = filekey;
        this.file_pass = filepass;
        this.file_noexpire = filenoexpire;
        this.slots = DEFAULT_WORKERS;
        this.file_link = url;
        this.chunkdownloaders = new ArrayList();
        this.chunkPartialReads = new ConcurrentLinkedQueue();
        this.checking_cbc = false;
        this.retrying_mc_api = false;
        this.debug_mode = debug_mode;
        this.exit = false;
        this.pause = false;
        this.fatal_error = null;
        this.executor = Executors.newCachedThreadPool();
        this.download_urls = new String[(int)Math.ceil(Downloader.MAX_WORKERS/Downloader.MAX_WORKERS_URL)];
        this.pause_lock = new Object();
        this.paused_workers=0;
        this.provision_ok = true;
        this.download_started=false;
        this.status_error=false;
        this.restart = restart;
        this.downloading=false;
 
    }
    
    public void provisionDownload() {
        
        this.printStatus("Provisioning download, please wait...");
        
        MiscTools.swingSetVisible(this.getPanel().copy_button, true, false);
        
        String[] file_info;
        
        String exit_message=null;
        
        try {
                if(this.file_name == null)
                {
                    file_info = this.getMegaFileMetadata(this.file_link, this.panel.getPanel());
                    
                    if(file_info==null) {
                        
                       this.provision_ok=false;

                    } else {

                        this.file_name = file_info[0];

                        this.size = Long.valueOf(file_info[1]);

                        this.file_key=file_info[2];
                        
                        if(file_info.length == 5)
                        {
                            this.file_pass = file_info[3];
                        
                            this.file_noexpire = file_info[4];
                        }
                        
                        try {

                       McDownloaderMain.registerDownload(this.file_link, this.download_path, this.file_name, this.file_key, this.size, this.file_pass, this.file_noexpire);

                        } catch (SQLException ex) {

                            this.provision_ok=false;

                            exit_message = "Error registering download (file "+ this.download_path+"/"+this.file_name +" already downloading)";
                        }
                    }
                } else if(this.restart) {
                    
                    try {

                           McDownloaderMain.registerDownload(this.file_link, this.download_path, this.file_name, this.file_key, this.size, this.file_pass, this.file_noexpire);

                        } catch (SQLException ex) {

                            this.provision_ok=false;

                            exit_message = "Error registering download (file "+ this.download_path+"/"+this.file_name +" already downloading)";
                        }
                }
                

            } catch (Exception ex) {
                
                this.provision_ok=false;
                
                exit_message = ex.getMessage();
            }

        if(!this.provision_ok) {
            
            this.hideAllExceptStatus();
            
            if(this.fatal_error != null) {
                
                this.printStatusError(this.fatal_error);
                
            }else if(exit_message!=null) {
                
                this.printStatusError(exit_message);
            }

        } else {

            this.printStatus(this.download_path+"/"+this.file_name +" [" +String.format("%.2f",((double)this.size)/(1024*1024)) + " MB] (Waiting to start...)");
        }
        
        MiscTools.swingSetVisible(this.getPanel().closebutton, true, false);
        
        if(!this.provision_ok) {
            MiscTools.swingSetVisible(this.getPanel().restart_download, true, false);
        }
    }

        
    @Override
    public void run()
    {    
        MiscTools.swingSetVisible(this.getPanel().closebutton, false, false);
        
        String[] file_info;
        
        String exit_message=null;
        
        int r;
        
        this.printStatus("Starting download, please wait...");
     
        try {       
            
            if(!this.exit)
            {
                String filename = this.download_path+"/"+this.file_name;
                
                MiscTools.swingSetVisible(this.getPanel().fname_label, true, false);
                
                MiscTools.swingSetText(this.getPanel().fname_label, filename+" ["+ String.format("%.2f", ((double)this.size)/(1024*1024)) +" MB]", false);
                
                this.file = new File(filename);
                
                if(this.file.getParent()!=null)
                {
                    File path = new File(this.file.getParent());
                
                    path.mkdirs();
                }
                
                if(!this.file.exists()) 
                {
                    this.printStatus("Starting download (retrieving MEGA temp link), please wait...");
                    
                    this.download_urls[0] = this.getMegaFileDownloadUrl(this.file_link);
                    
                    if(!this.exit)
                    {
                        this.retrying_mc_api = false;
                        

                        MiscTools.swingSetMinimum(this.getPanel().progress, 0, false);
                        MiscTools.swingSetMaximum(this.getPanel().progress, Integer.MAX_VALUE, false);
                        MiscTools.swingSetStringPainted(this.getPanel().progress, true, false);

                        this.progress_bar_rate = (double)Integer.MAX_VALUE/(double)this.size;

                        filename = this.download_path+"/"+this.file_name;

                        this.file = new File(filename+".mctemp");

                        if(this.file.exists())
                        {
                            this.printStatus("File exists, resuming download...");

                            long max_size = this.calculateMaxTempFileSize(this.file.length());

                            if(max_size != this.file.length())
                            {                            

                                this.printStatus("Truncating temp file...");

                                try (FileChannel out_truncate = new FileOutputStream(filename+".mctemp", true).getChannel())
                                {
                                    out_truncate.truncate(max_size);
                                }
                            }

                            this.prog = this.file.length();
                            MiscTools.swingSetValue(this.getPanel().progress, (int)Math.ceil(this.progress_bar_rate*this.prog), false);
                        }
                        else
                        {
                            this.prog = 0;
                            MiscTools.swingSetValue(this.getPanel().progress, 0, false);
                        }

                        this.os = new BufferedOutputStream(new FileOutputStream(this.file, (this.prog > 0)));

                        this.filewriter = new ChunkWriter(this);

                        this.executor.execute(this.filewriter);
                        
                        ProgressMeter pm = new ProgressMeter(this);
                        this.executor.execute(pm);
                        
                        SpeedMeter sp = new SpeedMeter(this);
                        Future future_sp=executor.submit(sp);
                        
                        this.getPanel().getPanel().global_speed_meter.registerSpeedMeter(sp);

                        for(int t=1; t <= this.slots; t++)
                        {
                            ChunkDownloader c = new ChunkDownloader(t, this);

                            this.chunkdownloaders.add(c);

                            this.executor.execute(c);
                        }

                        this.printStatus("Downloading file from mega.co.nz ...");
                        
                        this.downloading = true;
                        
                        this.getPanel().getPanel().download_queue.secureNotify();
                        

                        MiscTools.swingSetVisible(this.getPanel().pause_button, true, false);
                        MiscTools.swingSetVisible(this.getPanel().progress, true, false);
                        MiscTools.swingSetVisible(this.getPanel().slots_label, true, false);
                        MiscTools.swingSetVisible(this.getPanel().slots, true, false);
                        

                        synchronized(this.executor)
                        {
                            try {
                                this.executor.wait();
                            } catch (InterruptedException ex) {
                                this.printDebug(ex.getMessage());
                            }
                        }
                        
                        synchronized(this.chunkPartialReads)
                        {
                            pm.setExit(true);
                            this.chunkPartialReads.notifyAll();
                        }
                        
                        this.getPanel().getPanel().global_speed_meter.unregisterSpeedMeter(sp);
                        
                        synchronized(this.getPanel().getPanel().global_speed_meter) {
                           this.getPanel().getPanel().global_speed_meter.notify();
                        }
                        
                        future_sp.cancel(true);
                        
                        MiscTools.swingSetVisible(this.getPanel().speed, false, false);
                        MiscTools.swingSetVisible(this.getPanel().rem_time, false, false);
                        
                        this.executor.shutdown();
                        
                        while(!this.executor.isTerminated())
                        {
                            try {
                                
                                this.executor.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);

                            } catch (InterruptedException ex) {
                                Logger.getLogger(Downloader.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }

                        this.os.close();
                        
                        this.downloading=false;
                        
                        this.getPanel().getPanel().download_queue.secureNotify();

                        MiscTools.swingSetVisible(this.getPanel().pause_button, false, false);
                        MiscTools.swingSetVisible(this.getPanel().stop_button, false, false);
                     
                        MiscTools.swingSetVisible(this.getPanel().slots_label, false, false);
                        MiscTools.swingSetVisible(this.getPanel().slots, false, false);


                        if(this.prog == this.size)
                        {
                            MiscTools.swingSetValue(this.getPanel().progress, Integer.MAX_VALUE, false);

                            this.file.renameTo(new File(filename));
                            
                            String verify_file = McDownloaderMain.getValueFromDB("verify_file");

                            if(verify_file!=null && verify_file.equals("yes"))
                            {
                                this.checking_cbc = true;
                   
                                this.printStatus("Waiting to check file integrity...");
                                
                                this.prog = 0;
                                
                                MiscTools.swingSetValue(this.getPanel().progress, 0, false);
                                
                                synchronized(Downloader.cbc_lock) {
                                    
                                    this.printStatus("Checking file integrity, please wait...");
                                   
                                    MiscTools.swingSetVisible(this.getPanel().stop_button, true, false);
                                    MiscTools.swingSetText(this.getPanel().stop_button, "CANCEL CHECK", false);

                                    boolean cbc_ok;

                                    this.getPanel().getPanel().download_queue.download_boxes_running_list.remove(this.panel);

                                    this.getPanel().getPanel().download_queue.download_boxes_cbc_list.add(this.panel);

                                    this.getPanel().getPanel().download_queue.secureNotify();

                                    if((cbc_ok = this.verifyFileCBCMAC(filename)))
                                    {
                                        exit_message = "File successfully downloaded! (Integrity check PASSED)";

                                        this.printStatusOK(exit_message);
                                    }
                                    else if(!this.exit)
                                    {
                                        exit_message = "BAD NEWS :( File is DAMAGED!";

                                        this.printStatusError(exit_message);

                                        this.status_error = true;
                                    }
                                    else
                                    {                                
                                        exit_message = "File successfully downloaded! (but integrity check CANCELED)";

                                        this.printStatusOK(exit_message);

                                        this.status_error = true;

                                    }

                                    MiscTools.swingSetVisible(this.getPanel().stop_button, false, false);

                                    MiscTools.swingSetValue(this.getPanel().progress, Integer.MAX_VALUE, false);
                                
                                }
                            }
                            else
                            {
                                exit_message = "File successfully downloaded!";
                                
                                this.printStatusOK(exit_message);
                                                                
                            }
                        }
                        else if(this.exit && this.fatal_error == null)
                        {
                            this.hideAllExceptStatus();
                            
                            exit_message = "Download CANCELED!";
                            
                            this.printStatusError(exit_message);
                            
                           this.status_error = true;
                            
                            if(this.file!=null && !MiscTools.swingIsSelected(this.panel.keep_temp)){
                                this.file.delete();
                            }
                            
                        }
                        else if(this.fatal_error != null)
                        {
                            this.hideAllExceptStatus();
                            
                            exit_message = this.fatal_error;
                            
                            this.printStatusError(this.fatal_error);
                            
                            this.status_error = true;
                            
                           
                        }
                        else
                        {
                            this.hideAllExceptStatus();
                            
                            exit_message = "OOOPS!! Something (bad) happened but... what?";
                            
                            this.printStatusError(exit_message);
                            
                            this.status_error = true;
                           
                        }     
                        

                        if(this.debug_mode)
                            this.log_file.close();
                    }
                    else if(this.fatal_error != null)
                    {
                        this.hideAllExceptStatus();
                        
                        exit_message = this.fatal_error;
                            
                        this.printStatusError(this.fatal_error);
                            
                        this.status_error = true;
                    }
                    else
                    {
                        this.hideAllExceptStatus();
                        
                        exit_message = "Download CANCELED!";
                            
                        this.printStatusError(exit_message);
                            
                        this.status_error = true;
                        
                        if(this.file!=null && !MiscTools.swingIsSelected(this.panel.keep_temp)){
                            this.file.delete();
                        }
                    }
                    
            } else {
            
                    this.hideAllExceptStatus();
                    
                    MiscTools.swingSetVisible(this.getPanel().fname_label, false, false);

                    exit_message = filename+" already exists!";

                    this.printStatusError(exit_message);
                    
                    this.status_error = true;
            }
                
            }
            else if(this.fatal_error != null)
            {
                this.hideAllExceptStatus();
                
                exit_message = this.fatal_error;
                            
                this.printStatusError(this.fatal_error);
                            
                this.status_error = true;
            }
            else
            {
                this.hideAllExceptStatus();
                
                exit_message = "Download CANCELED!";
                            
                this.printStatusError(exit_message);
                
                this.status_error = true;

                if(this.file!=null && !MiscTools.swingIsSelected(this.panel.keep_temp)){
                    this.file.delete();
                }
            }
        }
        catch (IOException ex) {
            exit_message = "I/O ERROR "+ex.getMessage();
                            
            this.printStatusError(exit_message);
            
            this.status_error = true;
       
            this.printDebug(ex.getMessage());
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException ex) {
            this.printDebug(ex.getMessage());
        } catch (InterruptedException ex) {
            Logger.getLogger(Downloader.class.getName()).log(Level.SEVERE, null, ex);
        }        
        
        this.printDebug("Downloader: bye bye");

        this.getPanel().getPanel().download_queue.download_boxes_running_list.remove(this.panel);
        
        this.getPanel().getPanel().download_queue.download_boxes_finished_queue.add(this.panel);

        McDownloaderMain.unRegisterDownload(this.file_link);
        
        this.getPanel().getPanel().download_queue.secureNotify();
      
        MiscTools.swingSetVisible(this.getPanel().closebutton, true, false);
        
        if(this.status_error) {
            MiscTools.swingSetVisible(this.getPanel().restart_download, true, false);
        }
    }
    
    public synchronized void pause_worker() {
        
        if(++this.paused_workers == (int)MiscTools.swingGetValue(this.getPanel().slots)) {
            
            this.printStatus("Download paused!");
            MiscTools.swingSetText(this.getPanel().pause_button, "RESUME DOWNLOAD", false);
            MiscTools.swingSetEnabled(this.getPanel().pause_button, true, false);
        }
    }
    
    public boolean checkDownloadUrl(String string_url)
    {
       try {
            URL url = new URL(string_url+"/0-0");
            URLConnection connection = url.openConnection();
            connection.setConnectTimeout(CONNECT_TIMEOUT);
            InputStream is = connection.getInputStream();
            
            while(is.read()!=-1);
            
            is.close();
             
            return true;
            
        }catch (Exception ex) {
            
            return false;
        }        
    }
    
    public synchronized String getDownloadUrlForWorker(int chunk_downloader_id) throws IOException
    {
        int pos = (int)Math.ceil((double)chunk_downloader_id/Downloader.MAX_WORKERS_URL)-1;

        if(this.download_urls[pos] == null || !this.checkDownloadUrl(this.download_urls[pos]))
        {
            int retry=0;
            
            boolean mc_error;
            
            do
            {
                mc_error=false;
                
                try {
                    
                    if( MiscTools.findFirstRegex("://mega(\\.co)?\\.nz/", this.file_link, 0) != null)
                    {
                        MegaAPI ma = new MegaAPI();

                        this.download_urls[pos] = ma.getMegaFileDownloadUrl(this.file_link);
                    }    
                    else
                    {
                        this.download_urls[pos] = MegaCrypterAPI.getMegaFileDownloadUrl(this.file_link, this.file_pass, this.file_noexpire);
                    }
 
                }
                catch(MegaCrypterAPIException | MegaAPIException e)
                {
                    mc_error=true;

                    for(long i=MiscTools.getWaitTimeExpBackOff(retry++, Downloader.EXP_BACKOFF_BASE, Downloader.EXP_BACKOFF_SECS_RETRY, Downloader.EXP_BACKOFF_MAX_WAIT_TIME); i>0 && !this.exit; i--)
                    {
                        this.printStatusError("[Worker "+chunk_downloader_id+"] E "+e.getMessage()+" ("+i+")");
                        
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException ex) {}
                    }
                }

            }while(!this.exit && mc_error);
        }
        
        return this.download_urls[pos];
    }
    
    public synchronized void startSlot()
    {
        MiscTools.swingSetForeground(((JSpinner.DefaultEditor)MiscTools.swingGetEditor(this.getPanel().slots)).getTextField(), Color.black, false);

        this.printDebug("Adding download slot...");

        int chunk_id = this.chunkdownloaders.size()+1;

        ChunkDownloader c = new ChunkDownloader(chunk_id, this);

        this.chunkdownloaders.add(c);

        try {
            
            this.executor.execute(c);
            
        }catch(java.util.concurrent.RejectedExecutionException e){}
    }
    
    public synchronized void stopLastStartedSlot()
    {
        MiscTools.swingSetForeground(((JSpinner.DefaultEditor)MiscTools.swingGetEditor(this.getPanel().slots)).getTextField(), Color.black, false);
        
        this.printDebug("Removing download slot...");
        
        ChunkDownloader chunkdownloader = this.chunkdownloaders.remove(this.chunkdownloaders.size()-1);
        
        chunkdownloader.setExit(true);
    }
    
    public synchronized void stopThisSlot(ChunkDownloader chunkdownloader, boolean error)
    {
        if(this.chunkdownloaders.remove(chunkdownloader))
        {
            if(error) {
                MiscTools.swingSetForeground(((JSpinner.DefaultEditor)MiscTools.swingGetEditor(this.getPanel().slots)).getTextField(), Color.red, false);
            }

            MiscTools.swingSetValue(this.getPanel().slots, ((int)MiscTools.swingGetValue(this.getPanel().slots))-1, true);
        }
    }
   
    public synchronized boolean chunkDownloadersRunning()
    {
        return !this.getChunkdownloaders().isEmpty();
    }
    
    /* NO SINCRONIZADO para evitar que el progress-meter tenga que esperar */
    public void updateProgress(int reads)
    {
        this.prog+=reads;

        MiscTools.swingSetValue(this.getPanel().progress, (int)Math.ceil(this.progress_bar_rate*this.prog), false);     
    }
    
    private void printStatusError(String message)
    {
        MiscTools.swingSetForeground(this.getPanel().status, Color.red, false);
        MiscTools.swingSetText(this.getPanel().status, message, false);
    }
    
    private void printStatusOK(String message)
    {        
        MiscTools.swingSetForeground(this.getPanel().status, new Color(0,128,0), false);
        MiscTools.swingSetText(this.getPanel().status, message, false);
    }
    
    private void printStatus(String message)
    {
        MiscTools.swingSetForeground(this.getPanel().status, Color.BLACK, false);
        MiscTools.swingSetText(this.getPanel().status, message, false);
    }
    
    private boolean verifyFileCBCMAC(String filename) throws FileNotFoundException, IOException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException
    {
        int[] int_key = MiscTools.bin2i32a(MiscTools.UrlBASE642Bin(this.file_key));
        
        int[] iv = new int[2];

        iv[0] = int_key[4];
        iv[1] = int_key[5];
        
        int[] meta_mac = new int[2];
        
        meta_mac[0] = int_key[6];
        meta_mac[1] = int_key[7];
        
        int[] file_mac = {0,0,0,0};
        
        int[] cbc_iv = {0,0,0,0};
     
        Cipher cryptor = CryptTools.genCrypter("AES", "AES/CBC/NoPadding", this.filewriter.getByte_file_key(), MiscTools.i32a2bin(cbc_iv));
 
        File f = new File(filename);
        
        FileInputStream is = new FileInputStream(f);
        
        long chunk_id=1;
        
        long tot=0;
        
        int[] chunk_mac = new int[4];
        
        byte[] chunk_buffer = new byte[8*1024];
        
        byte[] byte_block = new byte[16];
        
        int[] int_block;
        
        int re, reads, to_read;

        try
        {
            while(!this.exit)
            {
                Chunk chunk = new Chunk(chunk_id++, this.size, null);

                tot+=chunk.getSize();
                
                chunk_mac[0]=iv[0];
                chunk_mac[1]=iv[1];
                chunk_mac[2]=iv[0];
                chunk_mac[3]=iv[1];
                
                reads = -2;
                
                do
                {
                    to_read = chunk.getSize() - chunk.getOutputStream().size() >= chunk_buffer.length?chunk_buffer.length:(int)(chunk.getSize() - chunk.getOutputStream().size());

                    re=is.read(chunk_buffer, 0, to_read);

                    chunk.getOutputStream().write(chunk_buffer, 0, re);

                }while(!this.exit && chunk.getOutputStream().size()<chunk.getSize());

                InputStream chunk_is = chunk.getInputStream();

                while(!this.exit && (reads=chunk_is.read(byte_block))!=-1)
                {
                     if(reads<byte_block.length)
                     {
                         for(int i=reads; i<byte_block.length; i++)
                             byte_block[i]=0;
                     }

                    int_block = MiscTools.bin2i32a(byte_block);

                     for(int i=0; i<chunk_mac.length; i++)
                     {
                        chunk_mac[i]^=int_block[i];
                     }

                     chunk_mac = MiscTools.bin2i32a(cryptor.doFinal(MiscTools.i32a2bin(chunk_mac)));
                }

                this.updateProgress((int)chunk.getSize());

                for(int i=0; i<file_mac.length; i++)
                {
                    file_mac[i]^=chunk_mac[i];
                }

                file_mac = MiscTools.bin2i32a(cryptor.doFinal(MiscTools.i32a2bin(file_mac)));
                
            }

        } catch (ChunkInvalidIdException e){}
        
        is.close();
        
        int[] cbc={file_mac[0]^file_mac[1], file_mac[2]^file_mac[3]};

        return (cbc[0] == meta_mac[0] && cbc[1]==meta_mac[1]);
    }
    
    public synchronized void stopDownloader()
    {
        if(!this.exit)
        {
            this.setExit(true);
            
            McDownloaderMain.unRegisterDownload(this.file_link);
            
            if(this.isRetrying_mc_api())
            {
                
                this.printStatus("Retrying cancelled!");
                MiscTools.swingSetEnabled(this.getPanel().stop_button, false, false);
            }
            else if(this.isChecking_cbc())
            {
                
                this.printStatus("Verification cancelled!");
                MiscTools.swingSetEnabled(this.getPanel().stop_button, false, false);
            }
            else
            {
        
                this.printStatus("Stopping download safely, please wait...");
                MiscTools.swingSetEnabled(this.getPanel().speed, false, false);
                MiscTools.swingSetEnabled(this.getPanel().pause_button, false, false);
                MiscTools.swingSetEnabled(this.getPanel().stop_button, false, false);
                MiscTools.swingSetEnabled(this.getPanel().keep_temp, false, false);
                MiscTools.swingSetEnabled(this.getPanel().slots_label, false, false);
                MiscTools.swingSetEnabled(this.getPanel().slots, false, false);
           
                
                if(this.pause) {
                    
                    synchronized(this.pause_lock)
                    {
                        this.pause_lock.notifyAll();
                    }
                }
                
                synchronized(this.executor)
                {
                    this.executor.notify();
                }
            }
        }
    }
    
    public synchronized void emergencyStopDownloader(String reason)
    {
        if(this.fatal_error == null)
        {
            this.fatal_error = reason!=null?reason:"FATAL ERROR!";
            
            this.stopDownloader();
        }
    }
    
    public void hideAllExceptStatus()
    {
        MiscTools.swingSetVisible(this.getPanel().speed, false, false);
        MiscTools.swingSetVisible(this.getPanel().rem_time, false, false);
        MiscTools.swingSetVisible(this.getPanel().slots, false, false);
        MiscTools.swingSetVisible(this.getPanel().slots_label, false, false);
        MiscTools.swingSetVisible(this.getPanel().pause_button, false, false);
        MiscTools.swingSetVisible(this.getPanel().stop_button, false, false);
        
        MiscTools.swingSetVisible(this.getPanel().progress, false, false);
        MiscTools.swingSetVisible(this.getPanel().keep_temp, false, false);
        
        
    }
    
    public long calculateMaxTempFileSize(long size)
    {
        if(size > 3584*1024)
        {
            long reminder = (size - 3584*1024)%(1024*1024);
            
            return reminder==0?size:(size - reminder);
        }
        else
        {
            int i=0, tot=0;
            
            while(tot < size)
            {
                i++;
                tot+=i*128*1024;
            }
            
            return tot==size?size:(tot-i*128*1024);
        }
    }
    
    public void printDebug(String message)
    {
        if(this.debug_mode) {
            this.log_file.write(message+"\n");
            
            System.out.println(message);
        }
    }
    
    public String[] getMegaFileMetadata(String link, McDownloaderMain panel) throws IOException, InterruptedException
    {
 
        String[] file_info=null;
        int retry=0, error_code=0;
        boolean error;

        do
        {
            error=false;
            
            try
            {
                 synchronized(getClass()) {
                     
                    Thread.sleep(ANTI_FLOOD);
                     
                    if( MiscTools.findFirstRegex("://mega(\\.co)?\\.nz/", link, 0) != null)
                    {
                        MegaAPI ma = new MegaAPI();

                        file_info = ma.getMegaFileMetadata(link);
                    }    
                    else
                    {
                        file_info = MegaCrypterAPI.getMegaFileMetadata(link, panel);    
                    }
                     
                }

            }
            catch(MegaAPIException e)
            {
                error=true;

                error_code = Integer.parseInt(e.getMessage());

                    for(long i=MiscTools.getWaitTimeExpBackOff(retry++, EXP_BACKOFF_BASE, EXP_BACKOFF_SECS_RETRY, EXP_BACKOFF_MAX_WAIT_TIME); i>0; i--)
                    {
                        if(error_code == -18)
                        {
                            this.printStatusError("File temporarily unavailable! (Retrying in "+i+" secs...)");
                        }
                        else
                        {
                            this.printStatusError("MegaAPIException error "+e.getMessage()+" (Retrying in "+i+" secs...)");
                        }

                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException ex) {}
                    }
            }
            catch(MegaCrypterAPIException e)
            {
                error=true;

                error_code = Integer.parseInt(e.getMessage());

                switch(error_code)
                { 
                    case 22:
                        this.emergencyStopDownloader("MegaCrypter link is not valid!");
                        break;

                    case 23:
                        this.emergencyStopDownloader("MegaCrypter link is blocked!");
                        break;

                    case 24:
                        this.emergencyStopDownloader("MegaCrypter link has expired!");
                        break;

                    default:

                        this.retrying_mc_api = true;

                        MiscTools.swingSetVisible(this.getPanel().stop_button, true, false);

                        MiscTools.swingSetText(this.getPanel().stop_button, "CANCEL RETRY", false);

                        for(long i=MiscTools.getWaitTimeExpBackOff(retry++, EXP_BACKOFF_BASE, EXP_BACKOFF_SECS_RETRY, EXP_BACKOFF_MAX_WAIT_TIME); i>0 && !this.exit; i--)
                        {
                            if(error_code == -18)
                            {
                                this.printStatusError("File temporarily unavailable! (Retrying in "+i+" secs...)");
                            }
                            else
                            {
                                this.printStatusError("MegaCrypterAPIException error "+e.getMessage()+" (Retrying in "+i+" secs...)");
                            }

                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException ex) {}
                        }
                }
            }
            
        }while(!this.exit && error);
        
        if(!error) {
            MiscTools.swingSetText(this.getPanel().stop_button, "CANCEL DOWNLOAD", false);
            MiscTools.swingSetVisible(this.getPanel().stop_button, false, false);
        }
        
        return file_info;

        
    }
    
    public String getMegaFileDownloadUrl(String link) throws IOException, InterruptedException
    {

        String dl_url=null;
        int retry=0, error_code;
        boolean error;

        do
        {
            error=false;

            try
            {
                synchronized(getClass())
                {
                    Thread.sleep(ANTI_FLOOD);
                    
                    if( MiscTools.findFirstRegex("://mega(\\.co)?\\.nz/", this.file_link, 0) != null)
                    {
                        MegaAPI ma = new MegaAPI();

                        dl_url = ma.getMegaFileDownloadUrl(link);
                    }    
                    else
                    {
                        dl_url = MegaCrypterAPI.getMegaFileDownloadUrl(link, this.file_pass, this.file_noexpire);
                    }
                }
            }
            catch(MegaAPIException e)
            {
                error=true;

                error_code = Integer.parseInt(e.getMessage());

                    for(long i=MiscTools.getWaitTimeExpBackOff(retry++, EXP_BACKOFF_BASE, EXP_BACKOFF_SECS_RETRY, EXP_BACKOFF_MAX_WAIT_TIME); i>0; i--)
                    {
                        if(error_code == -18)
                        {
                            this.printStatusError("File temporarily unavailable! (Retrying in "+i+" secs...)");
                        }
                        else
                        {
                            this.printStatusError("MegaAPIException error "+e.getMessage()+" (Retrying in "+i+" secs...)");
                        }

                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException ex) {}
                    }
            }
            catch(MegaCrypterAPIException e)
            {
                error=true;

                error_code = Integer.parseInt(e.getMessage());

                switch(error_code)
                { 
                    case 22:
                        this.emergencyStopDownloader("MegaCrypter link is not valid!");
                        break;

                    case 23:
                        this.emergencyStopDownloader("MegaCrypter link is blocked!");
                        break;

                    case 24:
                        this.emergencyStopDownloader("MegaCrypter link has expired!");
                        break;

                    default:

                        this.retrying_mc_api = true;

                        MiscTools.swingSetVisible(this.getPanel().stop_button, true, false);

                        MiscTools.swingSetText(this.getPanel().stop_button, "CANCEL RETRY", false);

                        for(long i=MiscTools.getWaitTimeExpBackOff(retry++, EXP_BACKOFF_BASE, EXP_BACKOFF_SECS_RETRY, EXP_BACKOFF_MAX_WAIT_TIME); i>0 && !this.exit; i--)
                        {
                            if(error_code == -18)
                            {
                                this.printStatusError("File temporarily unavailable! (Retrying in "+i+" secs...)");
                            }
                            else
                            {
                                this.printStatusError("MegaCrypterAPIException error "+e.getMessage()+" (Retrying in "+i+" secs...)");
                            }

                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException ex) {}
                        }
                }
            }

        }while(!this.exit && error);
        
        if(!error) {
            MiscTools.swingSetText(this.getPanel().stop_button, "CANCEL DOWNLOAD", false);
            MiscTools.swingSetVisible(this.getPanel().stop_button, false, false);
        }
        
        return dl_url;
        }
}
    
    

