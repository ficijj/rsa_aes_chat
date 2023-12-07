import java.util.ArrayList;
import java.util.List;

public class MessageBuffer {
	private Message outGoingMessage;
	private boolean outgoingMessageToRead = false;
	private Object outgoingLock = new Object();

	private ArrayList<String> messageList = new ArrayList<String>();

	public Message readOutgoingMessage() {
		synchronized (outgoingLock) {
			while (!outgoingMessageToRead) {
				try {
					outgoingLock.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			outgoingMessageToRead = false;
			outgoingLock.notifyAll();

			return outGoingMessage;
		}
	}

	public void makeOutgoingMessage(Message message) {
		synchronized (outgoingLock) {
			while (outgoingMessageToRead) {
				try {
					outgoingLock.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			outgoingMessageToRead = true;
			outgoingLock.notifyAll();

			this.outGoingMessage = message;
		}
	}

	public List<String> getMessageList() {
		return messageList;
	}

}
