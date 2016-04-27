package com.idea.connection;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;
import java.util.*;

/**
 * Created by angelynz95 on 25-Apr-16.
 */
public class Client {
    private BufferedReader tcpIn;
    private int id;
    private DatagramSocket udpSocket;
    private PrintWriter tcpOut;
    private Scanner scan;
    private Socket tcpSocket;
    private String username;
    public static final HashMap<String, List<String>> clientToServerRequestKeys = initializedClientToServerRequestKeys();
    public static final HashMap<String, List<String>> clientToClientRequestKeys = initializedClientToClientRequestKeys();

    /**
     * Konstruktor
     * @param hostName hostname server
     * @param port port server
     * @param udpPort port UDP
     */
    public Client(String hostName, int port, int udpPort) throws IOException {
        tcpSocket = new Socket(hostName, port);
        udpSocket = new DatagramSocket(udpPort);
        tcpIn = new BufferedReader(new InputStreamReader(tcpSocket.getInputStream()));
        tcpOut = new PrintWriter(tcpSocket.getOutputStream(), true);
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
     * @throws JSONException
     * @throws IOException
     */
    public void run() throws JSONException, IOException {
        joinGame();
        if (id == 0) {
            System.out.println("Client address: " + InetAddress.getLocalHost().getHostAddress());
            System.out.println("Client port: " + udpSocket.getLocalPort());
            while (true) {
                DatagramPacket packet = receiveFromClient();
                getData(packet);
            }
        } else {
            JSONObject data = new JSONObject();
            System.out.print("Input client address: ");
            InetAddress address = InetAddress.getByName(scan.nextLine());
            System.out.print("Input client port: ");
            int port = Integer.parseInt(scan.nextLine());
            data.put("client", username);
            data.put("message", "hello");
            sendToClient(data, address, port);
        }
        udpSocket.close();
        tcpOut.close();
        tcpIn.close();
        tcpSocket.close();
    }

    /**
     * Mengirim request ke server
     * @param request request yang dikirim ke server
     */
    private void sendToServer(JSONObject request) {
        System.out.println("Request to server: " + request);
        tcpOut.println(request.toString());
        tcpOut.flush();
    }

    /**
     * Menerima response dari server
     * @return response dari server dalam JSON
     * @throws JSONException
     * @throws IOException
     */
    private JSONObject receiveFromServer() throws JSONException, IOException {
        JSONObject response;
        String serverString;
        StringBuilder stringBuilder = new StringBuilder();

        serverString = tcpIn.readLine();
        stringBuilder.append(serverString);
        response = new JSONObject(stringBuilder.toString());
        id = response.getInt("player_id");
        System.out.println("Response from server: " + response);

        return response;
    }

    /**
     * Mengirim data ke client lain
     * @param data data yang dikirim ke client lain
     * @param address alamat client lain
     * @param port port UDP client lain
     * @throws IOException
     */
    private void sendToClient(JSONObject data, InetAddress address, int port) throws IOException {
        byte[] dataByte = data.toString().getBytes();
        DatagramPacket packet = new DatagramPacket(dataByte, dataByte.length, address, port);
        System.out.println("Data to client " + address + " " + port + ": " + data);
        udpSocket.send(packet);
    }

    /**
     * Menerima data dari client lain
     * @return paket yang diterima dalam DatagramPacket
     * @throws IOException
     */
    private DatagramPacket receiveFromClient() throws IOException {
        byte[] dataByte = new byte[1024];
        DatagramPacket packet = new DatagramPacket(dataByte, dataByte.length);
        udpSocket.receive(packet);

        return packet;
    }

    /**
     * Mengambil data dari paket UDP dalam JSON
     * @param packet paket UDP
     * @return data dari paket UDP dalam JSON
     * @throws JSONException
     */
    private JSONObject getData(DatagramPacket packet) throws JSONException {
        StringBuilder stringBuilder = new StringBuilder();

        String dataString = new String(packet.getData());
        stringBuilder.append(dataString);
        JSONObject data = new JSONObject(stringBuilder.toString());
        System.out.println("Data from client " + packet.getAddress() + " " + packet.getPort() + ": " + data);

        return data;
    }

    /**
     * Request join game ke server
     */
    private void joinGame() throws JSONException, IOException {
        System.out.print("Username: ");
        username = scan.nextLine();
        JSONObject request = requestJoinGame(username);

        sendToServer(request);
        receiveFromServer();
    }

    /**
     * Menyusun JSON untuk request join game ke server
     * @param username username client
     */
    private JSONObject requestJoinGame(String username) throws JSONException, UnknownHostException {
        JSONObject request = new JSONObject();
        request.put("method", "join");
        request.put("username", username);
        request.put("udp_address", InetAddress.getLocalHost().getHostAddress());
        request.put("udp_port", udpSocket.getLocalPort());

        return request;
    }

    public static void main(String[] args) throws IOException, JSONException {
        Scanner scan = new Scanner(System.in);

        System.out.print("Input server IP host name: ");
        String hostName = scan.nextLine();
        System.out.print("Input server port: ");
        int port = Integer.parseInt(scan.nextLine());
        System.out.print("Input UDP port: ");
        int udpPort = Integer.parseInt(scan.nextLine());
        System.out.println(InetAddress.getLocalHost().getHostAddress());

        System.out.println("Connecting to " + hostName + " on port " + port);
        Client client = new Client(hostName, port, udpPort);
        client.run();
    }
}
