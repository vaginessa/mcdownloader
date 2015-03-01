package mcdownloader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

/**
 * TODO: add methods to get metadata info without require MegaCrypter API
 */
public class MegaAPI {
 
    public static final String API_URL = "https://g.api.mega.co.nz";
    
    public static int seqno;
    
    public static String api_key=null;
    
    public MegaAPI()
    {
        Random randomno = new Random();
            
        seqno=randomno.nextInt();
    }
    
    public MegaAPI(String ak)
    {
        Random randomno = new Random();
            
        seqno=randomno.nextInt();
        
        api_key=ak;
    }
     
    public String getMegaFileDownloadUrl(String link) throws IOException, MegaAPIException
    {
        seqno++;

        String file_id = MiscTools.findFirstRegex("#!([^!]+)", link, 1);
        
        String request = "[{\"a\":\"g\", \"g\":\"1\", \"p\":\""+file_id+"\"}]";
        
        URL url_api = new URL(API_URL+"/cs?id="+seqno+(api_key!=null?"&ak="+api_key:""));
        HttpURLConnection conn = (HttpURLConnection) url_api.openConnection();
        conn.setConnectTimeout(ProxyStreamServer.CONNECT_TIMEOUT);
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        
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
        
        int mega_error;
        
        if((mega_error=checkMEGAError(data))!=0)
        {
            throw new MegaAPIException(String.valueOf(mega_error));
        }
        
        return MiscTools.findFirstRegex("\"g\" *: *\"([^\"]+)\"", response_guay.replaceAll("\\\\", ""), 1);
    }
    
    
    public String[] getMegaFileMetadata(String link) throws IOException, MegaAPIException
    {
        
        seqno++;

        String file_id = MiscTools.findFirstRegex("#!([^!]+)", link, 1);
        
        String file_key = MiscTools.findFirstRegex("#![^!]+!([^!]+)", link, 1);
        
        String request = "[{\"a\":\"g\", \"p\":\""+file_id+"\"}]";
        
        URL url_api = new URL(API_URL+"/cs?id="+seqno+(api_key!=null?"&ak="+api_key:""));
        HttpURLConnection conn = (HttpURLConnection) url_api.openConnection();
        conn.setConnectTimeout(ProxyStreamServer.CONNECT_TIMEOUT);
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        
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
        
        int mega_error;
        
        if((mega_error=checkMEGAError(data))!=0)
        {
            throw new MegaAPIException(String.valueOf(mega_error));
        }
        
        String fsize = MiscTools.findFirstRegex("\"s\" *: *([0-9]+)", response_guay.replaceAll("\\\\", ""), 1);
        
        String at = MiscTools.findFirstRegex("\"at\" *: *\"([^\"]+)\"", response_guay, 1);
        
        int[] iv = {0,0,0,0};

        Cipher decrypter;
        
        String[] file_data=null;
        
        try {
            
            decrypter = CryptTools.genDecrypter("AES", "AES/CBC/NoPadding", CryptTools.initMEGALinkKey(file_key), MiscTools.i32a2bin(iv));

            byte[] decrypted_at = decrypter.doFinal(MiscTools.UrlBASE642Bin(at));

            String fname_aux = new String(decrypted_at, "UTF-8");

            String fname = MiscTools.findFirstRegex("\"n\" *: *\"([^\"]+)\"", fname_aux, 1);
            
            file_data = new String[]{fname.replace('[','(').replace(']',')'), fsize, file_key};
            
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException ex) {
            Logger.getLogger(MegaAPI.class.getName()).log(Level.SEVERE, null, ex);
        }

        return file_data;
    }
    
    
    private int checkMEGAError(String data)
    {
        String error = MiscTools.findFirstRegex("\\[(\\-[0-9]+)\\]", data, 1);

        return error != null?Integer.parseInt(error):0;
    }
}
