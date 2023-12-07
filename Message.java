import java.io.Serializable;

public class Message implements Serializable {
	private String message;

	public Message(String username, String message, String key) {
		this.message = encrypt(message, key);
	}

	public Message(String username) {
		message = "";
	}

	public Message() {
		message = "";
	}

	public String getCipherMessage() {
		return message;
	}

	public String getPlainMessage(String key) {
		return decrypt(message, key);
	}

	private String encrypt(String plaintext, String key) {
		return null;
	}

	private String decrypt(String ciphertext, String key) {
		return null;
	}

	@Override
	public String toString() {
		return getCipherMessage();
	}
}
