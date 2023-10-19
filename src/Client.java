package src;

import java.io.*;
import java.net.Socket;

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

            //creating a thread for the clientInputHandler
            clientInputHandler myHandler = new clientInputHandler();
            Thread thread = new Thread(myHandler);
            thread.start(); //to open a separate thread

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

        @Override
        public void run() {
            try{
                //passing command line input
                BufferedReader inputReader = new BufferedReader(new InputStreamReader(System.in));
                while(!complete) {
                    String text = inputReader.readLine();
                    if(text.equals("/quit")) {
                        writer.println("has left the chat");
                        inputReader.close();
                        shutdown();
                    }
                    //handling file sending and receiving
                    else if (text.startsWith("/sendfile ")){
                        String[] parts = text.split(" ");
                        if (parts.length == 3) {
                            String receiverUsername = parts[1];
                            String filename = parts[2];
                            sendFile(receiverUsername, filename);
                        }else {
                            System.out.println("Invalid file send command. Use /sendfile <username> <filename>");

                        }
                    } else { //sends text to the server
                        writer.println(text);
                    }
                }
            } catch (IOException exception) {
                shutdown();
            }
        }

        //file handling
        public void sendFile(String receiverUsername, String filename) {
            try {
                File file = new File(filename);
                byte[] fileData = new byte[(int) file.length()];
                FileInputStream fis = new FileInputStream(file);
                BufferedInputStream bis = new BufferedInputStream(fis);
                bis.read(fileData, 0, fileData.length);

                writer.println("/file " + receiverUsername + " " + filename);
                writer.println(fileData.length); // Send the file size
                writer.flush();
                writer.write(String.valueOf(fileData), 0, fileData.length);
                writer.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void receiveFile(String senderUsername, String filename, byte[] data) {
            try {
                // Implement code to save the received file
                // For example, save it to a user-specific directory
                File receivedFile = new File("received_files/" + filename);
                FileOutputStream fos = new FileOutputStream(receivedFile);
                fos.write(data);
                fos.close();
                System.out.println("File received from " + senderUsername + ": " + filename);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    public static void main(String[] args) {
        Client client = new Client();
        client.run();
    }
}
