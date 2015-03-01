package mcdownloader;

import java.awt.Color;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.SwingUtilities;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

public class MiscTools {
    
    public static int[] bin2i32a(byte[] bin)
    {
        ByteBuffer bin_buffer = ByteBuffer.wrap(bin);
        IntBuffer int_buffer = bin_buffer.asIntBuffer();
        
        if(int_buffer.hasArray()) {
            return int_buffer.array();
        }
        else
        {
            ArrayList<Integer> list = new ArrayList();
        
            while(int_buffer.hasRemaining()) {
                list.add(int_buffer.get());
            }

            int[] aux = new int[list.size()];

            for(int i=0; i<aux.length; i++) {
                aux[i] = list.get(i);
            }

            return aux;
        }
    }
    
    public static byte[] i32a2bin(int[] i32a)
    {
        ByteBuffer bin_buffer  = ByteBuffer.allocate(i32a.length * 4);        
        IntBuffer int_buffer = bin_buffer.asIntBuffer();
        int_buffer.put(i32a);
        
        if(bin_buffer.hasArray()) {
            return bin_buffer.array();
        }
        else
        {
            ArrayList<Byte> list = new ArrayList();
        
            while(int_buffer.hasRemaining()) {
                list.add(bin_buffer.get());
            }

            byte[] aux = new byte[list.size()];

            for(int i=0; i<aux.length; i++) {
                aux[i] = list.get(i);
            }

            return aux;
        }
    }
    
    public static byte[] long2bytearray(long val) {
    
        byte [] b = new byte[8];
        
        for (int i = 7; i >= 0; i--) {
          b[i] = (byte) val;
          val >>>= 8;
        }
        
        return b;
    }
    
    public static long bytearray2long(byte[] val) {
        
        long l=0;
        
        for (int i = 0; i <=7; i++) {
            l+=val[i];
            l<<=8;
        }
        
        return l;
    }
    
    public static String findFirstRegex(String regex, String data, int group)
    {
        Pattern pattern = Pattern.compile(regex);
        
        Matcher matcher = pattern.matcher(data);
        
        return matcher.find()?matcher.group(group):null;   
    }
    
    public static ArrayList<String> findAllRegex(String regex, String data, int group)
    {
        Pattern pattern = Pattern.compile(regex);
        
        Matcher matcher = pattern.matcher(data);
        
        ArrayList<String> matches = new ArrayList<>();
        
        while(matcher.find()) {
            matches.add(matcher.group(group));
        }
        
        return matches;
    }
    
    public static void updateFont(javax.swing.JComponent label, Font font, int layout)
    {
        label.setFont(font.deriveFont(layout, label.getFont().getSize()));
    }
    
    
    public static String HashString(String algo, String data) throws NoSuchAlgorithmException, UnsupportedEncodingException
    {
        MessageDigest md = MessageDigest.getInstance(algo);
        
        byte[] thedigest = md.digest(data.getBytes("UTF-8"));
        
        return bin2hex(thedigest); 
    }
    
    public static String HashString(String algo, byte[] data) throws NoSuchAlgorithmException
    {
        MessageDigest md = MessageDigest.getInstance(algo);
        
        byte[] thedigest = md.digest(data);
        
        return bin2hex(thedigest); 
    }
    
    public static byte[] HashBin(String algo, String data) throws NoSuchAlgorithmException, UnsupportedEncodingException
    {
        MessageDigest md = MessageDigest.getInstance(algo);
        
        return md.digest(data.getBytes("UTF-8"));
    }
    
    public static byte[] HashBin(String algo, byte[] data) throws NoSuchAlgorithmException
    {
        MessageDigest md = MessageDigest.getInstance(algo);
        
        return md.digest(data);
    }
    
    public static byte[] BASE642Bin(String data) throws IOException
    {
        BASE64Decoder decoder = new BASE64Decoder();
       
        int count_padding = (4 - data.length()%4)%4;
        
        for(int i=0; i<count_padding; i++)
            data+='=';
        
        return decoder.decodeBuffer(data);
    }
    
    public static String Bin2BASE64(byte[] data) throws IOException
    {
        BASE64Encoder encoder = new BASE64Encoder();
        return encoder.encode(data);
    }
    
    public static byte[] UrlBASE642Bin(String data) throws IOException
    {
        return MiscTools.BASE642Bin(data.replace('_', '/').replace('-', '+').replace(",", ""));
    }
    
    public static String Bin2UrlBASE64(byte[] data) throws IOException
    {
        return MiscTools.Bin2BASE64(data).replace('/', '_').replace('+', '-');
    }
    
