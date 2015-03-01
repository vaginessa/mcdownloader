package mcdownloader;

import java.util.concurrent.TimeUnit;

public class SpeedMeter implements Runnable
{
    private long progress;
    private final Downloader down;
    public int lastSpeed;
    
    public static final int SLEEP = 3000;
    public static final int NO_DATA_SLEEP = 250;
  
    SpeedMeter(Downloader down)
    {
        this.down = down;
        this.progress = down.getProg();
        this.lastSpeed=0;
    }
    
    public synchronized int getLastSpeed()
    {
        return this.lastSpeed;
    }
    
    public synchronized void setLastSpeed(int speed)
    {
        this.lastSpeed = speed;
    }
    
    @Override
    public void run()
    { 
        long p, sp;
        int no_data_count;
                
        MiscTools.swingSetText(this.down.getPanel().speed, "------ KB/s", false);
        MiscTools.swingSetText(this.down.getPanel().rem_time, "--d --:--:--", false);
        MiscTools.swingSetVisible(this.down.getPanel().speed, true, false);
        MiscTools.swingSetVisible(this.down.getPanel().rem_time, true, false);
        

            try
            {
                while(true)
                {
                    Thread.sleep(SpeedMeter.SLEEP);

                no_data_count=0;

                do
                {
                    p = this.down.getProg();

                    sp = (p - this.progress)/(1024*((SpeedMeter.SLEEP/1000) + no_data_count*(SpeedMeter.NO_DATA_SLEEP/1000)));

                    if(sp > 0) {
                        this.progress = p;
                        MiscTools.swingSetText(this.down.getPanel().speed, String.valueOf(sp)+" KB/s", false);
                        MiscTools.swingSetText(this.down.getPanel().rem_time, this.calculateRemTime((long)Math.floor(this.down.getFile_Size()-p)/(sp*1024)), false);
                    }
                    else if(!this.down.isPause())
                    {
                        no_data_count++;
                        MiscTools.swingSetText(this.down.getPanel().speed, "------ KB/s", false);
                        MiscTools.swingSetText(this.down.getPanel().rem_time, "--d --:--:--", false);
                        Thread.sleep(SpeedMeter.NO_DATA_SLEEP);
                    }

                    this.setLastSpeed((int)sp);

                    synchronized(this.down.getPanel().getPanel().global_speed_meter) {
                        this.down.getPanel().getPanel().global_speed_meter.notify();
                    } 

                }while(sp == 0 && !this.down.isPause()); 

                if(this.down.isPause()) {

                    MiscTools.swingSetText(this.down.getPanel().speed, "------ KB/s", false);
                    MiscTools.swingSetText(this.down.getPanel().rem_time, "--d --:--:--", false);

                    this.setLastSpeed(0);

                    synchronized(this.down.getPanel().getPanel().global_speed_meter) {
                        this.down.getPanel().getPanel().global_speed_meter.notify();
                    }

                    synchronized(this.down.getPauseLock()) {
                        this.down.getPauseLock().wait();
                    }
                }
                }
            }
            catch (InterruptedException ex)
            {
                
            }
        
    }
    
    private String calculateRemTime(long seconds)
    {
        int days = (int) TimeUnit.SECONDS.toDays(seconds);
        
        long hours = TimeUnit.SECONDS.toHours(seconds) -
                     TimeUnit.DAYS.toHours(days);
        
        long minutes = TimeUnit.SECONDS.toMinutes(seconds) - 
                      TimeUnit.DAYS.toMinutes(days) -
                      TimeUnit.HOURS.toMinutes(hours);
        
        long secs = TimeUnit.SECONDS.toSeconds(seconds) -
                      TimeUnit.DAYS.toSeconds(days) -
                      TimeUnit.HOURS.toSeconds(hours) - 
                      TimeUnit.MINUTES.toSeconds(minutes);
        
        return String.format("%dd %d:%02d:%02d", days, hours, minutes, secs);
    }
}
