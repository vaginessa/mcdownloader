package mcdownloader;

public class ProgressMeter implements Runnable
{
    private final Downloader down;
    private volatile boolean exit;
  
    ProgressMeter(Downloader down)
    {
        this.down = down;
        this.exit = false;
    }
    
    public void setExit(boolean value)
    {
        this.exit = value;
    }
    
    @Override
    public void run()
    {  
        try
        {
            while(!this.exit)
            {
                Integer reads;

                while((reads=this.down.getChunkPartialReads().poll())!=null)
                {
                    this.down.updateProgress(reads);
                }

                synchronized(this.down.getChunkPartialReads())
                {
                    this.down.getChunkPartialReads().wait();
                }
            }
        }
        catch (InterruptedException ex)
        {
            this.exit = true;
        }
        
        this.down.printDebug("Progressmeter: bye bye");
    }

}
