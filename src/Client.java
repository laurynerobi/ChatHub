package src;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class Client implements Runnable {
    private Socket myClient;
    private BufferedReader reader;
    private PrintWriter writer;

    public void run(){
        try{
            Socket myClient =  new Socket("127.0.0.1", 5000);
            reader = new BufferedReader(new InputStreamReader(myClient.getInputStream()));
            writer = new PrintWriter(myClient.getOutputStream(), true);

        } catch (IOException exception) {
            //TODO: handle
        }
    }

    class clientInputHandler implements Runnable{

        @Override
        public void run() {
            try{
                BufferedReader inReader = new BufferedReader(.)
            } catch (IOException exception) {
                //TODO: handle
            }
        }
    }
}
