package com.idea.connection;

import org.json.JSONArray;
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
    public static final HashMap<String, List<String>> clientToServerRequestKeys = initializedClientToServerRequestKeys();
    public static final HashMap<String, List<String>> clientToClientRequestKeys = initializedClientToClientRequestKeys();
    public static final HashMap<String, String> commands = initializedCommands();
    public static final HashMap<Integer, String> answers = initializedAnswerChoices();

    private BufferedReader tcpIn;
    private PrintWriter tcpOut;
    private DatagramSocket udpSocket;
    private Socket tcpSocket;
    private Scanner scan;

    private int id;
    private int proposalId;
    private int leaderId;
    private boolean isProposer;
    private boolean isLeader;
    private boolean isAlive;
    private String username;
    private List<ClientInfo> clientsInfo;

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

        proposalId = -1;
        leaderId = -1;
        isProposer = false;
        isLeader = false;
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

    private static HashMap initializedCommands() {
        HashMap<String, String> commands = new HashMap<>();
        commands.put("join", "Joining the game...");
        commands.put("leave", "Do you want to leave the game? (y/n)");
        commands.put("ready", "Are you ready? (y/n)");
        commands.put("username", "Username: ");
        commands.put("left", "Are you sure want to leave the game?");
        commands.put("client_list", "Retrieving players information...");
        commands.put("prepare_proposal_client", "Propose to be a leader...");
        commands.put("accept_proposal_client", "Accept to be a leader...");
        commands.put("player_address_lost", "Lost a player address...");
        commands.put("prepare_me_leader", "I am a leader (propose)...");
        commands.put("accept_me_leader", "I am a leader (accept)...");

        return commands;
    }

    private static HashMap initializedAnswerChoices() {
        HashMap<Integer, String> answerChoices = new HashMap<>();
        answerChoices.put(1, "y");
        answerChoices.put(0, "n");

        return answerChoices;
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
        System.out.println("Received data: " + packet);

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
     * @throws JSONException
     * @throws IOException
     */
    private void joinGame() throws JSONException, IOException {
        System.out.println(commands.get("join"));
        boolean isValid = false;
        do {
            System.out.print(commands.get("username"));
            username = scan.nextLine();

            JSONObject request = requestJoinGame(username);
            sendToServer(request);

            JSONObject response = receiveFromServer();
            String status = response.getString("status");
            switch (status) {
                case "ok":
                    isValid = true;
                    id = response.getInt("player_id");
                    break;
                default:
                    String description = response.getString("description");
                    System.out.println(description);
                    break;
            }
        } while (!isValid);
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

    /**
     * Request meninggalkan game ke server
     * @throws JSONException
     * @throws IOException
     */
    private void leaveGame() throws JSONException, IOException {
        System.out.println(commands.get("leave"));
        String decision = scan.nextLine();
        if (decision.equals(answers.get(1))) {
            JSONObject request = requestLeaveGame();
            sendToServer(request);
            JSONObject response = receiveFromServer();
            String status = response.getString("status");
            switch (status) {
                case "ok":
                    System.out.println(commands.get("left"));
                    break;
                default:
                    String description = response.getString("description");
                    System.out.println(description);
                    break;
            }
        }
    }

    /**
     * Menyusun JSON untuk request leave game ke server
     * @return objek JSON yang akan dikirim
     * @throws JSONException
     */
    private JSONObject requestLeaveGame() throws JSONException {
        JSONObject request = new JSONObject();
        request.put("method", "leave");

        return request;
    }


    /**
     * Menyatakan diri telah siap bermain kepada server
     * @throws IOException
     * @throws JSONException
     */
    private void readyUp() throws IOException, JSONException {
        System.out.println(commands.get("ready"));
        String decision = scan.nextLine();
        if (decision.equals(answers.get(1))) {
            JSONObject request = new JSONObject();
            sendToServer(request);
            JSONObject response = receiveFromServer();
            System.out.println(response.getString("description"));
        }
    }

    /**
     * Menyusun JSON untuk menyatakan diri sudah siap bermain ke server
     * @return objek JSON yang akan dikirim
     * @throws JSONException
     */
    private JSONObject requestReadyUp() throws JSONException {
        JSONObject request = new JSONObject();
        request.put("method", "ready");

        return request;
    }

    /**
     * Meminta daftar client dari server dan menyimpannya
     * @throws JSONException
     * @throws IOException
     */
    private void askClientList() throws JSONException, IOException {
        boolean isSuccess = true;
        do {
            System.out.println(commands.get("client_list"));
            JSONObject request = requestClientAddress();
            sendToServer(request);

            JSONObject response = receiveFromServer();
            System.out.println(response.getString("description"));
            String status =  response.getString("status");
            switch (status) {
                case "ok":
                    saveClientList(response.getJSONArray("clients"));
                    break;
                default:
                    isSuccess = false;
                    break;
            }
        } while (!isSuccess);
    }

    /**
     * Menyimpan daftar client
     * @param clients array berisi response daftar client dari server
     * @throws JSONException
     */
    private void saveClientList(JSONArray clients) throws JSONException {
        for (int i=0; i<clients.length(); i++) {
            JSONObject client = clients.getJSONObject(i);
            int clientId = client.getInt("player_id");
            if (clientId != id) {
                // ID client bukan dirinya sendiri
                boolean isAlive = client.getBoolean("is_alive");
                String addressName = client.getString("address");
                InetAddress address = null;
                try {
                    address = InetAddress.getByName(addressName);
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                    System.out.println(commands.get("player_address_lost"));
                }
                int port = client.getInt("port");
                String username = client.getString("username");

                // Memeriksa keberadaan client di clientsInfo
                int idInClientsInfo = getIdxInClientsInfo(clientId);
                if (idInClientsInfo == -1) {
                    clientsInfo.add(new ClientInfo(clientId, isAlive, address, port, username));
                } else {
                    if (clientsInfo.get(idInClientsInfo).isAlive() != isAlive) {
                        clientsInfo.get(idInClientsInfo).set(clientId, isAlive, address, port, username);
                        System.out.println("Player " + clientId + " is dead.");
                    }
                }
            }
        }
    }

    /**
     * Memeriksa apakah ID client pada parameter terdapat pada clientsInfo pada atribut
     * @param clientId yang ingin dibandingkan
     * @return indeks keberadaan ClientInfo yang dicari, bernilai -1 apabila tidak ditemukan
     */
    private int getIdxInClientsInfo(int clientId) {
        int i = 0;
        boolean isExist = false;
        while (i<clientsInfo.size() && !isExist) {
            ClientInfo clientInfo = clientsInfo.get(i);
            if (clientInfo.getPlayerId()==clientId) {
                isExist = true;
            } else {
                i++;
            }
        }

        if (!isExist) {
            return -1;
        } else {
            return i;
        }
    }

    /**
     * Menyusun JSON untuk meminta informasi seluruh client
     * @return objek JSON yang akan dikirim
     * @throws JSONException
     */
    private JSONObject requestClientAddress() throws JSONException {
        JSONObject request = new JSONObject();
        request.put("method", "client_address");

        return request;
    }

    /**
     * Mengirimkan proposal dari proposer ke acceptor
     * @throws JSONException
     * @throws IOException
     */
    private void prepareProposalToClient() throws JSONException, IOException {
        System.out.println(commands.get("prepare_proposal_client"));
        JSONObject request = requestPrepareProposalToClient();
        for (int i=0; i<clientsInfo.size(); i++) {
            InetAddress address = clientsInfo.get(i).getAddress();
            int port = clientsInfo.get(i).getPort();
            sendToClient(request, address, port);
        }
        int receivedPacket = 0;
        int okProposal = 0;
        while (receivedPacket < clientsInfo.size()-1) {
            // Menunggu paket sampai seluruh acceptor mengirim response
            DatagramPacket packet = receiveFromClient();
            JSONObject response = new JSONObject(new String(packet.getData(), 0, packet.getLength()));
            System.out.println("Response converted to JSON: " + response);

            String status = response.getString("status");
            String description = response.getString("description");
            if (!status.equals("error")) {
                System.out.println(getClientIdByAddress(packet.getAddress(), packet.getPort()) + ": " + description + " proposal.");
            } else {
                System.out.println(getClientIdByAddress(packet.getAddress(), packet.getPort()) + ": " + description);
                if (status.equals("ok")) {
                    okProposal++;
                }
            }
            if (okProposal == receivedPacket) {
                isLeader = true;
                System.out.println(commands.get("prepare_me_leader"));
            }
            // TODO: Previous accepted dipakai?
        }
    }

    /**
     * Mencari client dengan alamat dan port dari parameter pada list clientsInfo
     * @param address alamat yang ingin ditemukan
     * @param port port yang ingin ditemukan
     * @return ID client yang ditemukan, bernilai -1 jika tidak ditemukan
     */
    private int getClientIdByAddress(InetAddress address, int port) {
        int clientId = 0;
        int i = 0;
        boolean isFound = false;
        while (i<clientsInfo.size() && !isFound) {
            ClientInfo clientInfo = clientsInfo.get(i);
            if (clientInfo.getAddress().equals(address) && clientInfo.getPort()==port) {
                isFound = true;
            } else {
                i++;
            }
        }

        if (isFound) {
            return clientId;
        } else {
            return -1;
        }
    }

    /**
     * Menyusun JSON untuk request menyiapkan proposal dari proposer ke acceptor
     * @return objek JSON yang akan dikirim
     * @throws JSONException
     */
    private JSONObject requestPrepareProposalToClient() throws JSONException {
        JSONObject request = new JSONObject();
        request.put("method", "prepare_proposal");
        request.put("proposal_id", getProposalId());

        return request;
    }

    private void acceptProposalToClient() throws JSONException, IOException {
        System.out.println(commands.get("accept_proposal_client"));
        JSONObject request = requestAcceptProposal();
        for (int i=0; i<clientsInfo.size(); i++) {
            InetAddress address = clientsInfo.get(i).getAddress();
            int port = clientsInfo.get(i).getPort();
            sendToClient(request, address, port);
        }
        int receivedPacket = 0;
        int okProposal = 0;
        while (receivedPacket < clientsInfo.size()-1) {
            // Menunggu paket sampai seluruh acceptor mengirim response
            DatagramPacket packet = receiveFromClient();
            JSONObject response = new JSONObject(new String(packet.getData(), 0, packet.getLength()));
            System.out.println("Response converted to JSON: " + response);

            String status = response.getString("status");
            String description = response.getString("description");
            if (!status.equals("error")) {
                System.out.println(getClientIdByAddress(packet.getAddress(), packet.getPort()) + ": " + description + " proposal.");
            } else {
                System.out.println(getClientIdByAddress(packet.getAddress(), packet.getPort()) + ": " + description);
                if (status.equals("ok")) {
                    okProposal++;
                }
            }
            if (okProposal == receivedPacket) {
                isLeader = true;
                System.out.println(commands.get("accept_me_leader"));
            }
        }
    }

    /**
     * Menyusun JSON untuk request menerima dari proposer ke acceptor
     * @return objek JSON yang akan dikirim
     * @throws JSONException
     */
    private JSONObject requestAcceptProposal() throws JSONException {
        JSONObject request = new JSONObject();
        request.put("method", "accept_proposal");
        request.put("proposal_id", getProposalId());
        request.put("kpu_id", proposalId); // TODO: kpu_id

        return request;
    }

    private void acceptProposalToServer() {

    }

    /**
     * Menyusun JSON untuk menyatakan acceptor menerima proposal ke server
     * @return objek JSON yang akan dikirim
     * @throws JSONException
     */
    private JSONObject requestAcceptProposalToServer() throws JSONException {
        JSONObject request = new JSONObject();
        request.put("method", "prepare_proposal");
        request.put("kpu_id", leaderId);
        request.put("description", "Kpu is selected");

        return request;
    }

    /**
     * Menyusun JSON untuk request mengirimkan vote untuk membunuh werewolf ke leader
     * @param werewolfId ID werewolf yang ingin dibunuh
     * @return objek JSON yang akan dikirim
     * @throws JSONException
     */
    private JSONObject requestVoteWerewolf(int werewolfId) throws JSONException {
        JSONObject request = new JSONObject();
        request.put("method", "vote_werewolf");
        request.put("player_id", werewolfId);

        return request;
    }

    /**
     * Menyusun JSON untuk mengirim hasil vote membunuh werewolf dari leader ke server
     * @param voteStatus status vote
     * @param playerKilled ID client yang dibunuh
     * @return objek JSON yang akan dikirim
     * @throws JSONException
     */
    private JSONObject requestVoteResultWerewolf(HashMap<Integer, Integer> voteResult, int voteStatus, int playerKilled) throws JSONException {
        JSONObject request = new JSONObject();
        request.put("method", "vote_result_werewolf");
        request.put("vote_status", voteStatus);
        request.put("player_killed", playerKilled);

        List<List<Integer>> voteArrays = new ArrayList<>();
        for (Map.Entry<Integer, Integer> vote : voteResult.entrySet()) {
            Integer clientId = vote.getKey();
            Integer voteNum = vote.getValue();

            List<Integer> voteArray = new ArrayList<>();
            voteArray.add(clientId);
            voteArray.add(voteNum);
            voteArrays.add(voteArray);
        }
        request.put("vote_result", voteArrays);
        return request;
    }

    /**
     * Menyusun JSON untuk request mengirimkan vote untuk membunuh civilian ke leader
     * @param civilianId ID civilian yang ingin dibunuh
     * @return objek JSON yang akan dikirim
     * @throws JSONException
     */
    private JSONObject requestVoteCivilian(int civilianId) throws JSONException {
        JSONObject request = new JSONObject();
        request.put("method", "vote_civilian");
        request.put("player_id", civilianId);

        return request;
    }

    /**
     * Menyusun JSON untuk mengirim hasil vote membunuh civilian dari leader ke server
     * @param voteStatus status vote
     * @param playerKilled ID client yang dibunuh
     * @return objek JSON yang akan dikirim
     * @throws JSONException
     */
    private JSONObject requestVoteResultCivilian(HashMap<Integer, Integer> voteStatus, int playerKilled) throws JSONException {
        JSONObject request = new JSONObject();
        request.put("method", "vote_result_civilian");
        request.put("vote_status", voteStatus);
        request.put("player_killed", playerKilled);
        List<List<Integer>> voteArrays = new ArrayList<>();
        for (Map.Entry<Integer, Integer> vote : voteResult.entrySet()) {
            Integer clientId = vote.getKey();
            Integer voteNum = vote.getValue();

            List<Integer> voteArray = new ArrayList<>();
            voteArray.add(clientId);
            voteArray.add(voteNum);
            voteArrays.add(voteArray);
        }
        request.put("vote_result", voteArrays);

        return request;
    }

    /**
     * Mendapatkan ID proposal client yang bersangkutan, terdiri dari sequence number dan ID client
     * @return ID proposal dalam JSONArray
     */
    private JSONArray getProposalId() {
        JSONArray proposalIdArray = new JSONArray();
        proposalId += 1;
        proposalIdArray.put(proposalId);
        proposalIdArray.put(id);

        return proposalIdArray;
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
