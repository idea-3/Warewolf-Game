package com.idea.connection;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.xml.crypto.Data;
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
    public static final int civilianNum = 2;
    public static final int werewolfNum = 4;

    private BufferedReader tcpIn;
    private PrintWriter tcpOut;
    private DatagramSocket udpSocket;
    private Socket tcpSocket;
    private Scanner scan;

    private int id;
    private int sequenceId;
    private int leaderId;
    private int countReceiveProposal;
    private int[] promise = {0, -1};
    private String role;
    private boolean isProposer;
    private boolean isPreparedProposer;
    private boolean isLeader;
    private boolean isAlive;
    private boolean isGameOver;
    private String username;
    private HashMap<Integer, ClientInfo> clientsInfo;

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

        sequenceId = 0;
        leaderId = -1;
        role = "";
        isProposer = false;
        isLeader = false;
        isGameOver = false;
    }

    /**
     * Menginisialisasi request keys dari client ke server
     * @return map kunci request JSON yang telah diinisialisasi
     */
    private static HashMap<String, List<String>> initializedClientToServerRequestKeys() {
        HashMap<String, List<String>> requestKeys = new HashMap<>();
        List<String> keys;
        keys = Arrays.asList("method", "username", "udp_address", "udp_port");
        requestKeys.put("join", keys);

        keys = Collections.singletonList("method");
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
    private static HashMap<String, List<String>> initializedClientToClientRequestKeys() {
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

    private static HashMap<String, String> initializedCommands() {
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
        commands.put("accept_proposal_server", "Send leader information to server...");
        commands.put("vote_werewolf", "Who do you want to kill?");
        commands.put("vote_werewolf_not_success", "Vote werewolf is not success");
        commands.put("count_vote", "Counting vote from other players...");
        commands.put("vote_civilian", "Who do you want to kill?");
        commands.put("vote_civilian_not_success", "Vote civilian is not success");
        commands.put("start_game", "Game is starting...");
        commands.put("change_phase", "Phase change...");
        commands.put("game_over", "Game over! The winner is ");

        return commands;
    }

    private static HashMap<Integer, String> initializedAnswerChoices() {
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
        System.out.println("Client address: " + InetAddress.getLocalHost().getHostAddress());
        System.out.println("Client port: " + udpSocket.getLocalPort());

        // Inisialisasi game
        joinGame();
        readyUp();
        startGame();

        while (!isGameOver) {
            // Pemilihan leader
            askClientList();
            setIsProposer();
            isLeader = false;
            isPreparedProposer = false;
            promise[0] = 0;
            promise[1] = -1;
            if (isProposer) {
                prepareProposalToClient();
                if (isPreparedProposer) {
                    acceptProposalToClient();
                }
            } else {
                // Menerima proposal
                countReceiveProposal = 4;
                do {
                    DatagramPacket datagramPacket = receiveFromClient();
                    if (getData(datagramPacket).getString("method").equals("prepare_proposal")) {
                        receivedProposal(datagramPacket);
                    } else if (getData(datagramPacket).getString("method").equals("accept_proposal")) {
                        receiveAcceptProposal(datagramPacket);
                    }
                } while (countReceiveProposal > 0);
            }

            // Voting siapa yang akan dibunuh


        }

//        if (id == 0) {
//            System.out.println("Client address: " + InetAddress.getLocalHost().getHostAddress());
//            System.out.println("Client port: " + udpSocket.getLocalPort());
//            while (true) {
//                DatagramPacket packet = receiveFromClient();
//                getData(packet);
//            }
//        } else {
//            JSONObject data = new JSONObject();
//            System.out.print("Input client address: ");
//            InetAddress address = InetAddress.getByName(scan.nextLine());
//            System.out.print("Input client port: ");
//            int port = Integer.parseInt(scan.nextLine());
//            data.put("client", username);
//            data.put("message", "hello");
//            sendToClient(data, address, port);
//        }


//        udpSocket.close();
//        tcpOut.close();
//        tcpIn.close();
//        tcpSocket.close();
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
            System.out.println("Response from server: " + response.toString());
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
        String decision;
        do {
            System.out.println(commands.get("ready"));
            decision = scan.nextLine();

        } while (!decision.equals(answers.get(1)));

        JSONObject request = requestReadyUp();
        sendToServer(request);
        JSONObject response = receiveFromServer();
        System.out.println(response.getString("description"));
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
            String status = response.getString("status");
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
     * Menyimpan daftar client termasuk dirinya sendiri
     * @param clients array berisi response daftar client dari server
     * @throws JSONException
     */
    private void saveClientList(JSONArray clients) throws JSONException {
        for (int i=0; i<clients.length(); i++) {
            JSONObject client = clients.getJSONObject(i);
            int clientId = client.getInt("player_id");
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
            String role = "";
            if (client.has("role")) {
                role = client.getString("role");
            }

            // Memeriksa keberadaan client di clientsInfo
            if (clientsInfo.get(clientId) == null) {
                clientsInfo.put(clientId, new ClientInfo(isAlive, address, port, username, role));
            } else {
                if (clientsInfo.get(clientId).isAlive() != isAlive) {
                    clientsInfo.get(clientId).setAlive(isAlive);
                    clientsInfo.get(clientId).setRole(role);
                    System.out.println("Player " + clientsInfo.get(clientId).getUsername() + " is dead.");
                }
            }
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

    private void receivedProposal(DatagramPacket datagramPacket) throws IOException, JSONException {
        JSONObject request = getData(datagramPacket);
        JSONObject response = new JSONObject();
        ArrayList<String> keys = new ArrayList<>(Arrays.asList("method", "proposal_id"));
        if (isRequestKeyValid(keys, request)) {
            JSONArray proposalId = request.getJSONArray("proposal_id");
            if (promise[0] == 0) {
                response.put("status", "ok");
                response.put("description", "accepted");

                promise[0] = proposalId.getInt(0);
                promise[1] = proposalId.getInt(1);
            } else if (promise[0] < proposalId.getInt(0)) {
                response.put("status", "ok");
                response.put("description", "accepted");
                response.put("previous_accepted", leaderId);

                promise[0] = proposalId.getInt(0);
                promise[1] = proposalId.getInt(1);
            } else if (promise[0] > proposalId.getInt(0)){
                response.put("status", "fail");
                response.put("description", "rejected");

                countReceiveProposal--;
            } else if (promise[0] == proposalId.getInt(0)) {
                if (promise[1] < proposalId.getInt(1)) {
                    response.put("status", "ok");
                    response.put("description", "accepted");
                    response.put("previous_accepted", leaderId);

                    promise[1] = proposalId.getInt(1);
                } else if (promise[1] > proposalId.getInt(0)){
                    response.put("status", "fail");
                    response.put("description", "rejected");

                    countReceiveProposal--;
                }
            }
        } else {
            response = packResponse("error", request.getString("method"));
        }
        countReceiveProposal--;
        sendToClient(response, datagramPacket.getAddress(), datagramPacket.getPort());
    }

    private void receiveAcceptProposal(DatagramPacket datagramPacket) throws IOException, JSONException {
        JSONObject request = getData(datagramPacket);
        JSONObject response = new JSONObject();

        ArrayList<String> keys = new ArrayList<>(Arrays.asList("method", "proposal_id", "kpu_id"));
        if (isRequestKeyValid(keys, request)) {
            JSONArray proposalId = request.getJSONArray("proposal_id");
            if (promise[0] > proposalId.getInt(0)){
                response.put("status", "fail");
                response.put("description", "rejected");
            } else if (promise[0] == proposalId.getInt(0)) {
                if (promise[1] == proposalId.getInt(1)) {
                    response.put("status", "ok");
                    response.put("description", "accepted");

                    promise[1] = proposalId.getInt(1);
                } else if (promise[1] > proposalId.getInt(0)){
                    response.put("status", "fail");
                    response.put("description", "rejected");

                    countReceiveProposal--;
                }
            }
        } else {
            response = packResponse("error", request.getString("method"));
        }
        countReceiveProposal--;
        sendToClient(response, datagramPacket.getAddress(), datagramPacket.getPort());
    }

    private void setIsProposer() {
        ArrayList<Integer> proposerClientId = getProposer();
        for (int i=0; i<proposerClientId.size(); i++) {
            int clientId = proposerClientId.get(i);
            if (clientId == id) {
                isProposer = true;
            } else {
                isProposer = false;
            }
        }
    }

    /**
     * Mengirimkan proposal dari proposer ke acceptor
     * @throws JSONException
     * @throws IOException
     */
    private void prepareProposalToClient() throws JSONException, IOException {
        System.out.println(commands.get("prepare_proposal_client"));
        JSONObject request = requestPrepareProposalToClient();
        ArrayList<Integer> proposerClientId = getProposer();
        for (int i=0; i<proposerClientId.size(); i++) {
            int clientId = proposerClientId.get(i);
            if (clientId != id) {
                sendToClient(request, clientsInfo.get(clientId).getAddress(), clientsInfo.get(clientId).getPort());
            }
        }

        int receivedPacket = 0;
        int okProposal = 0;
        while (receivedPacket < getAliveClientsNum()-2) {
            // Menunggu paket sampai seluruh acceptor mengirim response
            DatagramPacket packet = receiveFromClient();
            JSONObject response = getData(packet);
            System.out.println("Response converted to JSON: " + response);

            String status = response.getString("status");
            String description = response.getString("description");
            if (!status.equals("error")) {
                System.out.println(getClientIdByAddress(packet.getAddress(), packet.getPort()) + ": " + description + " proposal.");
                if (status.equals("ok")) {
                    okProposal++;
                } else {
                    isPreparedProposer = false;
                }
            } else {
                isPreparedProposer = false;
                System.out.println(getClientIdByAddress(packet.getAddress(), packet.getPort()) + ": " + description);
            }
            if (okProposal == receivedPacket) {
                isLeader = true;
                System.out.println(commands.get("prepare_me_leader"));
            }
            // TODO: Previous accepted dipakai?
        }
    }

    private ArrayList<Integer> getProposer() {
        int firstClientId = -1;
        int secondClientId = -1;
        for (Integer key : clientsInfo.keySet()) {
            Integer clientId = key;
            if (clientId > firstClientId) {
                secondClientId = firstClientId;
                firstClientId = clientId;
            }
        }
        ArrayList<Integer> proposerClientId = new ArrayList<>();
        proposerClientId.add(firstClientId);
        proposerClientId.add(secondClientId);

        return proposerClientId;
    }

    /**
     * Mencari client dengan alamat dan port dari parameter pada list clientsInfo
     * @param address alamat yang ingin ditemukan
     * @param port port yang ingin ditemukan
     * @return ID client yang ditemukan, bernilai -1 jika tidak ditemukan
     */
    private int getClientIdByAddress(InetAddress address, int port) {
        Integer clientId = 0;
        boolean isFound = false;
        Iterator entries = clientsInfo.entrySet().iterator();
        while (entries.hasNext() && !isFound) {
            Map.Entry entry = (Map.Entry) entries.next();
            clientId = (Integer) entry.getKey();
            ClientInfo clientInfo = (ClientInfo) entry.getValue();
            if (clientInfo.getAddress().equals(address) && clientInfo.getPort()==port) {
                isFound = true;
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

    /**
     * Mengirim proposal untuk kedua kalinya ke acceptor
     * @throws JSONException
     * @throws IOException
     */
    private void acceptProposalToClient() throws JSONException, IOException {
        System.out.println(commands.get("accept_proposal_client"));
        JSONObject request = requestAcceptProposal();
        ArrayList<Integer> proposerClientId = getProposer();
        for (int i=0; i<proposerClientId.size(); i++) {
            int clientId = proposerClientId.get(i);
            if (clientId != id) {
                sendToClient(request, clientsInfo.get(clientId).getAddress(), clientsInfo.get(clientId).getPort());
            }
        }

        int receivedPacket = 0;
        int okProposal = 0;
        while (receivedPacket < getAliveClientsNum()-2) {
            // Menunggu paket sampai seluruh acceptor mengirim response
            DatagramPacket packet = receiveFromClient();
            JSONObject response = getData(packet);
            System.out.println("Response converted to JSON: " + response);

            String status = response.getString("status");
            String description = response.getString("description");
            if (!status.equals("error")) {
                System.out.println(getClientIdByAddress(packet.getAddress(), packet.getPort()) + ": " + description + " proposal.");
                if (status.equals("ok")) {
                    okProposal++;
                }
            } else {
                System.out.println(getClientIdByAddress(packet.getAddress(), packet.getPort()) + ": " + description);
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
        request.put("kpu_id", sequenceId); // TODO: kpu_id

        return request;
    }

    /**
     * Mengirimkan informasi leader dari seluruh client ke server (learner)
     * @throws JSONException
     * @throws IOException
     */
    private void acceptProposalToServer() throws JSONException, IOException {
        boolean isSuccess = true;
        do {
            System.out.println(commands.get("accept_proposal_server"));
            JSONObject request = requestAcceptProposalToServer();
            sendToServer(request);

            JSONObject response = receiveFromServer();
            String status =  response.getString("status");
            switch (status) {
                case "ok":
                    System.out.println(response.getString("description"));
                    break;
                default:
                    isSuccess = false;
                    break;
            }
        } while (!isSuccess);
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
     * Melakukan voting untuk membunuh werewolf pada malam hari
     * @throws JSONException
     * @throws IOException
     */
    private void voteWerewolf() throws JSONException, IOException {
        boolean isSuccess = false;
        do {
            System.out.println(commands.get("vote_werewolf"));
            String werewolfUsername = scan.nextLine();

            int clientId = getClientIdByUsername(werewolfUsername);
            JSONObject request = requestVoteWerewolf(clientId);
            sendToClient(request, clientsInfo.get(clientId).getAddress(), clientsInfo.get(clientId).getPort());

            JSONObject response = getData(receiveFromClient());
            System.out.println(response.getString("description"));
            String status =  response.getString("status");
            switch (status) {
                case "ok":
                    isSuccess = true;
                    break;
                default:
                    System.out.println(commands.get("vote_werewolf_not_success"));
                    break;
            }
        } while (!isSuccess);
    }

    /**
     * Mencari client dengan username dari parameter pada list clientsInfo
     * @param username username yang ingin dicari pada clientsInfo
     * @return ID client yang ditemukan, bernilai -1 jika tidak ditemukan
     */
    private int getClientIdByUsername(String username) {
        Integer clientId = 0;
        boolean isFound = false;
        Iterator entries = clientsInfo.entrySet().iterator();
        while (entries.hasNext() && !isFound) {
            Map.Entry entry = (Map.Entry) entries.next();
            clientId = (Integer) entry.getKey();
            ClientInfo clientInfo = (ClientInfo) entry.getValue();
            if (clientInfo.getUsername().equals(username)) {
                isFound = true;
            }
        }
        if (isFound) {
            return clientId;
        } else {
            return -1;
        }
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
     * Leader melakukan vote ke data pada dirinya sendiri, menunggu voting dari client lain,
     * menghitung vote dan mengirimkan hasil rekapitulasi vote ke server pada malam hari
     * @throws JSONException
     * @throws IOException
     */
    private void voteResultWerewolf() throws JSONException, IOException {
        boolean isSuccess = false;
        do {
            int selfIsWerewolf = 0;
            // Vote for itself if I am a werewolf
            if (role.equals("werewolf")) {
                System.out.println(commands.get("vote_werewolf"));
                String werewolfUsername = scan.nextLine();

                int clientIdVoted = getClientIdByUsername(werewolfUsername);
                int voteNum = clientsInfo.get(clientIdVoted).getVoteNum();
                clientsInfo.get(clientIdVoted).setVoteNum(voteNum);
                selfIsWerewolf = 1;
            }

            // Waiting others to vote
            int votedClientNum = 0;
            while (votedClientNum < werewolfNum-getDeadClientsNum("werewolf")-selfIsWerewolf) {
                JSONObject voteRequest = getData(receiveFromClient());
                handleClientVote(voteRequest);
                votedClientNum++;
            }

            // Count vote
            System.out.println(commands.get("count_vote"));
            JSONObject playerKilledRequest = requestVoteResultWerewolf();
            sendToServer(playerKilledRequest);
            JSONObject response = receiveFromServer();
            System.out.println(response.getString("description"));
            if (response.getString("ok").equals("ok") && getHighestVoteClientId()!=-1) {
                isSuccess = true;
            }
            emptyClientsInfoVoteNum();

            // TODO: vote_now
        } while (!isSuccess);
    }

    private void handleClientVote(JSONObject voteRequest) throws JSONException, IOException {
        int voteId = voteRequest.getInt("player_id");
        JSONObject response;

        ArrayList<String> keys = new ArrayList<>(Arrays.asList("method", "player_id"));
        if (isRequestKeyValid(keys, voteRequest)) {
            int voteNum = clientsInfo.get(voteId).getVoteNum();
            clientsInfo.get(voteId).setVoteNum(voteNum+1);

            response = packResponse("ok", "Kill werewolf");
        } else {
            response = packResponse("error", "Kill werewolf");
        }
        sendToClient(response, clientsInfo.get(voteId).getAddress(), clientsInfo.get(voteId).getPort());
    }

    private void emptyClientsInfoVoteNum() {
        for (ClientInfo clientInfo : clientsInfo.values()) {
            clientInfo.setVoteNum(0);
        }
    }

    /**
     * Menghitung jumlah client yang masih hidup pada seluruh client
     * @return jumlah client yang masih hidup
     */
    private int getAliveClientsNum() {
        int aliveClientsNum = 0;
        for (ClientInfo clientInfo : clientsInfo.values()) {
            if (clientInfo.isAlive()) {
                aliveClientsNum++;
            }
        }

        return aliveClientsNum;
    }

    /**
     * Menghitung jumlah client yang telah mati dengan role sesuai parameter
     * @param wantedRole role yang ingin dicari
     * @return jumlah client yang telah mati dengan role wantedRole
     */
    private int getDeadClientsNum(String wantedRole) {
        int deadClientsNum = 0;
        for (ClientInfo clientInfo : clientsInfo.values()) {
            if (!clientInfo.isAlive() && clientInfo.getRole().equals(wantedRole)) {
                deadClientsNum++;
            }
        }

        return deadClientsNum;
    }

    /**
     * Menyusun JSON untuk mengirim hasil vote membunuh werewolf dari leader ke server
     * @return objek JSON yang akan dikirim
     * @throws JSONException
     */
    private JSONObject requestVoteResultWerewolf() throws JSONException {
        JSONObject request = new JSONObject();
        int playerKilled = getHighestVoteClientId();
        if (playerKilled == -1) {
            // Tidak terdapat pemain yang dibunuh
            request.put("method", "vote_result");
            request.put("vote_status", -1);
        } else {
            request.put("method", "vote_result_werewolf");
            request.put("vote_status", 1);
            request.put("player_killed", playerKilled);
        }

        List<List<Integer>> voteArrays = new ArrayList<>();
        for (Map.Entry<Integer, ClientInfo> vote : clientsInfo.entrySet()) {
            Integer clientId = vote.getKey();
            Integer voteNum = vote.getValue().getVoteNum();

            List<Integer> voteArray = new ArrayList<>();
            voteArray.add(clientId);
            voteArray.add(voteNum);
            voteArrays.add(voteArray);
        }
        request.put("vote_result", voteArrays);

        return request;
    }

    /**
     * Mengembalikan ID client dengan vote tertinggi
     * @return ID client apabila terdapat vote tertinggi yang valid dan -1 apabila tidak dapat diambil keputusan
     */
    private int getHighestVoteClientId() {
        int highestVoteNum = -1;
        int sameHighestVoteNum = 0;
        int highestVoteId = -1;
        for (Map.Entry<Integer, ClientInfo> vote : clientsInfo.entrySet()) {
            Integer clientId = vote.getKey();
            Integer voteNum = vote.getValue().getVoteNum();
            if (highestVoteNum < voteNum) {
                highestVoteId = clientId;
                highestVoteNum = voteNum;
                sameHighestVoteNum = 0;
            } else if (highestVoteNum == voteNum) {
                sameHighestVoteNum++;
            }
        }
        if (sameHighestVoteNum > 0) {
            return -1;
        } else {
            return highestVoteId;
        }
    }

    /**
     * Melakukan voting untuk membunuh civilian
     * @throws JSONException
     * @throws IOException
     */
    private void voteCivilian() throws JSONException, IOException {
        int voteSequence = 0;
        boolean isSuccess = false;
        do {
            System.out.println(commands.get("vote_civilian"));
            String civilianUsername = scan.nextLine();

            int clientId = getClientIdByUsername(civilianUsername);
            JSONObject request = requestVoteCivilian(clientId);
            sendToClient(request, clientsInfo.get(clientId).getAddress(), clientsInfo.get(clientId).getPort());

            JSONObject response = receiveFromServer();
            System.out.println(response.getString("description"));
            String status =  response.getString("status");
            switch (status) {
                case "ok":
                    isSuccess = true;
                    break;
                default:
                    System.out.println(commands.get("vote_civilian_not_success"));
                    voteSequence++;
                    break;
            }
        } while (!isSuccess && voteSequence<2);
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

    private void voteResultCivilian() throws IOException, JSONException {
        int voteSequence = 0;
        boolean isSuccess = false;
        do {
            int selfIsCivilian = 0;
            // Vote for itself if I am a civilian
            if (role.equals("civilian")) {
                System.out.println(commands.get("vote_civilian"));
                String playerUsername = scan.nextLine();

                int clientIdVoted = getClientIdByUsername(playerUsername);
                int voteNum = clientsInfo.get(clientIdVoted).getVoteNum();
                clientsInfo.get(clientIdVoted).setVoteNum(voteNum);
                selfIsCivilian = 1;
            }

            // Waiting others to vote
            int votedClientNum = 0;
            while (votedClientNum < civilianNum-getDeadClientsNum("civilian")-selfIsCivilian) {
                JSONObject voteRequest = getData(receiveFromClient());
                handleClientVote(voteRequest);
                votedClientNum++;
            }

            // Count vote
            System.out.println(commands.get("count_vote"));
            JSONObject playerKilledRequest = requestVoteResultCivilian();
            sendToServer(playerKilledRequest);
            JSONObject response = receiveFromServer();
            System.out.println(response.getString("description"));
            if (response.getString("ok").equals("ok") && getHighestVoteClientId()!=-1) {
                isSuccess = true;
            } else {
                voteSequence++;
            }
            emptyClientsInfoVoteNum();

            // TODO: vote_now
        } while (!isSuccess && voteSequence<2);
    }

    /**
     * Menyusun JSON untuk mengirim hasil vote membunuh civilian dari leader ke server
     * @return objek JSON yang akan dikirim
     * @throws JSONException
     */
    private JSONObject requestVoteResultCivilian() throws JSONException {
        JSONObject request = new JSONObject();
        int playerKilled = getHighestVoteClientId();
        if (playerKilled == -1) {
            // Tidak terdapat pemain yang dibunuh
            request.put("method", "vote_result");
            request.put("vote_status", -1);
        } else {
            request.put("method", "vote_result_civilian");
            request.put("vote_status", 1);
            request.put("player_killed", playerKilled);
        }

        List<List<Integer>> voteArrays = new ArrayList<>();
        for (Map.Entry<Integer, ClientInfo> vote : clientsInfo.entrySet()) {
            Integer clientId = vote.getKey();
            Integer voteNum = vote.getValue().getVoteNum();

            List<Integer> voteArray = new ArrayList<>();
            voteArray.add(clientId);
            voteArray.add(voteNum);
            voteArrays.add(voteArray);
        }
        request.put("vote_result", voteArrays);

        return request;
    }

    private JSONObject packResponse(String status, String method) throws JSONException {
        JSONObject response = new JSONObject();
        response.put("status", status);
        switch (status) {
            case "error":
                response.put("description", method + " error");
                break;
            case "fail":
                response.put("description", method + " fail");
                break;
        }
        return response;
    }

    /**
     * Memeriksa apakah json request memiliki sintaks yang salah
     * @return bernilai true apabila request memiliki sintaks yang salah
     */
    private boolean isRequestKeyValid(ArrayList<String> keys, JSONObject request) {
        if (request.length() == keys.size()) {
            int i = 0;
            while (i < request.length()) {
                String key;
                key = keys.get(i);
                if (request.isNull(key)) {
                    return false;
                }
                i++;
            }
            return true;
        } else {
            return false;
        }
    }

    private void startGame() throws IOException, JSONException {
        JSONObject request = receiveFromServer();
        JSONObject response;
        switch (request.getString("role")) {
            case "civilian":
                role = "civilian";
                break;
            case "werewolf":
                role = "werewolf";
                break;
        }

        ArrayList<String> keys = new ArrayList<>(Arrays.asList("method", "time", "role", "friend", "description"));
        if (isRequestKeyValid(keys, request)) {
            response = packResponse("ok", request.getString("method"));

            System.out.print(commands.get("start_game"));
            System.out.println(request.getString("description"));

            if (request.getString("role").equals("werewolf")) {
                System.out.println("Your friends: ");
                JSONArray friends = request.getJSONArray("friend");
                for (int i = 0; i < friends.length(); i++) {
                    System.out.println(friends.getString(i));
                }
            }

            //TODO: check when start game is failed
        } else {
            response = packResponse("error", request.getString("method"));
        }
        sendToServer(response);
    }

    /**
     * Menerima request dari server ketika fase berganti
     * @throws IOException
     * @throws JSONException
     */
    private void changePhase() throws IOException, JSONException {
        JSONObject request = receiveFromServer();
        JSONObject response;
        ArrayList<String> keys = new ArrayList<>(Arrays.asList("method", "time", "days", "description"));

        if (isRequestKeyValid(keys, request)) {
            response = packResponse("ok", request.getString("method"));

            System.out.print(commands.get("change_phase"));
            System.out.println(request.getString("description"));

            //TODO: check when change phase is failed
        } else {
            response = packResponse("error", request.getString("method"));
        }
        sendToServer(response);
    }

    /**
     * Menerima request untuk melakukan vote saat ini dari server
     * @throws IOException
     * @throws JSONException
     */
    private void voteNow() throws IOException, JSONException {
        JSONObject request = receiveFromServer();
        JSONObject response;

        ArrayList<String> keys = new ArrayList<>(Arrays.asList("method", "phase"));
        if (isRequestKeyValid(keys, request)) {
            if (request.getString("method").equals("vote_now")) {
                response = packResponse("ok", "vote_now");
            } else {
                response = packResponse("fail", "vote_now");
            }
        } else {
            response = packResponse("error", "vote_now");
        }
        sendToServer(response);
    }

    /**
     * Menerima request game over dari server
     * @return true jika game over
     * @throws IOException
     * @throws JSONException
     */
    private void gameOver() throws IOException, JSONException {
        JSONObject request = receiveFromServer();
        JSONObject response;
        ArrayList<String> keys = new ArrayList<>(Arrays.asList("method", "winner", "description"));

        if (isRequestKeyValid(keys, request)) {
            response = packResponse("ok", request.getString("method"));

            System.out.println(request.getString("description"));
            System.out.print(commands.get("game_over"));
            System.out.println(request.getString("winner"));

            //TODO: check when game over is failed
        } else {
            response = packResponse("error", request.getString("method"));
        }
        sendToServer(response);
    }

    /**
     * Menerima request dari server bahwa leader saat ini telah terpilih dan menyimpan IDnya ke leaderId
     * @throws IOException
     * @throws JSONException
     */
    private void getLeaderSelected() throws IOException, JSONException {
        JSONObject request = receiveFromServer();
        JSONObject response;

        ArrayList<String> keys = new ArrayList<>(Arrays.asList("method", "kpu_id"));
        if (isRequestKeyValid(keys, request)) {
            int leaderIdByServer = request.getInt("kpu_id");
            if (request.getString("method").equals("kpu_selected") && clientsInfo.get(leaderIdByServer)!=null) {
                leaderId = leaderIdByServer;
                response = packResponse("ok", "kpu_selected");
            } else {
                response = packResponse("fail", "kpu_selected");
            }
        } else {
            response = packResponse("error", "kpu_selected");
        }
        sendToServer(response);
    }

    /**
     * Mendapatkan ID proposal client yang bersangkutan, terdiri dari sequence number dan ID client
     * @return ID proposal dalam JSONArray
     */
    private JSONArray getProposalId() {
        JSONArray proposalIdArray = new JSONArray();
        sequenceId += 1;
        proposalIdArray.put(sequenceId);
        proposalIdArray.put(id);

        return proposalIdArray;
    }

    public static void main(String[] args) throws IOException, JSONException {
        Scanner scan = new Scanner(System.in);

        String hostName = "10.5.24.104";
        int port = 8080;
//        int udpPort = 8000;

//        System.out.print("Input server IP host name: ");
//        String hostName = scan.nextLine();
//        System.out.print("Input server port: ");
//        int port = Integer.parseInt(scan.nextLine());
        System.out.print("Input UDP port: ");
        int udpPort = Integer.parseInt(scan.nextLine());
        System.out.println(InetAddress.getLocalHost().getHostAddress());



        System.out.println("Connecting to " + hostName + " on port " + port);
        Client client = new Client(hostName, port, udpPort);
        client.run();
    }
}
