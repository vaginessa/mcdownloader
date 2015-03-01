
package mcdownloader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.swing.JOptionPane;



public class MegaCrypterAPI {
    
    public static String getMegaFileDownloadUrl(String link) throws IOException, MegaCrypterAPIException
    {
        String request = "{\"m\":\"dl\", \"link\": \""+link+"\"}";
        
        URL url_api = new URL(MiscTools.findFirstRegex("https?://[^/]+", link, 0)+"/api");
        HttpURLConnection conn = (HttpURLConnection) url_api.openConnection();
        conn.setConnectTimeout(Downloader.CONNECT_TIMEOUT);
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("User-agent", "Mozilla/5.0 (X11; Linux x86_64; rv:35.0) Gecko/20100101 Firefox/35.0");
        
        OutputStream out;
        
        out = conn.getOutputStream();
	out.write(request.getBytes());
        out.close();
        
        if (conn.getResponseCode() != HttpURLConnection.HTTP_OK)
        {    
            throw new IOException(link+"Failed : HTTP error code : " + conn.getResponseCode());
        }
	
        BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
  
        String response, response_guay="";

        while ((response = br.readLine()) != null)
        {
                response_guay+=response;
        }

        br.close();
        
        conn.disconnect();
        
        String data = response_guay.replaceAll("\\\\", "");
        
        int mc_error;
        
        if((mc_error=MegaCrypterAPI.checkMCError(data))!=0)
        {
            throw new MegaCrypterAPIException(String.valueOf(mc_error));
        }
       
        return MiscTools.findFirstRegex("\"url\" *: *\"([^\"]+)\"", response_guay.replaceAll("\\\\", ""), 1);
    }
    
    public static String[] getMegaFileMetadata(String link, McDownloaderMain panel) throws IOException, MegaCrypterAPIException
    {
        String request = "{\"m\":\"info\", \"link\": \""+link+"\"}";

        URL url_api = new URL(MiscTools.findFirstRegex("https?://[^/]+", link, 0)+"/api");
        HttpURLConnection conn = (HttpURLConnection) url_api.openConnection();
        conn.setConnectTimeout(Downloader.CONNECT_TIMEOUT);
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("User-agent", "Mozilla/5.0 (X11; Linux x86_64; rv:35.0) Gecko/20100101 Firefox/35.0");
        
        OutputStream out;
        out = conn.getOutputStream();
	
        out.write(request.getBytes());
        
        out.close();
        
        if (conn.getResponseCode() != HttpURLConnection.HTTP_OK)
        {    
            throw new IOException(" Failed : HTTP error code : " + conn.getResponseCode());
        }
	
        BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
  
        String response, response_guay="";

        while ((response = br.readLine()) != null)
        {
                response_guay+=response;
        }
        
        br.close();
        
        conn.disconnect();
        
        String data = response_guay.replaceAll("\\\\", "");
        
        int mc_error;
        
        if((mc_error=MegaCrypterAPI.checkMCError(data))!=0)
        {
            throw new MegaCrypterAPIException(String.valueOf(mc_error));
        }
      
        String fname = MiscTools.findFirstRegex("\"name\" *: *\"([^\"]+)\"", data, 1);

        String file_size = MiscTools.findFirstRegex("\"size\" *: *([0-9]+)", data, 1);

        String fkey = MiscTools.findFirstRegex("\"key\" *: *\"([^\"]+)\"", data, 1);

        String pass = MiscTools.findFirstRegex("\"pass\" *: *(false|\"[^\"]+\")", data, 1);

        if(!pass.equals("false"))
        {
            String pass_aux[] = pass.replaceAll("\"", "").split("#");

            int iterations = Integer.parseInt(pass_aux[0]);
            
            String key_check = pass_aux[1];

            String salt = pass_aux[2];

            String password;

            String crypted_key = fkey;
            
            byte[] pass_hash = null;

            try {    
                    Iterator<String> it = panel.passwords.iterator();
                    
                    do
                    {                
                        if(it.hasNext())
                        {
                            password = it.next();
                        }
                        else
                        {
                            password = JOptionPane.showInputDialog(panel, "Enter password:");
                        }
                        
                        if(password!=null) {
                            
                            pass_hash = CryptTools.passHashHmac("HmacSHA256", password, MiscTools.BASE642Bin(salt), (long)Math.pow(2, iterations));
                        }
 
                    }while(password!=null && !Arrays.equals(MiscTools.HashBin("SHA-256", pass_hash), MiscTools.BASE642Bin(key_check)));


                    if(password==null)
                    {
                        return null;
                    }
                    else
                    {
                        if(!panel.passwords.contains(password))
                        {
                            panel.passwords.add(password);
                        }
                        
                        int[] iv = {0,0,0,0};

                        Cipher decrypter = CryptTools.genDecrypter("AES", "AES/CBC/PKCS5Padding", pass_hash, MiscTools.i32a2bin(iv));

                        byte[] decrypted_key = decrypter.doFinal(MiscTools.BASE642Bin(crypted_key));

                        fkey = MiscTools.Bin2UrlBASE64(decrypted_key);

                        decrypter = CryptTools.genDecrypter("AES", "AES/CBC/PKCS5Padding", pass_hash, MiscTools.i32a2bin(iv));

                        byte[] decrypted_name = decrypter.doFinal(MiscTools.BASE642Bin(fname));

                        fname = new String(decrypted_name);
                    }

                    } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException ex) {
                        Logger.getLogger(MegaCrypterAPI.class.getName()).log(Level.SEVERE, null, ex);
                    }
        }

        String file_data[] = {fname.replace('[','(').replace(']',')'), file_size, fkey};

        return file_data;
    }
    
    private static int checkMCError(String data)
    {
        String error = MiscTools.findFirstRegex("\"error\" *: *([0-9-]+)", data, 1);

        return error != null?Integer.parseInt(error):0;
    }
}
