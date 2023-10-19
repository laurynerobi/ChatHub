package src;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
            myServer = new ServerSocket(7000);
            pool = Executors.newCachedThreadPool();
            while(!complete) {
                Socket client = myServer.accept();
                ClientConnectHandler myHandler = new ClientConnectHandler(client);
                connectedClients.add(myHandler);
                pool.execute(myHandler);
            }
        }
        catch (IOException exception) {
            exitHandler();
        }
    }

    public void messenger(String text){
        for (ClientConnectHandler connect : connectedClients) {
            if(connect != null){
                connect.sendText(text);
            }
        }
    }

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

    class ClientConnectHandler implements Runnable{
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
                reader = new BufferedReader(new InputStreamReader(myClient.getInputStream()));
                writer = new PrintWriter(myClient.getOutputStream(),true);
                writer.println("Welcome to CHATHUB! Your number one communication service.");
                writer.println(" ");
                writer.println("To change your username , type: /user");
                writer.println("To exit the app, type: /quit");
                writer.println(" ");
                writer.println("Please enter your username: ");
                username = reader.readLine();
                System.out.println(username + " is connected");
                messenger(username + " joined the party!");

                String clientMessage;
                while ((clientMessage = reader.readLine()) != null) {
                    if(clientMessage.startsWith("/sendfile ")) {
                        String[] divideMessage = clientMessage.split(" ", 4);
                        if (divideMessage.length == 4) {
                            String receiverUsername = divideMessage[1];
                            String filename = divideMessage[2];
                            File file = new File(filename);
                            if (file.exists()) {
                                byte[] data = Files.readAllBytes(file.toPath());
                                forwardFile(username, receiverUsername, filename, data);
                            } else {
                                writer.println("Alert: The specified file does not exist!");
                            }
                        }
//                        } else {
//                            writer.println("Alert: Invalid command format! Use /sendfile receiverUsername filename");
//                        }
                    } else if (clientMessage.startsWith("/quit ")) {
                        messenger(username + " has left the chat!");
                        writer.flush();
                        exitHandler();
                    } else if (clientMessage.startsWith("/user ")) {
                        {
                            //handle username
                            String[] divideMessage = clientMessage.split(" ", 2);
                            if (divideMessage.length == 2) {
                                messenger(username + " changed their username to " + divideMessage[1]);
                                System.out.println(username + " changed their username to " + divideMessage[1]);
                                username = divideMessage[1];
                                writer.println("Username changed to " + username);
                            } else {
                                writer.println("Alert: No username was provided!");
                            }
                        }
                        
                    } else {
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

        public void forwardFile(String senderUsername, String receiverUsername, String filename, byte[] data) {
            for (ClientConnectHandler client : connectedClients) {
                if (client.getUsername().equals(receiverUsername)) {
                    client.receiveFile(senderUsername, filename, data);
                    break;
                }
            }
        }

        public void receiveFile(String senderUsername, String filename, byte[] data) {
            try {
                File receivedDir = new File("received_files");
                if (!receivedDir.exists()) {
                    receivedDir.mkdir();
                }

                File receivedFile = new File("received_files", filename);

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
