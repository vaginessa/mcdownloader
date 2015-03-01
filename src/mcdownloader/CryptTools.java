
package mcdownloader;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;


public class CryptTools {
    
    public static Cipher genDecrypter(String algo, String mode, byte[] key, byte[] iv) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException
    {
        SecretKeySpec skeySpec = new SecretKeySpec(key, algo); 
        
        Cipher decryptor = Cipher.getInstance(mode);

        IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
        
        decryptor.init(Cipher.DECRYPT_MODE, skeySpec, ivParameterSpec);
        
        return decryptor;
    }
    
    public static Cipher genCrypter(String algo, String mode, byte[] key, byte[] iv) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException
    {
        SecretKeySpec skeySpec = new SecretKeySpec(key, algo); 
        
        Cipher cryptor = Cipher.getInstance(mode);

        IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
        
        cryptor.init(Cipher.ENCRYPT_MODE, skeySpec, ivParameterSpec);
        
        return cryptor;
    }
    
    public static byte[] initMEGALinkKey(String key_string) throws IOException
    {
        int[] int_key = MiscTools.bin2i32a(MiscTools.UrlBASE642Bin(key_string));
        int[] k=new int[4];

        k[0] = int_key[0] ^ int_key[4];
        k[1] = int_key[1] ^ int_key[5];
        k[2] = int_key[2] ^ int_key[6];
        k[3] = int_key[3] ^ int_key[7];
      
        return MiscTools.i32a2bin(k);
    }     
    
    public static byte[] initMEGALinkKeyIV(String key_string) throws IOException
    {
        int[] int_key =MiscTools.bin2i32a(MiscTools.UrlBASE642Bin(key_string));
        int[] iv = new int[4];

        iv[0] = int_key[4];
        iv[1] = int_key[5];
        iv[2] = 0;
        iv[3] = 0;

        return MiscTools.i32a2bin(iv);
    }
        
    public static byte[] forwardMEGALinkKeyIV(byte[] iv, long forward_bytes)
    {
        byte[] new_iv = new byte[iv.length];
   
        System.arraycopy(iv, 0, new_iv, 0, iv.length/2);
        
        byte[] ctr = MiscTools.long2bytearray(forward_bytes/iv.length);
        
        System.arraycopy(ctr, 0, new_iv, iv.length/2, ctr.length);
        
        return new_iv;
    }    
    
    public static byte[] passHashHmac(String algo, String pass, byte[] salt, long iterations) throws NoSuchAlgorithmException, InvalidKeyException, UnsupportedEncodingException
    {
        Mac HMAC = Mac.getInstance(algo);

        SecretKeySpec secret_key = new SecretKeySpec(pass.getBytes("UTF-8"), algo);

        HMAC.init(secret_key);

        byte[] last, xor;
        
        xor=(last = HMAC.doFinal(salt));

        for(long i=1; i<iterations; i++) {

            HMAC.init(secret_key);

            last = HMAC.doFinal(last);

            for(int x=0; x<last.length; x++)
            {
                xor[x]^=last[x];
            }
        }

        return xor; 
    }
}
