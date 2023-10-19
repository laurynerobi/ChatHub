package src;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;

public class Client implements Runnable {


    private Socket myClient;
    private BufferedReader reader;
    private PrintWriter writer;
    private boolean complete;

    public void run(){
        try{
            myClient =  new Socket("127.0.0.1", 7000);
            reader = new BufferedReader(new InputStreamReader(myClient.getInputStream()));
            writer = new PrintWriter(myClient.getOutputStream(), true);

            clientInputHandler myHandler = new clientInputHandler();
            Thread thread = new Thread(myHandler);
            thread.start();

            String clientText;
            while ((clientText = reader.readLine()) !=null){
                System.out.println(clientText);
            }
        } catch (IOException exception) {
            shutdown();
        }
    }

    private void shutdown() {
        complete = true;
        try{
            reader.close();
            writer.close();
            if(!myClient.isClosed()){
                myClient.close();
            }
        } catch (IOException e) {
            //IGNORE
        }
    }

    class clientInputHandler implements Runnable{

        private String username;
        @Override
        public void run() {
            try{
                BufferedReader inputReader = new BufferedReader(new InputStreamReader(System.in));

                while(!complete) {
                    String text = inputReader.readLine();
                    if(text.equals("/quit")) {
                        writer.println("has left the chat");
                        inputReader.close();
                        shutdown();
                    } else if (text.startsWith("/sendfile ")){
                        String[] parts = text.split(" ");
                        if (parts.length == 3) {
                            String receiverUsername = parts[1];
                            String filename = parts[2];
                            sendFile(receiverUsername, filename);
                        } else {
                            System.out.println("Invalid file send command. Use /sendfile <username> <filename>");
                        }
                    }
//                    else if(text.startsWith("/user ")) {
//                        String[] divideMessage = text.split(" ", 2);
//                        if (divideMessage.length == 2) {
//                            String oldUsername = username;
//                            username = divideMessage[1];
//                            writer.println(oldUsername + " changed their username to " + username);
//                        } else {
//                            writer.println("Alert: No username was provided!");
//                        }
//                    }
                    else {
                        writer.println(text);
                    }
                }
            } catch (IOException exception) {
                shutdown();
            }
        }

        public void sendFile(String receiverUsername, String filename) {
            try {
                File file = new File(filename);
                if (file.exists()) {
                    byte[] data = Files.readAllBytes(file.toPath());
                    writer.println("/sendfile " + receiverUsername + " " + filename);
                    for (byte b : data) {
                        writer.print((char)b);
                    }
                    writer.println(); // End of file
                    writer.flush();
                } else {
                    System.out.println("The specified file does not exist!");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        Client myClient = new Client();
        myClient.run();
    }

}

