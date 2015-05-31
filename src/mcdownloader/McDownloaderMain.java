
package mcdownloader;


import java.awt.AWTException;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;


public class McDownloaderMain extends javax.swing.JFrame {

    public static final String VERSION="beta 0.3.1";
    public static final int MAX_DOWNLOADS_DEFAULT = 2;
    public static final int MAX_DOWNLOADS_MAX = 20;
    public static final String LOCK_FILE="mcdownloader.lock";
    public static final boolean VERIFY_CBC_MAC=false;
    public static final String USER_AGENT="Mozilla/5.0 (X11; Linux x86_64; rv:38.0) Gecko/20100101 Firefox/38.0";
    
    
    protected final GlobalSpeedMeter global_speed_meter;
    protected final DownloadQueue download_queue;
    protected int max_dl, default_slots;
    protected String default_download_path;
    protected ArrayList<String> passwords;
    
    /**
     * Creates new form mainBox
     */
    public McDownloaderMain() throws SQLException {
        initComponents();
        
        this.setTitle("MCDownloader " + VERSION);
        
        this.checkAppIsRunning();
        
        try {
            this.trayIcon();
        } catch (AWTException ex) {
            Logger.getLogger(McDownloaderMain.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        Font font = null;
        
        try {
            font = Font.createFont(Font.TRUETYPE_FONT, this.getClass().getResourceAsStream("Gochi.ttf"));
        } catch (FontFormatException | IOException ex) {
            Logger.getLogger(DownloaderBox.class.getName()).log(Level.SEVERE, null, ex);
        }
       
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        ge.registerFont(font);
        
        
        mcdownloader.MiscTools.updateFont(this.jMenu1, font, Font.PLAIN);
        mcdownloader.MiscTools.updateFont(this.jMenu2, font, Font.PLAIN);
        mcdownloader.MiscTools.updateFont(this.jMenu4, font, Font.PLAIN);
        mcdownloader.MiscTools.updateFont(this.jMenuItem2, font, Font.PLAIN);
        mcdownloader.MiscTools.updateFont(this.jMenuItem5, font, Font.PLAIN);
        mcdownloader.MiscTools.updateFont(this.jMenuItem1, font, Font.PLAIN);
        mcdownloader.MiscTools.updateFont(this.jMenuItem3, font, Font.PLAIN);
        mcdownloader.MiscTools.updateFont(this.jMenuItem4, font, Font.PLAIN);
        mcdownloader.MiscTools.updateFont(this.menu_clean_all, font, Font.PLAIN);
        mcdownloader.MiscTools.updateFont(this.global_speed, font, Font.BOLD);
        mcdownloader.MiscTools.updateFont(this.proxy_status, font, Font.BOLD);
        mcdownloader.MiscTools.updateFont(this.status, font, Font.BOLD);
        mcdownloader.MiscTools.updateFont(this.close_all_finished, font, Font.BOLD);
        mcdownloader.MiscTools.updateFont(this.pause_all, font, Font.BOLD);
        
        MiscTools.swingSetVisible(this.close_all_finished, false, false);
        MiscTools.swingSetVisible(this.pause_all, false, false);
        MiscTools.swingSetEnabled(this.menu_clean_all, false, false);
        
        /* Cargamos el video streamer */
        ProxyStreamServer streamserver = new ProxyStreamServer(this);
        
        try {
            
            streamserver.start(ProxyStreamServer.DEFAULT_PORT, "/video");

        }
        catch (Exception ex) {
            Logger.getLogger(DownloaderBox.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        this.global_speed.setForeground(new Color(0,128,255));
        
        this.passwords = new ArrayList();
        
        this.download_queue = new DownloadQueue(this);
        
        this.global_speed_meter = new GlobalSpeedMeter(this);
        
        MiscTools.swingSetVisible(this.global_speed, false, false);
        
        this.initSqlite();
        
        this.loadSettings();
        
        Executors.newCachedThreadPool().execute(this.global_speed_meter);
        
        Executors.newCachedThreadPool().execute(this.download_queue);
        
        this.resumeDownloads();
    }

    protected final void initSqlite() {
        
        String nombreDB= "mcdownloader.db";
        
        try {
            
            Class.forName("org.sqlite.JDBC");
           
            try (Connection conn = DriverManager.getConnection("jdbc:sqlite:"+nombreDB)
            ) {
                Statement stat = (Statement) conn.createStatement();
                stat.executeUpdate("CREATE TABLE IF NOT EXISTS downloads(url TEXT, path VARCHAR(255), filename VARCHAR(255), filekey VARCHAR(255), filesize UNSIGNED BIG INT, filepass VARCHAR(64), filenoexpire VARCHAR(64), PRIMARY KEY ('url'), UNIQUE(path, filename));");
                stat.executeUpdate("CREATE TABLE IF NOT EXISTS settings(key VARCHAR(255), value TEXT, PRIMARY KEY('key'));");
                
                stat.close();
                conn.close();
            }
        }catch(ClassNotFoundException | SQLException ex){
             ex.printStackTrace();
        }
    }
    
    protected final void loadSettings()
    {
        String def_slots = getValueFromDB("default_slots");
        
        if(def_slots != null) {
            this.default_slots = Integer.parseInt(def_slots);
        } else {
            this.default_slots = Downloader.DEFAULT_WORKERS;
        }

        String max_downloads = getValueFromDB("max_downloads");

        if(max_downloads != null) {
            this.max_dl = Integer.parseInt(max_downloads);
        } else {
            this.max_dl=McDownloaderMain.MAX_DOWNLOADS_DEFAULT;
        }

        this.default_download_path = getValueFromDB("default_download_dir");

        if(this.default_download_path == null) {
            this.default_download_path = ".";
        }
    }
    
    protected final void resumeDownloads() {
        
        String nombreDB= "mcdownloader.db";
        
        try {
            
            Class.forName("org.sqlite.JDBC");
           
            try (Connection conn = DriverManager.getConnection("jdbc:sqlite:"+nombreDB)
            ) {
                
                Statement stat = (Statement) conn.createStatement();
                
                ResultSet res = stat.executeQuery("SELECT * FROM downloads;");
          
                while(res.next()) {

                    DownloaderBox dlbox = new DownloaderBox(this, res.getString(1), res.getString(2), res.getString(3), res.getString(4), res.getLong(5), res.getString(6), res.getString(7), false);

                    this.download_queue.download_boxes_provision_queue.add(dlbox);
                }
                
                stat.close();
                
                conn.close();
                 
                synchronized(this.download_queue.download_boxes_provision_queue) {
                    this.download_queue.download_boxes_provision_queue.notify();
                }
                

            }
        }catch(ClassNotFoundException | SQLException ex){
             ex.printStackTrace();
        }
    }
    
    protected final void checkAppIsRunning()
    {
        File lock = new File(LOCK_FILE);
        
        if(lock.exists()) {
            
            Object[] options = {"Yes, load it anyway",
                            "No, quit"};
        
            int n = JOptionPane.showOptionDialog(this,
            "It seems MCDownloader is already running. Do you want to continue?",
            "Warning!",
            JOptionPane.YES_NO_CANCEL_OPTION,
            JOptionPane.QUESTION_MESSAGE,
            null,
            options,
            options[1]);
        
            if(n==1) {
                System.exit(0);
            }
        }
        
        try {
            lock.createNewFile();
            lock.deleteOnExit();
        } catch (IOException ex) {
            Logger.getLogger(McDownloaderMain.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        logo = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jPanel2 = new javax.swing.JPanel();
        global_speed = new javax.swing.JLabel();
        proxy_status = new javax.swing.JLabel();
        jLabel1 = new javax.swing.JLabel();
        status = new javax.swing.JLabel();
        close_all_finished = new javax.swing.JButton();
        pause_all = new javax.swing.JButton();
        jMenuBar1 = new javax.swing.JMenuBar();
        jMenu1 = new javax.swing.JMenu();
        jMenuItem2 = new javax.swing.JMenuItem();
        jSeparator1 = new javax.swing.JPopupMenu.Separator();
        jMenuItem3 = new javax.swing.JMenuItem();
        jSeparator2 = new javax.swing.JPopupMenu.Separator();
        menu_clean_all = new javax.swing.JMenuItem();
        jSeparator3 = new javax.swing.JPopupMenu.Separator();
        jMenuItem5 = new javax.swing.JMenuItem();
        jMenu2 = new javax.swing.JMenu();
        jMenuItem1 = new javax.swing.JMenuItem();
        jMenu4 = new javax.swing.JMenu();
        jMenuItem4 = new javax.swing.JMenuItem();

        setTitle("MCDownloader");

        logo.setIcon(new javax.swing.ImageIcon(getClass().getResource("/mcdownloader/mega_crypter.png"))); // NOI18N

        jPanel2.setLayout(new javax.swing.BoxLayout(jPanel2, javax.swing.BoxLayout.Y_AXIS));
        jScrollPane1.setViewportView(jPanel2);

        global_speed.setFont(new java.awt.Font("Dialog", 1, 54)); // NOI18N
        global_speed.setText("Speed");

        status.setFont(new java.awt.Font("Dialog", 1, 14)); // NOI18N

        close_all_finished.setText("Close all finished");
        close_all_finished.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                close_all_finishedActionPerformed(evt);
            }
        });

        pause_all.setBackground(new java.awt.Color(255, 153, 0));
        pause_all.setForeground(new java.awt.Color(255, 255, 255));
        pause_all.setText("PAUSE ALL");
        pause_all.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                pause_allActionPerformed(evt);
            }
        });

        jMenu1.setText("File");
        jMenu1.setFont(new java.awt.Font("Dialog", 0, 16)); // NOI18N

        jMenuItem2.setFont(new java.awt.Font("Dialog", 0, 16)); // NOI18N
        jMenuItem2.setText("New download");
        jMenuItem2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem2ActionPerformed(evt);
            }
        });
        jMenu1.add(jMenuItem2);
        jMenu1.add(jSeparator1);

        jMenuItem3.setFont(new java.awt.Font("Dialog", 0, 16)); // NOI18N
        jMenuItem3.setText("Hide to tray");
        jMenuItem3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem3ActionPerformed(evt);
            }
        });
        jMenu1.add(jMenuItem3);
        jMenu1.add(jSeparator2);

        menu_clean_all.setFont(new java.awt.Font("Dialog", 0, 16)); // NOI18N
        menu_clean_all.setText("Clean all waiting");
        menu_clean_all.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menu_clean_allActionPerformed(evt);
            }
        });
        jMenu1.add(menu_clean_all);
        jMenu1.add(jSeparator3);

        jMenuItem5.setFont(new java.awt.Font("Dialog", 0, 16)); // NOI18N
        jMenuItem5.setText("EXIT");
        jMenuItem5.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem5ActionPerformed(evt);
            }
        });
        jMenu1.add(jMenuItem5);

        jMenuBar1.add(jMenu1);

        jMenu2.setText("Edit");
        jMenu2.setFont(new java.awt.Font("Dialog", 0, 16)); // NOI18N

        jMenuItem1.setFont(new java.awt.Font("Dialog", 0, 16)); // NOI18N
        jMenuItem1.setText("Settings");
        jMenuItem1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem1ActionPerformed(evt);
            }
        });
        jMenu2.add(jMenuItem1);

        jMenuBar1.add(jMenu2);

        jMenu4.setText("Help");
        jMenu4.setFont(new java.awt.Font("Dialog", 0, 16)); // NOI18N

        jMenuItem4.setFont(new java.awt.Font("Dialog", 0, 16)); // NOI18N
        jMenuItem4.setText("About");
        jMenuItem4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem4ActionPerformed(evt);
            }
        });
        jMenu4.add(jMenuItem4);

        jMenuBar1.add(jMenu4);

        setJMenuBar(jMenuBar1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(global_speed, javax.swing.GroupLayout.PREFERRED_SIZE, 404, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(logo))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jScrollPane1)
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addComponent(proxy_status, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addGap(6, 6, 6)
                                .addComponent(pause_all)))
                        .addContainerGap())))
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(171, 171, 171)
                        .addComponent(jLabel1, javax.swing.GroupLayout.DEFAULT_SIZE, 202, Short.MAX_VALUE)
                        .addGap(148, 148, 148))
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(status, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(close_all_finished)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jLabel1)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(close_all_finished)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(6, 6, 6)
                        .addComponent(status, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 247, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(6, 6, 6)
                        .addComponent(proxy_status, javax.swing.GroupLayout.PREFERRED_SIZE, 11, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(pause_all))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(global_speed)
                    .addComponent(logo))
                .addGap(0, 0, 0))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jMenuItem2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem2ActionPerformed
        // TODO add your handling code here:
        
        LinkGrabber dialog = new LinkGrabber(this, true, this.default_download_path);

        dialog.setLocationRelativeTo(this);

        dialog.setVisible(true);
        
        String dl_path = dialog.download_path;
        
        if(dialog.download) {
            
            
            ArrayList<String> urls = mcdownloader.MiscTools.findAllRegex("https?://[^/]+/#?!.+![^\r\n]+", dialog.jTextArea1.getText(), 0);
           
            for (String url : urls ) {

                DownloaderBox dlbox = new DownloaderBox(this, url, dl_path, null, null, null,null,null, false);
                
                this.download_queue.download_boxes_provision_queue.add(dlbox);

            }
            
            synchronized(this.download_queue.download_boxes_provision_queue) {
                    this.download_queue.download_boxes_provision_queue.notify();
                }
        }
        
        dialog.dispose();
    }//GEN-LAST:event_jMenuItem2ActionPerformed

    private void jMenuItem1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem1ActionPerformed
        // TODO add your handling code here:
        
        Settings dialog = new Settings(this, true);
       
        dialog.setLocationRelativeTo(this);
        
        dialog.setVisible(true);
       
        this.loadSettings();

        this.download_queue.secureNotify();

    }//GEN-LAST:event_jMenuItem1ActionPerformed

    private void jMenuItem3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem3ActionPerformed
        // TODO add your handling code here:
        
        this.dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
    }//GEN-LAST:event_jMenuItem3ActionPerformed

    private void jMenuItem4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem4ActionPerformed
        // TODO add your handling code here:
        
        About dialog = new About(this, true);
       
        dialog.setLocationRelativeTo(this);
        
        dialog.setVisible(true);
    }//GEN-LAST:event_jMenuItem4ActionPerformed

    private void jMenuItem5ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem5ActionPerformed
        // TODO add your handling code here:
        
        System.exit(0);
    }//GEN-LAST:event_jMenuItem5ActionPerformed

    private void close_all_finishedActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_close_all_finishedActionPerformed

        this.download_queue.closeAllFinished();
    }//GEN-LAST:event_close_all_finishedActionPerformed

    private void menu_clean_allActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menu_clean_allActionPerformed
        // TODO add your handling code here:
        
        Object[] options = {"No",
                            "Yes"};
        
            int n = JOptionPane.showOptionDialog(this,
            "Remove all waiting downloads?",
            "Warning!",
            JOptionPane.YES_NO_CANCEL_OPTION,
            JOptionPane.QUESTION_MESSAGE,
            null,
            options,
            options[0]);
        
            if(n==1) {
                this.download_queue.closeAllWaiting();
            }
    }//GEN-LAST:event_menu_clean_allActionPerformed

    private void pause_allActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pause_allActionPerformed
        // TODO add your handling code here:
        
        this.download_queue.pauseAll();
    }//GEN-LAST:event_pause_allActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(McDownloaderMain.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(McDownloaderMain.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(McDownloaderMain.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(McDownloaderMain.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    new McDownloaderMain().setVisible(true);
                } catch (SQLException ex) {
                    Logger.getLogger(McDownloaderMain.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });
    }
    
    protected synchronized static void registerDownload(String url, String path, String filename, String filekey, Long size, String filepass, String filenoexpire) throws SQLException {
        
        String nombreDB= "mcdownloader.db";
        
        try {
            
            Class.forName("org.sqlite.JDBC");
           
            try (Connection conn = DriverManager.getConnection("jdbc:sqlite:"+nombreDB)
            ) {
                PreparedStatement ps = conn.prepareStatement("INSERT INTO downloads (url, path, filename, filekey, filesize, filepass, filenoexpire) VALUES (?,?,?,?,?,?,?)");
                
                ps.setString(1, url);
                ps.setString(2, path);
                ps.setString(3, filename);
                ps.setString(4, filekey);
                ps.setLong(5, size);
                ps.setString(6, filepass);
                ps.setString(7, filenoexpire);
                
                ps.executeUpdate();
                
                ps.close();
                
                conn.close();
            }
        }catch(ClassNotFoundException ex){
             ex.printStackTrace();
        }
    }
    
    protected synchronized static void unRegisterDownload(String url) {
        
        String nombreDB= "mcdownloader.db";
        
        try {
            
            Class.forName("org.sqlite.JDBC");
           
            try (Connection conn = DriverManager.getConnection("jdbc:sqlite:"+nombreDB)
            ) {
                
                PreparedStatement ps = conn.prepareStatement("DELETE FROM downloads WHERE url=?");
                
                ps.setString(1, url);
                
                ps.executeUpdate();
                
                ps.close();
                
                conn.close();
            }
        }catch(ClassNotFoundException | SQLException ex){
             ex.printStackTrace();
        }
    }
    
    protected synchronized static String getValueFromDB(String key)  {
        
        String nombreDB= "mcdownloader.db";
        
        String value=null;
        
        try {
            
            Class.forName("org.sqlite.JDBC");
           
            try (Connection conn = DriverManager.getConnection("jdbc:sqlite:"+nombreDB)
            ) {

                PreparedStatement ps = conn.prepareStatement("SELECT value from settings WHERE key=?");
                
                ps.setString(1, key);
                
                ResultSet res = ps.executeQuery();

                if(res.next()) {
                    value = res.getString(1);
                }
                
                ps.close();
                conn.close();
            }
        }catch(ClassNotFoundException | SQLException ex){
             ex.printStackTrace();
        }
        
        return value;
    }
    
    protected synchronized static void setValueInDB(String key, String value)  {
        
        String nombreDB= "mcdownloader.db";
        
        try {
            
            Class.forName("org.sqlite.JDBC");
           
            try (Connection conn = DriverManager.getConnection("jdbc:sqlite:"+nombreDB)
            ) {
                PreparedStatement ps = conn.prepareStatement("INSERT OR REPLACE INTO settings (key,value) VALUES (?, ?)");
                
                ps.setString(1, key);
                
                ps.setString(2, value);
                
                ps.executeUpdate();
                
                ps.close();
                
                conn.close();
            }
        }catch(ClassNotFoundException | SQLException ex){
             ex.printStackTrace();
        }
    }
    
    public final boolean trayIcon() throws AWTException {
        
        Font font = null;
        
        try {
            font = Font.createFont(Font.TRUETYPE_FONT, this.getClass().getResourceAsStream("Gochi.ttf"));
        } catch (FontFormatException | IOException ex) {
            Logger.getLogger(DownloaderBox.class.getName()).log(Level.SEVERE, null, ex);
        }
       
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        ge.registerFont(font);
        
        
        
        
        if (!SystemTray.isSupported()) {
            return false;
        }

    SystemTray tray = SystemTray.getSystemTray();
    Toolkit toolkit = Toolkit.getDefaultToolkit();
    Image image = toolkit.getImage(this.getClass().getResource("pica_roja.png"));

    PopupMenu menu = new PopupMenu();

    javax.swing.JFrame myframe = this;

    MenuItem messageItem = new MenuItem("Restore");
    
    messageItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        myframe.setExtendedState(javax.swing.JFrame.NORMAL);
        myframe.setVisible(true);

      }
    });
    menu.add(messageItem);

    MenuItem closeItem = new MenuItem("EXIT");
    closeItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        System.exit(0);
      }
    });
    
    menu.add(closeItem);
    
    ActionListener actionListener = new ActionListener() {
    @Override
    public void actionPerformed( ActionEvent e ) {
      //Double click code here
        if(!myframe.isVisible())
        {
            myframe.setExtendedState(javax.swing.JFrame.NORMAL);
            myframe.setVisible(true);
        } 
        else
        {
            myframe.dispatchEvent(new WindowEvent(myframe, WindowEvent.WINDOW_CLOSING));
        }
        
    }
};
    
    TrayIcon icon = new TrayIcon(image, "MCDownloader", menu);
    icon.setImageAutoSize(true);
    
    icon.addActionListener(actionListener);

    tray.add(icon);
    
    return true;
  }
    

    // Variables declaration - do not modify//GEN-BEGIN:variables
    protected javax.swing.JButton close_all_finished;
    protected javax.swing.JLabel global_speed;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenu jMenu2;
    private javax.swing.JMenu jMenu4;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JMenuItem jMenuItem1;
    private javax.swing.JMenuItem jMenuItem2;
    private javax.swing.JMenuItem jMenuItem3;
    private javax.swing.JMenuItem jMenuItem4;
    private javax.swing.JMenuItem jMenuItem5;
    protected javax.swing.JPanel jPanel2;
    protected javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JPopupMenu.Separator jSeparator1;
    private javax.swing.JPopupMenu.Separator jSeparator2;
    private javax.swing.JPopupMenu.Separator jSeparator3;
    protected javax.swing.JLabel logo;
    protected javax.swing.JMenuItem menu_clean_all;
    protected javax.swing.JButton pause_all;
    protected javax.swing.JLabel proxy_status;
    protected javax.swing.JLabel status;
    // End of variables declaration//GEN-END:variables
}
