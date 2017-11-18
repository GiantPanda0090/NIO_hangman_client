package Client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Queue;
import java.util.Scanner;

public class Client {
    public static Queue pendingS = new ArrayDeque();
    public static Queue pending = new ArrayDeque();
    private static ByteBuffer buff = ByteBuffer.allocateDirect(8192);
    private static SocketChannel listendingChannel;
    private static Selector selector = null;
    private static boolean connect = false;
    private static Client client;
    private static Game game;
    private static Queue<String> queue = new ArrayDeque();
    private static boolean receiveFlag = false;
    private static boolean sendFlag = false;
    private int port = 9000;
    private String ip;

    /*
    end
     */
    public static void main(String[] args) {

        try {
            //init
            client = new Client();
            client.ip = "localhost";
            //ServerSocket listendingSocket= new ServerSocket(8001);//back door
            listendingChannel = SocketChannel.open();
            listendingChannel.configureBlocking(false);
            listendingChannel.connect(new InetSocketAddress(client.ip, client.port));
            selector = Selector.open();
            listendingChannel.register(selector, SelectionKey.OP_CONNECT);
            Scanner keyboard = new Scanner(System.in);
            connect = true;

            while (connect) {
                if (sendFlag) {
                    cleanupQ();
                    sendFlag = false;
                }
                selector.select();
                for (SelectionKey key : selector.selectedKeys()) {
                    selector.selectedKeys().remove(key);
                    if (!key.isValid()) {
                        continue;
                    }
                    if (key.isConnectable()) {
                        listendingChannel.finishConnect();
                        System.out.println("Socket open!");
                        System.out.println("\n==================================");
                        System.out.println("Input Option:\nstart -> Start the game\nquit -> Quit the game\n==================================\n\nUser allow to input quit into the guess box anytime during the game to Quit the game!\n\nHow to play:\nYou can either guess word by word or finish up the rest of the letter once for all\nPlayer has same amount of chance as the length of the word!\nGood luck and Have fun!");
                        game = new Game(listendingChannel, keyboard, client, key);
                        key.interestOps(SelectionKey.OP_WRITE);
                    } else if (key.isWritable()) {
                        client.send(key);//start quit
                    } else if (key.isReadable()) {
                        //confirm
                        client.receive(key);//session

                    } else {
                        System.err.println("Key not readable");
                        break;
                    }

                }
            }
            keyboard.close();

        } catch (IOException e) {
            System.err.println(e);
        }

    }

    public static void selectorWake() {
        selector.wakeup();
    }

    /*
  Reference from:http://rox-xmlrpc.sourceforge.net/niotut/
  to solve deadlock
   */
    private static void cleanupQ() {
        synchronized (pendingS) {
            Iterator request = pendingS.iterator();
            while (request.hasNext()) {
                PendingRequest current = (PendingRequest) request.next();
                SelectionKey key = current.socket.keyFor(selector);
                key.interestOps(current.ops);
            }
        }
        pendingS.clear();
    }

    public void receive(SelectionKey key) throws IOException {
        game.start(re());
        key.interestOps(SelectionKey.OP_WRITE);

    }

    public void sendQ(String msg) {
        synchronized (queue) {
            queue.add(msg);
        }
        pendingS.add(new PendingRequest(listendingChannel, SelectionKey.OP_WRITE));
        sendFlag = true;
        selector.wakeup();

    }

    public void send(SelectionKey key) throws IOException {
        if (queue.isEmpty()) {
            game.start();
        } else {
            String msg = null;
            synchronized (queue) {
                while ((msg = queue.peek()) != null) {
                    se(msg);
                    queue.remove();
                }
            }
            game.phaseInc();

        }
        key.interestOps(SelectionKey.OP_READ);


    }

    public String re() throws IOException {
        try {
            synchronized (buff) {
                synchronized (listendingChannel) {
                    buff.clear();
                    listendingChannel.read(buff);
                    buff.flip();
                }
            }
            byte[] bytes = new byte[buff.remaining()];
            buff.get(bytes);
            System.out.println("Received " + bytes.length + " byte.");
            return new String(bytes);
        } catch (IOException e) {
            System.err.println(e);
        }
        return null;

    }

    public void se(String content) {
        ByteBuffer sendBuff = ByteBuffer.wrap(content.getBytes());
        try {
            int wrote = listendingChannel.write(sendBuff);
            System.out.println("wrote " + wrote + " byte.");
            sendBuff.clear();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /*
    Reference from:http://rox-xmlrpc.sourceforge.net/niotut/
     */
    private class PendingRequest {
        //init
        public SocketChannel socket;
        public int ops;

        public PendingRequest(SocketChannel socket, int ops) {
            this.socket = socket;
            this.ops = ops;
        }
    }
    /*
    end
     */
}
