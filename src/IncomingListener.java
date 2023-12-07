public class IncomingListener implements Runnable {
    private Client client;
    private Buffer buffer;

    public IncomingListener(Client c, Buffer b){
        client = c;
        buffer = b;
    }

    @Override
    public void run() {
        while(!buffer.isCloseConnection()){
            if(client.isRSAReady() && client.isAESReady()) {
                client.receiveData();
            } else {
                client.receiveRSAInfo();
                client.sendAESInfo();
                client.receiveAESInfo();
            }
        }
    }
}