    public static long getWaitTimeExpBackOff(int retryCount, int pow_base, int secs_by_retry, long max_time) {

        long waitTime = ((long) Math.pow(pow_base, retryCount) * secs_by_retry);

        return Math.min(waitTime, max_time);
    }
    
    
    /* Por normal general NO ESPERAR NUNCA (se pueden dan interbloqueos chungos de detectar) */
    private static void swingInvokeIt(Runnable r, boolean wait) {
        
        if(SwingUtilities.isEventDispatchThread())
        {
            r.run();

        } else {
            
            if(wait) {
            
                try {
                SwingUtilities.invokeAndWait(r);
                } catch (InterruptedException | InvocationTargetException ex) {
                    Logger.getLogger(MiscTools.class.getName()).log(Level.SEVERE, null, ex);
                }
            }else{
                SwingUtilities.invokeLater(r);
                }
        }
    }
    
    private static Object swingInvokeItAndReturn(Callable c)
    {
        Object ret=null;
        
        if(SwingUtilities.isEventDispatchThread())
        {
            try {
                ret =  c.call();
            } catch (Exception ex) {
                Logger.getLogger(MiscTools.class.getName()).log(Level.SEVERE, null, ex);
            }
           
        } else {
        
            FutureTask<Object> futureTask = new FutureTask<>(c);
        
            SwingUtilities.invokeLater(futureTask);

            try {
                ret = futureTask.get();
            } catch (InterruptedException | ExecutionException ex) {
                Logger.getLogger(MiscTools.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        return ret;
    }
    
    public static void swingSetText(final javax.swing.JComponent component, final String text, final boolean wait) {
        
        Runnable r = new Runnable(){
            
            @Override
            public void run() {
                
                Class c = component.getClass();
        
                if(c == javax.swing.JLabel.class) {

                    ((javax.swing.JLabel)component).setText(text);

                } else if(c == javax.swing.JButton.class) {

                    ((javax.swing.JButton)component).setText(text);
                }
            }
        };
        
        swingInvokeIt(r, wait);
    }
    
    public static void swingSetViewportView(final javax.swing.JScrollPane component, final javax.swing.JComponent component2, final boolean wait) {
        
        Runnable r = new Runnable(){
            
            @Override
            public void run() {
                
                component.setViewportView(component2);
            }
        };
        
        swingInvokeIt(r, wait);
    }
    
    
    public static String swingGetText(final javax.swing.JComponent component) {
        
        Callable c = new Callable<Object>(){
            
            @Override
            public Object call() {
                
                Class c = component.getClass();
                
                Object o=null;
                    
                if(c == javax.swing.JLabel.class) {

                    o = ((javax.swing.JLabel)component).getText();
                    
                } else if(c == javax.swing.JButton.class) {
                    
                    o = ((javax.swing.JButton)component).getText();
                }
                
                return o;
            }
        };
        
        return (String)swingInvokeItAndReturn(c);
    }
    
    public static javax.swing.JComponent swingGetEditor(final javax.swing.JComponent component) {
        
        Callable c = new Callable<Object>(){
            
            @Override
            public Object call() {
                
                Class c = component.getClass();
                
                Object o=null;
                    
                if(c == javax.swing.JSpinner.class) {

                    o = ((javax.swing.JSpinner)component).getEditor();
                } 
                
                return o;
            }
        };
        
        return (javax.swing.JComponent)swingInvokeItAndReturn(c);
    }
    
    public static void swingSetSelectedFile(final javax.swing.JComponent component, final File file, final boolean wait) {
        
        Runnable r = new Runnable(){
            
            @Override
            public void run() {
                
                Class c = component.getClass();
        
                if(c == javax.swing.JFileChooser.class) {

                    ((javax.swing.JFileChooser)component).setSelectedFile(file);

                }
            }
        };
        
        swingInvokeIt(r, wait);
    }
    
    public static void swingSetValue(final javax.swing.JComponent component, final Object value, final boolean wait) {
        
        Runnable r = new Runnable(){
            
            @Override
            public void run() {
                
                Class c = component.getClass();
        
                if(c == javax.swing.JSpinner.class) {

                    ((javax.swing.JSpinner)component).setValue(value);
                    
                } else if (c == javax.swing.JProgressBar.class) {
                    
                    ((javax.swing.JProgressBar)component).setValue((int)value);
                }
            }
        };
        
        swingInvokeIt(r, wait);
    }
    
    public static File swingGetSelectedFile(final javax.swing.JComponent component) {
        
        Callable c = new Callable<Object>(){
            
            @Override
            public Object call() {
                
                Class c = component.getClass();
                
                Object o=null;
                    
                if(c == javax.swing.JFileChooser.class) {

                    o = ((javax.swing.JFileChooser)component).getSelectedFile();
                } 
                
                return o;
            }
        };
        
        return (File)swingInvokeItAndReturn(c);
    }
    
    public static int swingShowSaveDialog(final javax.swing.JComponent component, final javax.swing.JPanel panel) {
        
        Callable c = new Callable<Object>(){
            
            @Override
            public Object call() {
                
                Class c = component.getClass();
                
                Object o=null;
                    
                if(c == javax.swing.JFileChooser.class) {

                    o = ((javax.swing.JFileChooser)component).showSaveDialog(panel);
                }
                
                return o;
            }
        };
        
        return (int)swingInvokeItAndReturn(c);
    }
    
    
    public static Object swingGetValue(final javax.swing.JComponent component) {
        
        Callable c = new Callable<Object>(){
            
            @Override
            public Object call() {
                
                Class c = component.getClass();
                
                Object o=null;
                    
                if(c == javax.swing.JSpinner.class) {

                    o = ((javax.swing.JSpinner)component).getValue();
                    
                } else if (c == javax.swing.JProgressBar.class) {
                    
                    o = ((javax.swing.JProgressBar)component).getValue();
                }
                
                return o;
            }
        };
        
        return swingInvokeItAndReturn(c);
    }
    
    public static void swingSetForeground(final javax.swing.JComponent component, final Color color, final boolean wait) {
        
        Runnable r = new Runnable(){
            
            @Override
            public void run() {
                
                Class c = component.getClass();
        
                if(c == javax.swing.JLabel.class) {

                    ((javax.swing.JLabel)component).setForeground(color);

                } else if(c == javax.swing.JButton.class) {

                    ((javax.swing.JButton)component).setForeground(color);
                    
                } else if(c == javax.swing.JFormattedTextField.class) {

                    ((javax.swing.JFormattedTextField)component).setForeground(color);
                }
            }
        };
        
        swingInvokeIt(r, wait);
    }
 
    public static void swingSetMaximum(final javax.swing.JComponent component, final Object max, final boolean wait) {
        
        Runnable r = new Runnable(){
            
            @Override
            public void run() {
                
                Class c = component.getClass();
        
                if(c == javax.swing.JProgressBar.class) {

                    ((javax.swing.JProgressBar)component).setMaximum((int)max);
                }
            }
        };
        
        swingInvokeIt(r, wait);
    }
    
    public static void swingSetMinimum(final javax.swing.JComponent component, final Object min, final boolean wait) {
        
        Runnable r = new Runnable(){
            
            @Override
            public void run() {
                
                Class c = component.getClass();
        
                if(c == javax.swing.JProgressBar.class) {

                    ((javax.swing.JProgressBar)component).setMinimum((int)min);
                }
            }
        };
        
        swingInvokeIt(r, wait);
    }
    
    public static void swingSetStringPainted(final javax.swing.JComponent component, final boolean paint, final boolean wait) {
        
        Runnable r = new Runnable(){
            
            @Override
            public void run() {
                
                Class c = component.getClass();
        
                if(c == javax.swing.JProgressBar.class) {

                    ((javax.swing.JProgressBar)component).setStringPainted(paint);
                }
            }
        };
        
        swingInvokeIt(r, wait);
    }
    
    public static void swingSetVisible(final javax.swing.JComponent component, final boolean visible, final boolean wait) {
        
        Runnable r = new Runnable(){
            
            @Override
            public void run() {
                
                component.setVisible(visible);
            }
        };
        
        swingInvokeIt(r, wait);
    }
    
    public static void swingSetEnabled(final javax.swing.JComponent component, final boolean enabled, final boolean wait) {
        
        Runnable r = new Runnable(){
            
            @Override
            public void run() {
                
                component.setEnabled(enabled);
            }
        };
        
        swingInvokeIt(r, wait);
    }
    
    public static boolean swingIsSelected(final javax.swing.JCheckBox checkbox) {
        
        Callable c = new Callable<Object>(){
            
            @Override
            public Object call() {
                
                return checkbox.isSelected();
            }
        };
        
        return (boolean)swingInvokeItAndReturn(c);
    }
    
    public static String bin2hex(byte[] b){
        
        BigInteger bi = new BigInteger(1, b);

        return String.format("%0" + (b.length << 1) + "x", bi);
    }
    
    public static void copyTextToClipboard(String text) {

        StringSelection stringSelection = new StringSelection (text);
        Clipboard clpbrd = Toolkit.getDefaultToolkit ().getSystemClipboard ();
        clpbrd.setContents (stringSelection, null);
        
    }
    
}
