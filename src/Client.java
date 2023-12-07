import javax.crypto.spec.IvParameterSpec;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.SecureRandom;
import java.util.Scanner;

public class Client implements Runnable {
    private boolean closeConnection;
    private Socket skt;
    private ServerSocket sskt;
    private String host;
    private int port;

    private static final SecureRandom RAND = new SecureRandom();
    private BigInteger p;
    private BigInteger q;
    private BigInteger n;
    private BigInteger theirN;
    private BigInteger e;
    private BigInteger theirE;
    private BigInteger d;
    private AES aes;

    private Buffer buffer;
    private ObjectInputStream inFromServer;
    private ObjectOutputStream outToServer;
    private Message messageToSend;
    private Message messageToReceive;

    private IncomingListener listener;

    private String name;
    private String myAESKey;
    private boolean rsaReady = false;
    private boolean aesReady = true;

    public Client(String name, int bitLength, String myAESKey, String host, int port, Buffer buffer){
        this.myAESKey = myAESKey;
        aes = new AES(myAESKey);

        closeConnection = false;
        this.host = host;
        this.port = port;

        inFromServer = null;
        outToServer = null;
        messageToSend = null;
        messageToReceive = null;

        this.buffer = buffer;
        listener = null;
        skt = null;
        sskt = null;

        this.name = name;

        generateRSAKeys(bitLength);
    }

    private void generateRSAKeys(int bitLength) {
        System.out.println("Generating RSA keys...");
        p = BigInteger.probablePrime(bitLength, RAND);
        q = BigInteger.probablePrime(bitLength, RAND);

        n = p.multiply(q);

        BigInteger phi = p.subtract(BigInteger.ONE).multiply(q.subtract(BigInteger.ONE));
        e = BigInteger.probablePrime(bitLength / 2, RAND);
        while (phi.gcd(e).compareTo(BigInteger.ONE) > 0 && e.compareTo(phi) < 0) {
            e.add(BigInteger.ONE);
        }
        d = e.modInverse(phi);

        System.out.println("p: " + p);
        System.out.println("q: " + q);
        System.out.println("n: " + n);
        System.out.println("e: " + e);
        System.out.println("d: " + d);
    }

    @Override
    public void run() {
        Scanner inFromStd = new Scanner(System.in);
        try {
            if(name.equals("alice")) {
                sskt = new ServerSocket(port);
                System.out.println("Waiting for connections...");
                skt = sskt.accept();
            } else if (name.equals("bob")) {
                skt = new Socket(host, port);
            }

            outToServer = new ObjectOutputStream(skt.getOutputStream());
            inFromServer = new ObjectInputStream(skt.getInputStream());

            Thread l = new Thread(new IncomingListener(this, buffer));
            l.start();

            sendRSAInfo();

            while (!buffer.isCloseConnection()) {
                messageToSend = buffer.readOutgoingMessage();
//                System.out.println("sending : " + messageToSend);
                sendData();
            }
            System.out.println("Closing connections...");
            inFromStd.close();
            skt.close();
            if(name.equals("alice")) {
                sskt.close();
            }
            outToServer.close();
            inFromServer.close();
        } catch (IOException ioe) {
            System.err.println(ioe.getMessage());
        }
    }

    public void sendRSAInfo() {
        System.out.println("Sending RSA keys...");
        try {
            Message keys = new Message(Message.RSA_KEYS, n + " " + e, null);
            outToServer.writeObject(keys);
            outToServer.flush();
            System.out.println("Keys flushed...");
        } catch (IOException ioe) {
            System.err.println(ioe.getMessage());
        }
    }

    public void sendAESInfo() {
        System.out.println("Sending AES key...");
        try {
            Message keys = new Message(Message.AES_KEY, rsaEncrypt(myAESKey), null);
            outToServer.writeObject(keys);
            outToServer.flush();
            System.out.println("Keys flushed...");
        } catch (IOException ioe) {
            System.err.println(ioe.getMessage());
        }
    }

    public void receiveRSAInfo() {
        try {
            String data = ((Message) inFromServer.readObject()).getData();
            theirN = new BigInteger(data.substring(0, data.indexOf(' ')));
            theirE = new BigInteger(data.substring(data.indexOf(' ') + 1));
            rsaReady = true;
        } catch (IOException | ClassNotFoundException e) {
            System.err.println(e.getMessage());
        }
    }

    public void receiveAESInfo() {
        try {
            String key = ((Message) inFromServer.readObject()).getData();
            String encKey = rsaDecrypt(key);
            aes.setEncKey(encKey);
            System.out.println("AES key: " + encKey);
            System.out.println("All ready!");
            aesReady = true;
        } catch (IOException | ClassNotFoundException e) {
            System.err.println(e.getMessage());
        }
    }

    public void sendData() {
//        System.out.println("Sending data...");
        try {
            if(messageToSend != null) {
                byte[] iv = aes.generateIv();
                IvParameterSpec ivs = new IvParameterSpec(iv);
                messageToSend = new Message(Message.MESSAGE, aes.encrypt(messageToSend.getData(), ivs), iv);
            }
            outToServer.writeObject(messageToSend);
            outToServer.flush();
//            System.out.println("Data flushed...");
        } catch (IOException ioe) {
            System.err.println(ioe.getMessage());
        }
    }

    public void receiveData() {
        try {
            messageToReceive = (Message) inFromServer.readObject();
                if(messageToReceive == null) {
                    buffer.setCloseConnection(true);
                    closeConnection = true;
                    System.out.println("Connection closed! Press enter to exit.");
                } else {
//                    System.out.println("Receiving message...");
                    byte[] iv = messageToReceive.getIv();
                    IvParameterSpec ivs = new IvParameterSpec(iv);
                    Message decMessageToReceive = new Message(Message.MESSAGE, aes.decrypt(messageToReceive.getData(), ivs), iv);
                    buffer.getMessageList().add(decMessageToReceive);
                    if(name.equals("alice")) {
                        System.out.println("bob: " + decMessageToReceive.getData());
                    } else {
                        System.out.println("alice: " + decMessageToReceive.getData());
                    }
                }
        } catch (IOException | ClassNotFoundException e) {
            System.err.println(e.getMessage());
        }
    }

    private String rsaEncrypt(String plaintext) {
        if(theirE != null && theirN != null) {
            String ciphertext = "";
            for (char c : plaintext.toCharArray()) {
                BigInteger m = new BigInteger(String.valueOf((int) c));
                BigInteger ciph = m.modPow(theirE, theirN);
                ciphertext += ciph + " ";
            }
            return ciphertext;
        }
        return null;
    }

    private String rsaDecrypt(String ciphertext) {
        if(d != null && n != null) {
            String plaintext = "";
            String[] ciphertextArr = ciphertext.split(" ");
            for (String c : ciphertextArr) {
                BigInteger decrypted = new BigInteger(c).modPow(d, n);
                plaintext += (char) decrypted.intValue();
            }
            return plaintext;
        }
        return null;
    }

    public void printData() {
        System.out.println(messageToReceive);
    }

    public boolean isCloseConnection() {
        return closeConnection;
    }
    public void setCloseConnection(boolean closeConnection) {
        this.closeConnection = closeConnection;
    }

    public boolean isRSAReady() {
        return rsaReady;
    }

    public boolean isAESReady() {
        return aesReady;
    }
}
