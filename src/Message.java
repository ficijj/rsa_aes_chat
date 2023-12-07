import java.io.Serializable;

public class Message implements Serializable {
    private int type;
    public static final int MESSAGE = 0;
    public static final int RSA_KEYS = 1;
    public static final int AES_KEY = 2;

    private String data;

    private byte[] iv;

    public Message(int type, String data, byte[] iv) {
        this.type = type;
        this.data = data;
        this.iv = iv;
    }

    public int getType() {
        return type;
    }

    public String getData() {
        return data;
    }

    public byte[] getIv() {
        return iv;
    }

    @Override
    public String toString(){
        return "type: " + type + " message: " + data;
    }
}
