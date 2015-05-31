
package mcdownloader;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;


public class DownloaderBox extends javax.swing.JPanel {

    protected Downloader down;
    
    protected final boolean provision_antiflood;
    
    private final McDownloaderMain panel;
    
    public McDownloaderMain getPanel(){
        return this.panel;
    }
    
    /**
     * Creates new form NewJPanel
     */
    public DownloaderBox(McDownloaderMain panel, String url, String download_path, String filename, String filekey, Long filesize, String filepass, String filenoexpire, boolean restart) {
        initComponents();
        
        Font font = null;
        
        try {
            font = Font.createFont(Font.TRUETYPE_FONT, this.getClass().getResourceAsStream("Gochi.ttf"));
        } catch (FontFormatException | IOException ex) {
            Logger.getLogger(DownloaderBox.class.getName()).log(Level.SEVERE, null, ex);
        }
       
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        ge.registerFont(font);

        mcdownloader.MiscTools.updateFont(this.status, font, Font.PLAIN);
        mcdownloader.MiscTools.updateFont(this.rem_time, font, Font.PLAIN);
        mcdownloader.MiscTools.updateFont(this.speed, font, Font.BOLD);
        mcdownloader.MiscTools.updateFont(this.progress, font, Font.PLAIN);
        mcdownloader.MiscTools.updateFont(this.slots_label, font, Font.BOLD);
        mcdownloader.MiscTools.updateFont(this.slots, font, Font.PLAIN);
        mcdownloader.MiscTools.updateFont(this.pause_button, font, Font.BOLD);
        mcdownloader.MiscTools.updateFont(this.stop_button, font, Font.BOLD);
   
        mcdownloader.MiscTools.updateFont(this.keep_temp, font, Font.PLAIN);
        mcdownloader.MiscTools.updateFont(this.fname_label, font, Font.PLAIN);
        mcdownloader.MiscTools.updateFont(this.closebutton, font, Font.PLAIN);
        mcdownloader.MiscTools.updateFont(this.copy_button, font, Font.PLAIN);
        mcdownloader.MiscTools.updateFont(this.restart_download, font, Font.PLAIN);
        
        this.slots.setVisible(false);
        this.slots_label.setVisible(false);
        this.panel = panel;
        this.slots.setModel(new SpinnerNumberModel(this.panel.default_slots, Downloader.MIN_WORKERS, Downloader.MAX_WORKERS, 1));
        ((JSpinner.DefaultEditor)this.slots.getEditor()).getTextField().setEditable(false);
        
        this.pause_button.setVisible(false);
        this.stop_button.setVisible(false);
        this.speed.setForeground(new Color(0,128,255));
        this.speed.setVisible(false);
        this.rem_time.setVisible(false);
        this.progress.setVisible(false);
        this.keep_temp.setVisible(false);
        this.fname_label.setVisible(false);
        this.closebutton.setVisible(false);
        this.copy_button.setVisible(false);
        this.restart_download.setVisible(false);
        this.provision_antiflood = (filename==null);
        this.down = new Downloader(this, url, download_path, filename, filekey, filesize, filepass, filenoexpire, restart, false);   
    }

    
    public void startDownload(int slots)
    {
        this.down.slots = slots;
        
        Executors.newCachedThreadPool().execute(this.down);
        
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        status = new javax.swing.JLabel();
        slots_label = new javax.swing.JLabel();
        slots = new javax.swing.JSpinner();
        rem_time = new javax.swing.JLabel();
        speed = new javax.swing.JLabel();
        progress = new javax.swing.JProgressBar();
        pause_button = new javax.swing.JButton();
        stop_button = new javax.swing.JButton();
        keep_temp = new javax.swing.JCheckBox();
        fname_label = new javax.swing.JLabel();
        closebutton = new javax.swing.JButton();
        copy_button = new javax.swing.JButton();
        restart_download = new javax.swing.JButton();

        setBorder(new javax.swing.border.LineBorder(new java.awt.Color(153, 204, 255), 4, true));

        status.setFont(new java.awt.Font("Dialog", 1, 16)); // NOI18N
        status.setText("status");

        slots_label.setFont(new java.awt.Font("Dialog", 1, 16)); // NOI18N
        slots_label.setText("Slots");

        slots.setFont(new java.awt.Font("Dialog", 1, 16)); // NOI18N
        slots.setToolTipText("Slots");
        slots.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                slotsStateChanged(evt);
            }
        });

        rem_time.setFont(new java.awt.Font("Verdana", 1, 16)); // NOI18N
        rem_time.setText("remaining_time");

        speed.setFont(new java.awt.Font("Verdana", 3, 24)); // NOI18N
        speed.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        speed.setText("speed");

        progress.setFont(new java.awt.Font("Verdana", 1, 18)); // NOI18N

        pause_button.setBackground(new java.awt.Color(255, 153, 0));
        pause_button.setFont(new java.awt.Font("Verdana", 1, 12)); // NOI18N
        pause_button.setForeground(java.awt.Color.white);
        pause_button.setText("PAUSE DOWNLOAD");
        pause_button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                pause_buttonMouseClicked(evt);
            }
        });

        stop_button.setBackground(new java.awt.Color(255, 0, 0));
        stop_button.setForeground(java.awt.Color.white);
        stop_button.setText("CANCEL DOWNLOAD");
        stop_button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                stop_buttonMouseClicked(evt);
            }
        });

        keep_temp.setFont(new java.awt.Font("Dialog", 1, 16)); // NOI18N
        keep_temp.setSelected(true);
        keep_temp.setText("Keep temp file");

        fname_label.setFont(new java.awt.Font("Dialog", 1, 16)); // NOI18N
        fname_label.setForeground(new java.awt.Color(51, 51, 255));
        fname_label.setText("file_name");

        closebutton.setText("Close");
        closebutton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                closebuttonActionPerformed(evt);
            }
        });

        copy_button.setText("Copy mc link");
        copy_button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                copy_buttonActionPerformed(evt);
            }
        });

        restart_download.setBackground(new java.awt.Color(51, 51, 255));
        restart_download.setForeground(new java.awt.Color(255, 255, 255));
        restart_download.setText("Restart");
        restart_download.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                restart_downloadActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(progress, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(speed, javax.swing.GroupLayout.PREFERRED_SIZE, 340, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 77, Short.MAX_VALUE)
                        .addComponent(pause_button))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(rem_time)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(closebutton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(restart_download)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(stop_button)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(keep_temp)
                        .addGap(1, 1, 1))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(status, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(slots_label)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(slots, javax.swing.GroupLayout.PREFERRED_SIZE, 49, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(fname_label)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(copy_button)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(slots, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(slots_label)
                    .addComponent(status))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(fname_label)
                    .addComponent(copy_button, javax.swing.GroupLayout.Alignment.TRAILING))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(rem_time)
                .addGap(6, 6, 6)
                .addComponent(progress, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(speed)
                    .addComponent(pause_button))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(keep_temp)
                    .addComponent(stop_button)
                    .addComponent(closebutton)
                    .addComponent(restart_download))
                .addContainerGap(14, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void slotsStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_slotsStateChanged

        
        Executors.newCachedThreadPool().execute(() -> {

            if(!this.down.isExit()) {

            int sl = (int)MiscTools.swingGetValue(this.slots);

            int cdownloaders = this.down.getChunkdownloaders().size();

            if(sl != cdownloaders) {

                if(sl > cdownloaders) {

                    this.down.startSlot();

                } else {

                    this.down.stopLastStartedSlot();
                }
            }

        }});
    }//GEN-LAST:event_slotsStateChanged

   
    
    private void pause_buttonMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_pause_buttonMouseClicked

        if(this.down.isPause()) {

            this.status.setText("Resuming download ...");
            this.pause_button.setEnabled(false);
           
            this.down.setPause(false);

            synchronized(this.down.getPauseLock()) {
                this.down.getPauseLock().notifyAll();
            }

            this.down.setPaused_workers(0);

            this.status.setText("Downloading file from mega.co.nz ...");
            this.speed.setEnabled(true);
            this.slots_label.setEnabled(true);
            this.slots.setEnabled(true);
            
            this.stop_button.setVisible(false);
            this.keep_temp.setVisible(false);
            this.pause_button.setEnabled(true);
            this.pause_button.setText("PAUSE DOWNLOAD");
            
            MiscTools.swingSetVisible(this.panel.pause_all, true, false);

        } else {

            this.status.setText("Pausing download...");
            this.pause_button.setEnabled(false);
            this.speed.setEnabled(false);
            this.slots_label.setEnabled(false);
            this.slots.setEnabled(false);
            
            this.stop_button.setVisible(true);
            this.keep_temp.setVisible(true);
            this.down.setPause(true);
            
        }
        
        this.panel.download_queue.secureNotify();
    }//GEN-LAST:event_pause_buttonMouseClicked

    private void stop_buttonMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_stop_buttonMouseClicked

        if(!this.down.isExit()) {
            this.down.stopDownloader();
        }
    }//GEN-LAST:event_stop_buttonMouseClicked

    private void closebuttonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_closebuttonActionPerformed
        // TODO add your handling code here:

        this.panel.download_queue.download_boxes_remove_queue.add(this);
       
        this.panel.download_queue.secureNotify();

    }//GEN-LAST:event_closebuttonActionPerformed

    private void copy_buttonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_copy_buttonActionPerformed
        // TODO add your handling code here:
        
        MiscTools.copyTextToClipboard(this.down.file_link);
        
        JOptionPane.showMessageDialog(this.getPanel(), "Link was copied to clipboard!");
    }//GEN-LAST:event_copy_buttonActionPerformed

    private void restart_downloadActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_restart_downloadActionPerformed
        // TODO add your handling code here:
        
        this.panel.download_queue.restartDownload(this);
    }//GEN-LAST:event_restart_downloadActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    protected javax.swing.JButton closebutton;
    protected javax.swing.JButton copy_button;
    protected javax.swing.JLabel fname_label;
    protected javax.swing.JCheckBox keep_temp;
    protected javax.swing.JButton pause_button;
    protected javax.swing.JProgressBar progress;
    protected javax.swing.JLabel rem_time;
    protected javax.swing.JButton restart_download;
    protected javax.swing.JSpinner slots;
    protected javax.swing.JLabel slots_label;
    protected javax.swing.JLabel speed;
    protected javax.swing.JLabel status;
    protected javax.swing.JButton stop_button;
    // End of variables declaration//GEN-END:variables
}
