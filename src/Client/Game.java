package Client;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Scanner;
import java.util.concurrent.ForkJoinPool;

public class Game implements Runnable {
    private static Scanner keyboard = null;
    private static ByteBuffer buff = ByteBuffer.allocateDirect(8192);//reciver buff
    private static SocketChannel listendingChannel;
    private static Client client;
    private static SelectionKey key;
    private static String info;
    public int phase = 0;

    public Game(SocketChannel listendingChannel, Scanner keyboard, Client client, SelectionKey key) {
        this.keyboard = keyboard;
        this.listendingChannel = listendingChannel;
        this.client = client;
        this.key = key;

    }

    public void start() {
//run();
       /*Thread run= new Thread(this);
       run.start();*/
        ForkJoinPool.commonPool().execute(this);

/*
        return run;
*/
    }

    public void start(String info) {
        this.info = info;
        /*Thread run= new Thread(this);
        run.start();*/
        //ForkJoinPool.commonPool().execute(this);
/*
        return run;
*/
        run();
    }

    public void phaseInc() {
        phase++;
    }

    //main
    @Override
    public void run() {
        String length = "";
        //try {
        if (phase == 0) {
            System.out.print("\nInput command:");
            String keyboardIn = keyboard.nextLine();
            client.sendQ(keyboardIn);
            return;
        } else if (phase == 1) {
            if (info.compareTo("quittrigger") == 0) {
                System.exit(0);//quit
            } else if (info.compareTo("wrongcommand") == 0) {

            } else {
                length = info;//receive
                phase++;
                System.out.println("Session start!");
                System.out.println("The length of the word is: " + length);
            }
            //flag=OP.WRITE

            return;
        } else if (phase == 2) {
            //current word
            System.out.print("Your guess is: ");
            client.sendQ(keyboard.nextLine());//send answer
            System.out.println();

            return;

        } else if (phase == 3) {
            String result = info;//receive
            if (result.compareTo("correct") == 0) {
                System.out.println("The answer is correct!");
                phase--;
            } else if (result.compareTo("wrong") == 0) {
                System.out.println("The answer is wrong!");
                phase--;
            } else if (result.compareTo("KO") == 0) {
                System.out.println("YOU HAVE GUESSED OUT THE WHOLE WORD!");
                phase++;
            } else if (info.compareTo("end") == 0) {
                phase++;
            }//end the game
            //flag =OP.WRITE
            return;

        } else if (phase == 4) {
            client.sendQ("result");//flip flag

            return;
        } else if (phase == 5) {
            String status = info;
            if (status.compareTo("lose") == 0) {
                System.out.println("Client.Game Over");
            } else if (status.compareTo("win") == 0) {
                System.out.println("We have a winner!");
            } else {
                System.err.println("Error occured duing phase 4: " + info);
            }
            //flag =OP.WRITE
            phase++;
            return;
        } else if (phase == 6) {
            client.sendQ("score");//flip flag

            return;
        } else if (phase == 7) {
            String score = info;
            System.out.println("Your current score is: " + score);
            System.out.println();
            System.out.println("==========================================================");
            System.out.println("Next round!");
            phase = 0;
            return;
        }

    }
}


