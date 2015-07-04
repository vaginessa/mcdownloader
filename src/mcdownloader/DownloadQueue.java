
package mcdownloader;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DownloadQueue implements Runnable {
    
    protected final ConcurrentLinkedQueue<DownloaderBox> download_boxes_provision_queue;
    
    protected final ConcurrentLinkedQueue<DownloaderBox> download_boxes_start_queue;
    
    protected final ConcurrentLinkedQueue<DownloaderBox> download_boxes_remove_queue;
    
    protected final ConcurrentLinkedQueue<DownloaderBox> download_boxes_finished_queue;
    
    protected final CopyOnWriteArrayList<DownloaderBox> download_boxes_running_list;
    
    protected final CopyOnWriteArrayList<DownloaderBox> download_boxes_cbc_list;
    
    protected final CopyOnWriteArrayList<DownloaderBox> download_boxes_provisioning_list;
    
    protected final McDownloaderMain panel;
    
    protected int provision_pending;
    
    private volatile boolean notified;

    public DownloadQueue(McDownloaderMain panel) {
        
        this.panel = panel;
        this.provision_pending=0;
        this.download_boxes_start_queue = new ConcurrentLinkedQueue();
        this.download_boxes_provision_queue = new ConcurrentLinkedQueue();
        this.download_boxes_remove_queue = new ConcurrentLinkedQueue();
        this.download_boxes_finished_queue = new ConcurrentLinkedQueue();
        this.download_boxes_running_list = new  CopyOnWriteArrayList();
        this.download_boxes_cbc_list = new  CopyOnWriteArrayList();
        this.download_boxes_provisioning_list = new  CopyOnWriteArrayList();
        this.notified = false;
    }
    
    public synchronized void secureNotify()
    {
        this.notified = true;
        notify();
    }
    
    public synchronized void registerProvisionedDownload(DownloaderBox dlbox) 
    {
        this.provision_pending--;
        
        this.download_boxes_provisioning_list.remove(dlbox);
        
        if(dlbox.down.provision_ok) {
            
            this.download_boxes_start_queue.add(dlbox);
            
            if(this.provision_pending==0) {
                this.sortDownloadStartQueue();
            }
            
        } else {
            this.download_boxes_finished_queue.add(dlbox);
        }

        if(this.provision_pending>0) {
            MiscTools.swingSetText(this.panel.status, this.provision_pending+" downloads waiting for provision...", false);
        } else {
            MiscTools.swingSetText(this.panel.status, "", false);
        }
        
        this.secureNotify();
    }
    
    public synchronized void closeAllFinished() 
    {
        while(!this.download_boxes_finished_queue.isEmpty()) {

            this.download_boxes_remove_queue.add(this.download_boxes_finished_queue.poll());
        }
        
        this.secureNotify();
    }
    
    public synchronized void closeAllWaiting() 
    {   
        MiscTools.swingSetEnabled(this.panel.menu_clean_all, false, false);

        while(!this.download_boxes_start_queue.isEmpty()) {

            this.download_boxes_remove_queue.add(this.download_boxes_start_queue.poll());
        }
        
        this.secureNotify();
    }
    
    public synchronized void pauseAll()
    {
        Iterator<DownloaderBox> it = this.download_boxes_running_list.iterator();
      
        while(it.hasNext()) {
            
            DownloaderBox dlbox = it.next();
            
            if(!dlbox.down.isPause()) {
                
                MiscTools.swingSetText(dlbox.status, "Pausing download...", false);
                MiscTools.swingSetEnabled(dlbox.pause_button, false, false);
                MiscTools.swingSetEnabled(dlbox.speed, false, false);
                MiscTools.swingSetEnabled(dlbox.slots_label, false, false);
                MiscTools.swingSetEnabled(dlbox.slots, false, false);
                
                MiscTools.swingSetVisible(dlbox.stop_button, true, false);
                MiscTools.swingSetVisible(dlbox.keep_temp, true, false);
                
                dlbox.down.setPause(true);
            }
        }
            
        this.secureNotify();
    }
        
    public synchronized void sortDownloadStartQueue() 
    {
        ArrayList<DownloaderBox> dl_box_list = new ArrayList();
        
        while(!this.download_boxes_start_queue.isEmpty()) {
        
            dl_box_list.add(this.download_boxes_start_queue.poll());
        }
        
        dl_box_list.sort((DownloaderBox o1, DownloaderBox o2) -> o1.down.file_name.compareToIgnoreCase(o2.down.file_name));
        
        Iterator<DownloaderBox> it = dl_box_list.iterator();
                
        while(it.hasNext()) {
            this.download_boxes_start_queue.add(it.next());
        }
    }
    
    @Override
    public void run() {
        
            try {
                
                while(true)
                {
                    if(!this.download_boxes_provision_queue.isEmpty())
                    {
                        this.provision_pending+=this.download_boxes_provision_queue.size();

                        MiscTools.swingSetText(this.panel.status, this.provision_pending + " downloads waiting for provision...", false);

                        while(!this.download_boxes_provision_queue.isEmpty())
                        {
                            DownloaderBox dlbox = this.download_boxes_provision_queue.poll();

                            Executors.newCachedThreadPool().execute(() -> {

                                dlbox.down.provisionDownload();

                                this.registerProvisionedDownload(dlbox);

                            });
                            
                            this.download_boxes_provisioning_list.add(dlbox);
                        }
                    }

                    while(!this.download_boxes_remove_queue.isEmpty()) {
                    
                        MiscTools.swingSetText(this.panel.status, "Removing ("+this.download_boxes_remove_queue.size()+")...", false);
                        
                        DownloaderBox dlbox = this.download_boxes_remove_queue.poll();

                        this.download_boxes_start_queue.remove(dlbox);

                        this.download_boxes_running_list.remove(dlbox);
                        
                        this.download_boxes_cbc_list.remove(dlbox);

                        this.download_boxes_finished_queue.remove(dlbox);

                        if(dlbox.down.provision_ok) {

                            this.panel.unRegisterDownload(dlbox.down.file_link);
                        }
                        
                        if(this.download_boxes_remove_queue.isEmpty()){
                            MiscTools.swingSetText(this.panel.status, "", false);
                        }
                    }

                    
                    while(!this.download_boxes_start_queue.isEmpty() && this.download_boxes_running_list.size() < this.panel.max_dl) {

                        DownloaderBox dlbox = this.download_boxes_start_queue.poll();

                        this.download_boxes_running_list.add(dlbox);

                        dlbox.startDownload(this.panel.default_slots);
                    }

                    
                    this.panel.jPanel2.removeAll();

                    Iterator<DownloaderBox> it = this.download_boxes_running_list.iterator();
                    
                    MiscTools.swingSetVisible(this.panel.pause_all, false, false);

                    while(it.hasNext()) {
                        
                        DownloaderBox dlbox = it.next();
                        
                        this.panel.jPanel2.add(dlbox);
                        
                        if(dlbox.down.downloading && !dlbox.down.isPause()) {

                            MiscTools.swingSetVisible(this.panel.pause_all, true, false);

                        }
                    }
                    
                    it = this.download_boxes_provisioning_list.iterator();

                    while(it.hasNext()) {
                        this.panel.jPanel2.add(it.next());
                    }
                    
                    it = this.download_boxes_cbc_list.iterator();

                    while(it.hasNext()) {
                        this.panel.jPanel2.add(it.next());
                    }
                    
                    it = this.download_boxes_finished_queue.iterator();

                    while(it.hasNext()) {
                        this.panel.jPanel2.add(it.next());
                    }

                    it = this.download_boxes_start_queue.iterator();

                    while(it.hasNext()) {
                        this.panel.jPanel2.add(it.next());
                    }
                    
                    MiscTools.swingSetViewportView(this.panel.jScrollPane1, this.panel.jPanel2, false);

                    if(this.download_boxes_finished_queue.size() > 0) {

                        MiscTools.swingSetText(this.panel.close_all_finished, "Close all finished ("+this.download_boxes_finished_queue.size()+")" , false);
                        MiscTools.swingSetVisible(this.panel.close_all_finished, true, false);

                    } else {

                        MiscTools.swingSetVisible(this.panel.close_all_finished, false, false);
                    }
                    
                    if(this.download_boxes_start_queue.size() > 0)
                    {
                    
                        MiscTools.swingSetEnabled(this.panel.menu_clean_all, true, false);
                    
                    } else{
                        
                        MiscTools.swingSetEnabled(this.panel.menu_clean_all, false, false);
                    }
                    
                    synchronized (this) {

                        while(!this.notified) {

                            wait();

                        }

                        this.notified = false;
                    }
            }

            } catch (InterruptedException ex) {
                Logger.getLogger(DownloadQueue.class.getName()).log(Level.SEVERE, null, ex);
            }
        
        }
    
    
}
