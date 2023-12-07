import java.util.Scanner;

public class Chat {
    public static void main(String args[]) {
        Scanner scan = new Scanner(System.in);

        Buffer buffer = new Buffer();

        System.out.print("alice or bob: ");
        String name = scan.next();
        System.out.print("RSA bit length: ");
        int bitLength = scan.nextInt();
        System.out.print("key: ");
        String key = scan.next();
        System.out.print("host: ");
        String host = scan.next();
        System.out.print("port: ");
        int port = scan.nextInt();

        Client c = new Client(name, bitLength, key, host, port, buffer);
        Thread cT = new Thread(c);
        cT.start();
        scan.nextLine();

        String nextLine = scan.nextLine();
        while(!buffer.isCloseConnection() && !nextLine.equals("/exit")) {
            buffer.makeOutgoingMessage(new Message(Message.MESSAGE, nextLine, null));
            nextLine = scan.nextLine();
        }

        buffer.makeOutgoingMessage(null);
        buffer.setCloseConnection(true);
        c.setCloseConnection(true);
        scan.close();
    }
}
