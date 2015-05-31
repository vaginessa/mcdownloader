
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
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import javax.swing.JOptionPane;



public class MegaCrypterAPI {
    
    public static String getMegaFileDownloadUrl(String link, String pass_hash, String noexpire_token) throws IOException, MegaCrypterAPIException
    {
        String request;
        
        if(noexpire_token != null)
        {
            request = "{\"m\":\"dl\", \"link\": \""+link+"\", \"noexpire\": \""+noexpire_token+"\"}";
            
        } else {
            
            request = "{\"m\":\"dl\", \"link\": \""+link+"\"}";
        }
        
        URL url_api = new URL(MiscTools.findFirstRegex("https?://[^/]+", link, 0)+"/api");
        HttpURLConnection conn = (HttpURLConnection) url_api.openConnection();
        conn.setConnectTimeout(Downloader.CONNECT_TIMEOUT);
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("User-Agent", McDownloaderMain.USER_AGENT);
        
        OutputStream out;
        out = conn.getOutputStream();
	out.write(request.getBytes());
        out.close();
        
        if (conn.getResponseCode() != HttpURLConnection.HTTP_OK)
        {    
            throw new RuntimeException("Failed : HTTP error code : " + conn.getResponseCode());
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
        
        String dl_url = MiscTools.findFirstRegex("\"url\" *: *\"([^\"]+)\"", response_guay.replaceAll("\\\\", ""), 1);
        
        if(pass_hash != null)
        {
            try {
                String pass = MiscTools.findFirstRegex("\"pass\" *: *\"([^\"]+)\"", data, 1);
                
                byte[] iv = MiscTools.BASE642Bin(pass);
                
                Cipher decrypter = CryptTools.genDecrypter("AES", "AES/CBC/PKCS5Padding", MiscTools.BASE642Bin(pass_hash),iv);
                
                byte[] decrypted_url = decrypter.doFinal(MiscTools.BASE642Bin(dl_url));
                
                dl_url = new String(decrypted_url);
                
            } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException ex) {
                Logger.getLogger(MegaCrypterAPI.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        if(!dl_url.startsWith("http"))
        {
            throw new MegaCrypterAPIException("Bad mega temp url!");
        }
       
        return dl_url;
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
        conn.setRequestProperty("User-Agent", McDownloaderMain.USER_AGENT);
        
        OutputStream out;
        out = conn.getOutputStream();
	
        out.write(request.getBytes());
        
        out.close();
        
        if (conn.getResponseCode() != HttpURLConnection.HTTP_OK)
        {    
            throw new RuntimeException("Failed : HTTP error code : " + conn.getResponseCode());
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
        
        String noexpire_token = MiscTools.findFirstRegex("\"expire\" *: *\"[^\"]+#([^\"]+)\"", data, 1);

        String pass = MiscTools.findFirstRegex("\"pass\" *: *\"([^\"]+)\"", data, 1);

        if(pass != null)
        {
            String pass_aux[] = pass.split("#");
            
            if(pass_aux.length != 4)
            {
                throw new MegaCrypterAPIException("Bad password data!");
            }

            int iterations = Integer.parseInt(pass_aux[0]);
            
            String key_check = pass_aux[1];

            String salt = pass_aux[2];
            
            byte[] iv = MiscTools.BASE642Bin(pass_aux[3]);
            
            String password;

            String crypted_key = fkey;
            
            byte[] pass_hash = null;
            
            byte[] pass_hash_hmac = null; 

            try {                
                    do
                    {   
                        password = JOptionPane.showInputDialog(panel, "Enter password:");
                        
                        if(password!=null) {
                            
                            pass_hash = CryptTools.passHashHmac("HmacSHA256", password, MiscTools.BASE642Bin(salt), (long)Math.pow(2, iterations));
                            
                            Mac HMAC = Mac.getInstance("HmacSHA256");

                            SecretKeySpec secret_key = new SecretKeySpec(iv, "HmacSHA256");

                            HMAC.init(secret_key);
                            
                            pass_hash_hmac = HMAC.doFinal(pass_hash);
                        }
 
                    }while(password!=null && !Arrays.equals(pass_hash_hmac, MiscTools.BASE642Bin(key_check)));

                    if(password==null)
                    {
                        return null;
                    }
                    else
                    {
                        Cipher decrypter = CryptTools.genDecrypter("AES", "AES/CBC/PKCS5Padding", pass_hash, iv);

                        byte[] decrypted_key = decrypter.doFinal(MiscTools.BASE642Bin(crypted_key));

                        fkey = MiscTools.Bin2UrlBASE64(decrypted_key);
                        
                        decrypter = CryptTools.genDecrypter("AES", "AES/CBC/PKCS5Padding", pass_hash, iv);
                        
                        byte[] decrypted_name = decrypter.doFinal(MiscTools.BASE642Bin(fname));

                        fname = new String(decrypted_name);
                        
                        pass=MiscTools.Bin2BASE64(pass_hash);
                        
                    }

                    } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException ex) {
                        Logger.getLogger(MegaCrypterAPI.class.getName()).log(Level.SEVERE, null, ex);
                    }
        }

        String file_data[] = {fname.replace('[','(').replace(']',')'), file_size, fkey, pass, noexpire_token};

        return file_data;
    }
    
    private static int checkMCError(String data)
    {
        String error = MiscTools.findFirstRegex("\"error\" *: *([0-9-]+)", data, 1);

        return error != null?Integer.parseInt(error):0;
    }
}
