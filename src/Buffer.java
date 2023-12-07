import java.util.ArrayList;

public class Buffer {
    private Message outgoingMessage;
    private boolean outgoingMessageToRead = false;
    private Object outgoingLock = new Object();

    private boolean closeConnection = false;

    private ArrayList<Message> messageList = new ArrayList<Message>();
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

            return outgoingMessage;
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

            this.outgoingMessage = message;
        }
    }

    public boolean isCloseConnection() {
        return closeConnection;
    }

    public void setCloseConnection(boolean closeConnection) {
        this.closeConnection = closeConnection;
    }

    public ArrayList<Message> getMessageList() {
        return messageList;
    }
}
