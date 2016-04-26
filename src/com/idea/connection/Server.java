package com.idea.connection;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;

/**
 * Created by angelynz95 on 25-Apr-16.
 */
public class Server {
    private ArrayList<ClientController> clients;
    private InetAddress ip;
    private ServerSocket serverSocket;

    /**
     * Konstruktor
     * @param port port server
     */
    public Server(int port) throws IOException {
        clients = new ArrayList<>();
        ip = InetAddress.getLocalHost();
        serverSocket = new ServerSocket(port);
    }

    /**
     * Controller untuk setiap client yang terhubung dengan server
     */
    private class ClientController extends Thread {
        private int clientId;
        private JSONObject request;
        private JSONObject response;
        private BufferedReader in;
        private PrintWriter out;
        private Socket clientSocket;
        private String username;

        /**
         * Konstruktor
         * @param clientSocket socket client
         */
        public ClientController(Socket clientSocket, int clientId) throws IOException {
            this.clientId = clientId;
            this.clientSocket = clientSocket;
            in = new BufferedReader(new InputStreamReader(this.clientSocket.getInputStream()));
            out = new PrintWriter(this.clientSocket.getOutputStream(), true);
        }

        /**
         * Menjalankan controller client
         */
        public void run() {
            String clientString;
            StringBuilder stringBuilder;

            try {
                while ((clientString = in.readLine()) != null) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append(clientString);
                    request = new JSONObject(stringBuilder.toString());
                    System.out.println("Request from client: " + request);
                    checkMethod(request.get("method").toString());
                    System.out.println("Response to client: " + response);
                    out.println(response.toString());
                    out.flush();
                    System.out.println();
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            } finally {
                try {
                    in.close();
                    out.close();
                    clientSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        /**
         * Menentukan response server berdasarkan method dari request client
         * @param method method dari request client
         */
        private void checkMethod(String method) throws JSONException {
            switch(method) {
                case "join":
                    respondJoinGame();
                    break;
            }
        }

        /**
         * Menyusun JSON untuk merespon request join game dari client
         */
        private void respondJoinGame() throws JSONException {
            username = request.get("username").toString();
            response = new JSONObject();
            response.put("status", "ok");
            response.put("player_id", clientId);
        }
    }

    /**
     * Memanggil ClientController
     */
    public void controlClient(Socket clientSocket) throws IOException {
        int clientId = clients.size();
        ClientController clientController = new ClientController(clientSocket, clientId);
        clients.add(clientController);
        Thread thread = new Thread(clientController);
        thread.start();
    }

    /**
     * Menjalankan server
     */
    public void run() throws IOException {
        String hostName = ip.getHostName();

        System.out.println("Server IP address: " + ip);
        System.out.println("Server IP host name : " + hostName);
        while (true) {
            Socket clientSocket = serverSocket.accept();
            System.out.println("Just connected to " + clientSocket.getRemoteSocketAddress() + " " + clientSocket.getPort());
            controlClient(clientSocket);
        }
    }

    public static void main(String[] args) throws IOException {
        int port;
        Scanner scan = new Scanner(System.in);

        System.out.print("Input server port: " );
        port = Integer.parseInt(scan.nextLine());
        Server server = new Server(port);
        server.run();
    }
}
