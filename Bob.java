import java.io.*;
import java.math.BigInteger;
import java.net.Socket;
import java.security.SecureRandom;
import java.util.Scanner;

public class Bob implements Runnable {
	private static final SecureRandom RAND = new SecureRandom();

	private BigInteger p;
	private BigInteger q;
	private BigInteger n;
	private BigInteger e;
	private BigInteger d;

	private String desKey;

	Socket skt;
	private String hostName;
	private static int DEFAULT_PORT = 7000;

	private MessageBuffer buffer;
	private ObjectInputStream inFromAlice;
	private ObjectOutputStream outToAlice;
	private Message messageToSend;
	private Message messageToReceive;

	public Bob(int bitLength, String desKey, MessageBuffer buffer) {
		generateRSAKeys(bitLength);
		this.desKey = desKey;
		this.buffer = buffer;
		inFromAlice = null;
		outToAlice = null;
		messageToSend = null;
		messageToReceive = null;
		skt = null;
	}

	private void generateRSAKeys(int bitLength) {
		p = BigInteger.probablePrime(bitLength, RAND);
		q = BigInteger.probablePrime(bitLength, RAND);

		n = p.multiply(q);

		BigInteger phi = p.subtract(BigInteger.ONE).multiply(q.subtract(BigInteger.ONE));
		e = BigInteger.probablePrime(bitLength / 2, RAND);
		while (phi.gcd(e).compareTo(BigInteger.ONE) > 0 && e.compareTo(phi) < 0) {
			e.add(BigInteger.ONE);
		}
		d = e.modInverse(phi);
	}

	@Override
	public void run() {
		Scanner inFromStd = new Scanner(System.in);
		try {
			skt = new Socket(hostName, DEFAULT_PORT);

			outToAlice = new ObjectOutputStream(skt.getOutputStream());
			inFromAlice = new ObjectInputStream(skt.getInputStream());

			sendDESKey();
			while (true) {
				messageToSend = buffer.readOutgoingMessage();
				sendMessage();
			}
//			inFromStd.close();
//			skt.close();
//			outToAlice.close();
//			inFromAlice.close();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}

	public void sendDESKey() {
		try {
			outToAlice.writeObject(rsaEncrypt(desKey));
			outToAlice.flush();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}

	public void sendMessage() {
		try {
			outToAlice.writeObject(messageToSend);
			outToAlice.flush();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}

	public void receiveData() {
		try {
			messageToReceive = (Message) inFromAlice.readObject();
			buffer.getMessageList().add((String) messageToReceive.getPlainMessage(desKey));
		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	public void printMessage() {
		System.out.println(messageToReceive.getPlainMessage(desKey));
	}

	private String rsaEncrypt(String plaintext) {
		String ciphertext = "";
		for (int i = 0; i < plaintext.length(); i++) {
			BigInteger plaintextChar = new BigInteger(String.valueOf((int) plaintext.charAt(i)));
			BigInteger ciphertextChar = plaintextChar.modPow(e, n);
			ciphertext += ciphertextChar.toString() + " ";
		}
		return ciphertext;
	}
}
