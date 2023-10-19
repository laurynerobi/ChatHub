package src;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

//implements runnable interface
//runnable causing server to be passed to thread
public class Server implements Runnable {
    private ArrayList<ClientConnectHandler> connectedClients;
    private ServerSocket myServer;
    private boolean complete;
    private ExecutorService pool;

    public Server(){
        connectedClients = new ArrayList<>();
        complete = false;
    }

    @Override
    public void run() {
        try {
            //pass the port number only
            myServer = new ServerSocket(7000);
            pool = Executors.newCachedThreadPool();
            while(!complete) {

                //when connection accepted socket created.
                Socket client = myServer.accept();
                //holds all the connected users/clients
                ClientConnectHandler myHandler = new ClientConnectHandler(client);
                connectedClients.add(myHandler);
                pool.execute(myHandler);
            }
        }
        catch (IOException exception) {
            exitHandler();
        }
    }
    //sending text to all connected clients
    public void messenger(String text){
        for (ClientConnectHandler connect : connectedClients) {
           if(connect != null){
               connect.sendText(text);
           }
        }
    }

    //to handle termination of the server
    public void exitHandler () {
        try {
            complete = true;
            pool.shutdown();
            if (!myServer.isClosed()) {
                myServer.close();
            }
            for (ClientConnectHandler connect : connectedClients){
                connect.exitHandler();
            }
        }catch(IOException exception) {
                //ignore
            }
        }
    //handles each client connection
    class ClientConnectHandler implements Runnable{
        //instance of clientConnections to handle multiple clients concurrently
        private Socket myClient;
        private BufferedReader reader;
        private PrintWriter writer;
        private String username;

        public ClientConnectHandler(Socket myClient){
            this.myClient = myClient;
        }

        public String getUsername() {
            return username;
        }

        public Socket getSocket() {
            return myClient;
        }

        @Override
        public void run() {
            try {
                //deal with client
                //initialize reader and writer
                reader = new BufferedReader(new InputStreamReader(myClient.getInputStream()));
                writer = new PrintWriter(myClient.getOutputStream(),true);
                //prompt user for their username
                writer.println("Welcome to CHATHUB! Your number one chatting service.");
                writer.println("Please enter your username: ");
                username = reader.readLine();
                //checks if username is blank
//                if(username.isBlank()){
//                    writer.println("Enter a valid user name");
//                    username = reader.readLine();
//                }
                System.out.println(username + " is connected");
                messenger(username + " joined the party!");

                //loop to always ask for new messages
                String clientMessage;
                while ((clientMessage = reader.readLine()) != null) {
                    if(clientMessage.startsWith("/user ")) {
                        //handle username
                        String[] divideMessage = clientMessage.split(" ", 2);
                        if (divideMessage.length == 2) {
                            messenger(username + "changed their username to " + divideMessage[1]);
                            System.out.println(username + "changed their username to " + divideMessage[1]);
                            username = divideMessage[1];
                            writer.println("Username changed to " + username);
                        } else {
                            writer.println("Alert: No username was provided!");
                        }
                    }   else if (clientMessage.startsWith("/quit ")) {
                        messenger(username + " has left the chat!");
                        writer.flush();
                        exitHandler();
                    }else {
                        // if no command send username and message to all other users in the chat
                        messenger(username + ": " + clientMessage);
                    }
                }

            } catch (IOException i){
                exitHandler();
            }
        }
        public void sendText (String text) {
            writer.println(text);
        }

        public void exitHandler(){
            try {
                reader.close();
                writer.close();
            if (!myClient.isClosed()) {
                myClient.close();
            }
            } catch(IOException exception) {
                //ignore
                }
            }

        private ConcurrentHashMap<ClientConnectHandler, PrintWriter> connectedClientsWithWriters = new ConcurrentHashMap<>();

        //file handling
        public void forwardFile(String senderUsername, String receiverUsername, String filename, byte[] data) {
            // Check if the receiver is connected
            for (ClientConnectHandler client : connectedClients) {
                if (client.getUsername().equals(receiverUsername)) {
                    client.receiveFile(senderUsername, filename, data);
                    break;
                }
            }
        }

        public void receiveFile(String senderUsername, String filename, byte[] data) {
            try {
                // Create a directory for received files (if it doesn't exist)
                File receivedDir = new File("received_files");
                if (!receivedDir.exists()) {
                    receivedDir.mkdir();
                }

                // Create a file in the received_files directory
                File receivedFile = new File("received_files", filename);

                // Write the received data to the file
                FileOutputStream fos = new FileOutputStream(receivedFile);
                fos.write(data);
                fos.close();

                System.out.println(senderUsername + " sent a file: " + filename);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }



    }


        public static void main(String[] args) {
    Server myServer = new Server();
    myServer.run();
    }

}

