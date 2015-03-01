
package mcdownloader;

import java.util.HashMap;

public class ContentType {
    
    private HashMap<String,String> content_type;

    public HashMap<String, String> getContent_type() {
        return content_type;
    }

    public ContentType() {
        this.content_type = new HashMap();
            
        this.content_type.put("mp2", "audio/x-mpeg");
        this.content_type.put("mp3", "audio/x-mpeg");
        this.content_type.put("mpga", "audio/x-mpeg");
        this.content_type.put("mpega", "audio/x-mpeg");
        this.content_type.put("mpg", "video/x-mpeg-system");
        this.content_type.put("mpeg", "video/x-mpeg-system");
        this.content_type.put("mpe", "video/x-mpeg-system");
        this.content_type.put("vob", "video/x-mpeg-system");
        this.content_type.put("aac", "audio/mp4");
        this.content_type.put("mp4", "video/mp4");
        this.content_type.put("mpg4", "video/mp4");
        this.content_type.put("m4v", "video/x-m4v");
        this.content_type.put("avi", "video/x-msvideo");
        this.content_type.put("ogg", "application/ogg");
        this.content_type.put("ogv", "video/ogg");
        this.content_type.put("asf", "video/x-ms-asf-plugin");          
        this.content_type.put("asx", "video/x-ms-asf-plugin");
        this.content_type.put("ogv", "video/ogg");
        this.content_type.put("wmv", "video/x-ms-wmv");
        this.content_type.put("wmx", "video/x-ms-wvx");
        this.content_type.put("wma", "audio/x-ms-wma");
        this.content_type.put("wav", "audio/wav");
        this.content_type.put("3gp", "audio/3gpp");
        this.content_type.put("3gp2", "audio/3gpp2");
        this.content_type.put("divx", "video/divx");
        this.content_type.put("flv", "video/flv");
        this.content_type.put("mkv", "video/x-matroska");
        this.content_type.put("mka", "audio/x-matroska");
        this.content_type.put("m3u", "audio/x-mpegurl");
        this.content_type.put("webm", "video/webm");
        this.content_type.put("rm", "application/vnd.rn-realmedia");
        this.content_type.put("ra", "audio/x-realaudio");
        this.content_type.put("amr", "audio/amr");
        this.content_type.put("flac", "audio/x-flac");
        this.content_type.put("mov", "video/quicktime");
        this.content_type.put("qt", "video/quicktime");
    }
    
    public String getMIME(String ext)
    {
        return this.content_type.get(ext);
    }
}
