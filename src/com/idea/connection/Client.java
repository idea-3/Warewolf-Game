package com.idea.connection;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.*;

/**
 * Created by angelynz95 on 25-Apr-16.
 */
public class Client {
    private Socket clientSocket;
    private BufferedReader in;
    private PrintWriter out;
    private Scanner scan;
    private String username;
    public static final HashMap<String, List<String>> clientToServerRequestKeys = initializedClientToServerRequestKeys();
    public static final HashMap<String, List<String>> clientToClientRequestKeys = initializedClientToClientRequestKeys();
    public static final List<String> clientMethodValues = Arrays.asList("join", "leave", "ready", "client_address",
                                                                        "vote_result_werewolf",  "vote_result_civilian",
                                                                        "start", "change_phase", "game_over");

    /**
     * Konstruktor
     * @param hostName hostname server
     * @param port port server
     */
    public Client(String hostName, int port) throws IOException {
        clientSocket = new Socket(hostName, port);
        in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        out = new PrintWriter(clientSocket.getOutputStream(), true);
        scan = new Scanner(System.in);
    }

    /**
     * Menginisialisasi request keys dari client ke server
     * @return map kunci request JSON yang telah diinisialisasi
     */
    private static HashMap initializedClientToServerRequestKeys() {
        HashMap<String, List<String>> requestKeys = new HashMap<>();
        List<String> keys;
        keys = Arrays.asList("method", "username", "udp_address", "udp_port");
        requestKeys.put("join", keys);

        keys = Arrays.asList("method");
        requestKeys.put("leave", keys);
        requestKeys.put("ready", keys);
        requestKeys.put("client_address", keys);

        keys = Arrays.asList("method", "kpu_id", "description");
        requestKeys.put("prepare_proposal", keys);

        keys = Arrays.asList("method", "vote_status", "player_killed", "vote_result");
        requestKeys.put("vote_result_werewolf", keys);

        keys = Arrays.asList("method", "vote_status", "player_killed", "vote_result");
        requestKeys.put("vote_result_civilian", keys);

        keys = Arrays.asList("method", "vote_status", "vote_result");
        requestKeys.put("vote_result", keys);

        keys = Arrays.asList("method", "time", "role", "friend", "description");
        requestKeys.put("start", keys);

        keys = Arrays.asList("method", "time", "days", "description");
        requestKeys.put("change_phase", keys);

        keys = Arrays.asList("method", "winner", "description");
        requestKeys.put("game_over", keys);

        return requestKeys;

    }

    /**
     * Menginisialisasi request keys dari client ke client lainnya
     * @return map kunci request JSON yang telah diinisialisasi
     */
    private static HashMap initializedClientToClientRequestKeys() {
        HashMap<String, List<String>> requestKeys = new HashMap<>();
        List<String> keys;

        keys = Arrays.asList("method", "proposal_id");
        requestKeys.put("prepare_proposal", keys);

        keys = Arrays.asList("method", "proposal_id", "kpu_id");
        requestKeys.put("accept_proposal", keys);

        keys = Arrays.asList("method", "player_id");
        requestKeys.put("vote_werewolf", keys);

        keys = Arrays.asList("method", "player_id");
        requestKeys.put("vote_civilian", keys);

        return requestKeys;
    }

    /**
     * Menjalankan client
     */
    public void run() throws JSONException, IOException {
        joinGame();
        out.close();
        in.close();
        clientSocket.close();
    }

    /**
     * Mengirim request ke server
     */
    private void sendToServer(JSONObject request) {
        System.out.println("Request to server: " + request);
        out.println(request.toString());
        out.flush();
    }

    /**
     * Menerima response dari server
     */
    private JSONObject receiveFromServer() throws JSONException, IOException {
        JSONObject response;
        String serverString;
        StringBuilder stringBuilder = new StringBuilder();

        serverString = in.readLine();
        stringBuilder.append(serverString);
        response = new JSONObject(stringBuilder.toString());
        System.out.println("Response from server: " + response);

        return response;
    }

    /**
     * Request join game ke server
     */
    private void joinGame() throws JSONException, IOException {
        System.out.print("Username: ");
        String username = scan.nextLine();
        JSONObject request = requestJoinGame(username);
        sendToServer(request);
        receiveFromServer();
    }

    /**
     * Menyusun JSON untuk request join game ke server
     * @param username username client
     */
    private JSONObject requestJoinGame(String username) throws JSONException {
        JSONObject request = new JSONObject();
        request.put("method", "join");
        request.put("username", username);
        request.put("udp_address", clientSocket.getLocalAddress().getHostAddress());
        request.put("udp_port", clientSocket.getLocalPort());

        return request;
    }

    public static void main(String[] args) throws IOException, JSONException {
        Scanner scan = new Scanner(System.in);
        String hostName;
        int port;

        System.out.print("Input server IP host name: ");
        hostName = scan.nextLine();
        System.out.print("Input server port: ");
        port = Integer.parseInt(scan.nextLine());

        System.out.println("Connecting to " + hostName + " on port " + port);
        Client client = new Client(hostName, port);
        client.run();
    }
}
