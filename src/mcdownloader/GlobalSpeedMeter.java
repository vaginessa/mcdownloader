package mcdownloader;

import java.util.ArrayList;
import java.util.Iterator;

public class GlobalSpeedMeter implements Runnable
{
    private final McDownloaderMain panel;
    private final ArrayList<SpeedMeter> speedmeters;
    private volatile boolean exit;
    
    public static final int SLEEP = 1000;
  
    GlobalSpeedMeter(McDownloaderMain panel)
    {
        this.panel = panel;
        this.speedmeters = new ArrayList();
    }
    
    public synchronized void registerSpeedMeter(SpeedMeter speed) {
        
        this.speedmeters.add(speed);
    }
    
    public synchronized void unregisterSpeedMeter(SpeedMeter speed) {
        this.speedmeters.remove(speed);
    }
    
    private synchronized int calcSpeed() {
    
        int sp = 0;

        Iterator<SpeedMeter> it = this.speedmeters.iterator();

        while(it.hasNext())
        {
            SpeedMeter speed = it.next();
            
            sp+=speed.getLastSpeed();
        }

        return sp;
    }
    
    @Override
    public void run()
    { 
        long p, sp;
        int no_data_count;
                
        MiscTools.swingSetText(this.panel.global_speed, "------ KB/s", false);
        MiscTools.swingSetVisible(this.panel.global_speed, true, false);
        
    
            try
            {
                while(true)
                {
                    synchronized (this) {
                        wait();
                    }

                    if(!this.exit)
                    {
                        sp = this.calcSpeed();

                        if(sp > 0) {

                            MiscTools.swingSetText(this.panel.global_speed, String.valueOf(sp)+" KB/s", false);

                        }
                        else 
                        {
                            MiscTools.swingSetText(this.panel.global_speed, "------ KB/s", false);

                        }
                    }
                }
            }
            catch (InterruptedException ex)
            {
            }
        
    }
}
