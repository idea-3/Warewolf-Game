package com.idea.connection;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.*;

/**
 * Created by angelynz95 on 25-Apr-16.
 */
public class Server {
    private ArrayList<ClientController> clients;
    private ServerSocket serverSocket;
    private int leaderId = -1;
    private int nextClientId = 0;
    public static final HashMap<String, List<String>> okResponseKeys = initializeOkResponseKeys();
    public static final List<String> failResponseKeys = Arrays.asList("status", "description");
    public static final List<String> errorResponseKeys = Arrays.asList("status", "description");
    public static final String methodKey = "method";
    public static final HashMap<String, String> statusResponseValues = initializeStatusResponseValues();
    public static final HashMap<String, String> descriptionResponseValues = initializeDescriptionResponseValues();

    /**
     * Konstruktor
     * @param port port server
     */
    public Server(int port) throws IOException {
        clients = new ArrayList<>();
        serverSocket = new ServerSocket(port);
    }

    /**
     * Menginisialisasi OK response keys dari server ke client
     * @return map kunci response JSON yang telah diinisialisasi
     */
    private static HashMap initializeOkResponseKeys() {
        HashMap<String, List<String>> okResponseKeys = new HashMap<>();
        List<String> keys;
        keys = Arrays.asList("status", "player_id");
        okResponseKeys.put("join", keys);

        keys = Arrays.asList("status");
        okResponseKeys.put("leave", keys);
        okResponseKeys.put("start", keys);
        okResponseKeys.put("change_phase", keys);
        okResponseKeys.put("game_over", keys);

        keys = Arrays.asList("status", "description");
        okResponseKeys.put("ready", keys);
        okResponseKeys.put("accepted_proposal", keys);
        okResponseKeys.put("vote_result_werewolf", keys);
        okResponseKeys.put("vote_result_civilian", keys);
        okResponseKeys.put("vote_result", keys);

        keys = Arrays.asList("status", "clients", "description");
        okResponseKeys.put("client_address", keys);

        return okResponseKeys;
    }

    /**
     * Menginisialisasi nilai status response dari server
     * @return map nilai status yang telah diinisialisasi
     */
    private static HashMap<String, String> initializeStatusResponseValues() {
        HashMap<String, String> statusResponseValues = new HashMap<>();
        statusResponseValues.put("ok", "ok");
        statusResponseValues.put("fail", "fail");
        statusResponseValues.put("error", "error");

        return statusResponseValues;
    }

    /**
     * Menginisialisasi nilai deskripsi response dari server
     * @return map nilai deskripsi response dari server yang telah diinisialisasi
     */
    private static HashMap initializeDescriptionResponseValues() {
        HashMap<String, String> descriptionResponseValues = new HashMap<>();
        descriptionResponseValues.put("join fail user exists", "user exists");
        descriptionResponseValues.put("join fail game running", "“please wait, game is currently running");
        descriptionResponseValues.put("join error", "wrong request");
        descriptionResponseValues.put("leave fail", "you can not leave the game right now");
        descriptionResponseValues.put("leave error", "wrong request");
        descriptionResponseValues.put("ready ok", "waiting for other player to start");
        descriptionResponseValues.put("ready fail", "ready up fail");
        descriptionResponseValues.put("ready error", "ready up error");
        descriptionResponseValues.put("client_address fail", "client list can not be received");
        descriptionResponseValues.put("client_address error", "client list error");
        descriptionResponseValues.put("accepted_proposal ok", "");
        descriptionResponseValues.put("accepted_proposal fail", "");
        descriptionResponseValues.put("accepted_proposal error", "");
        descriptionResponseValues.put("vote_result_werewolf ok", "");
        descriptionResponseValues.put("vote_result ok", "");
        descriptionResponseValues.put("vote_result_werewolf fail", "");
        descriptionResponseValues.put("vote_result fail", "");
        descriptionResponseValues.put("vote_result_werewolf error", "");
        descriptionResponseValues.put("vote_result error", "");
        descriptionResponseValues.put("vote_civilian ok", "");
        descriptionResponseValues.put("vote_civilian fail", "");
        descriptionResponseValues.put("vote_civilian error", "");
        descriptionResponseValues.put("vote_result_civilian ok", "");
        descriptionResponseValues.put("vote_result_werewolf fail", "");
        descriptionResponseValues.put("vote_result_werewolf error", "");
        descriptionResponseValues.put("start fail", "");
        descriptionResponseValues.put("start error", "");
        descriptionResponseValues.put("change_phase ok", "change phase is success");
        descriptionResponseValues.put("change_phase fail", "");
        descriptionResponseValues.put("change_phase error", "");
        descriptionResponseValues.put("game_over fail", "");
        descriptionResponseValues.put("game_over error", "");

        return descriptionResponseValues;
    }

    /**
     * Menentukan role dari setiap client, apakah warga biasa atau werewolf
     */
    private void decideRole() {
        int roles[] = new int[clients.size()];
        int playerId;

        Random rand = new Random();
        double n = 0.5 * (clients.size() - 1);
        int countWerewolf = rand.nextInt(2) + (int) n;
        for (int i = 0; i < countWerewolf; i++) {
            do {
                playerId = rand.nextInt(0) +  (clients.size() - 1);

            } while(roles[playerId] == 1);

            roles[playerId] = 1;
        }

        for (int i = 0; i < roles.length; i++) {
            String role;
            if (roles[i] == 1) {
                role = "werewolf";
            } else {
                role = "civilian";
            }
            clients.get(i).setRole(role);
        }
    }

    /**
     * Mengembalikan true jika semua client telah mengirimkan request ready
     */
    public boolean isAllReady() {
        boolean allReady = true;
        int i = 0;
        while ((allReady) && (i < clients.size())) {
            if (!clients.get(i).isReady) {
                allReady = false;
            } else {
                i++;
            }
        }
        return allReady;
    }

    /**
     * Memeriksa apakah username telah ada atau tidak
     * @return true jika username telah ada
     */
    private boolean isUsernameExist(String username) {
        boolean found = false;
        int i = 0;
        while ((!found) && (i < clients.size())) {
            if (clients.get(i).username.equals(username)) {
                found = true;
            } else {
                i++;
            }
        }
        return found;
    }

    /**
     * Controller untuk setiap client yang terhubung dengan server
     */
    private class ClientController extends Thread {
        private int clientId;
        private BufferedReader in;
        private PrintWriter out;
        private Socket clientSocket;
        private String username;
        private boolean isAlive;
        private boolean isReady = false;
        private String role;
        private int countDay = 0;

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

        public void setRole(String role) {
            this.role = role;
        }

        /**
         * Mengirim pesan ke server
         * @param response pesan yang akan dikirim
         */
        public void sendToClient(JSONObject response) {
            System.out.println("Response to client: " + response);
            out.println(response.toString());
            out.flush();
        }

        /**
         * Menerima pesan dari client
         * @return pesan yang diterima dari client
         * @throws IOException
         * @throws JSONException
         */
        public JSONObject receiveFromClient() throws IOException, JSONException {
            JSONObject request;
            String clientString;
            StringBuilder stringBuilder = new StringBuilder();

            clientString = in.readLine();
            stringBuilder.append(clientString);
            request = new JSONObject(stringBuilder.toString());
            System.out.println("Receive from server: " + request);

            return request;
        }

        /**
         * Menjalankan controller client
         */
        public void run() {
            JSONObject request, response;
                try {
                    while (!isReady) {
                        request = receiveFromClient();
                        response = getResponse(request);
                        sendToClient(response);
                    }

                    while (!isAllReady()) {
                        // Menunggu sampai semua client ready
                    }

                    startGame();

                    String narration = "The day has came. All villagers wake up.";
                    changePhase("day", narration);

                    acceptLeader();
                    if (leaderId == clientId) {
                        processWerewolfKilledVote();
                    } else {

                    }

                } catch (SocketException e) {
                    e.printStackTrace();
                    clients.remove(this);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
//                    try {
//                        in.close();
//                        out.close();
//                        clientSocket.close();
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
        }

        private void startGame() throws JSONException, IOException {
            JSONObject request = new JSONObject();
            JSONObject response;
            ArrayList<String> friends = new ArrayList<>();

            request.put("method", "start");
            request.put("time", "night");
            request.put("role", role);
            request.put("friend", friends);
            request.put("description", "game is started");

            do {
                sendToClient(request);
                response = receiveFromClient();

                switch (response.getString("status")) {
                    case "fail":
                        System.out.println("Failed to start game: " + response.getString("description"));
                        break;
                    case "error":
                        System.out.println("Error when starting game: " + response.getString("description"));
                        break;
                }
            } while (!response.getString("status").equals("ok"));

        }

        private void changePhase(String time, String narration) throws JSONException, IOException {
            JSONObject request = new JSONObject();
            JSONObject response;

            request.put("method", "change_phase");
            request.put("time", time);
            request.put("days", countDay);
            request.put("description", narration);

            if (time.equals("day")) {
                countDay++;
            }

            do {
                sendToClient(request);
                response = receiveFromClient();

                switch (response.getString("status")) {
                    case "fail":
                        System.out.println("Failed to change phase: " + response.getString("description"));
                        break;
                    case "error":
                        System.out.println("Error when changing phase: " + response.getString("description"));
                        break;
                }
            } while (!response.getString("status").equals("ok"));
        }

        private void acceptLeader() throws IOException, JSONException {
            JSONObject kpuInfo = receiveFromClient();
            JSONObject response = getResponse(kpuInfo);
            sendToClient(response);
        }

        /**
         * Menangani proses penerimaan voting werewolf terbunuh
         * @throws IOException
         * @throws JSONException
         */
        private void processWerewolfKilledVote() throws IOException, JSONException {
            JSONObject voteResult;
            JSONObject response;
            int voteStatus = -1;

            do {
                voteResult = receiveFromClient();
                response =  getResponse(voteResult);
                if (response.getString("status").equals("ok")) {
                    voteStatus = 1;
                }
                sendToClient(response);
            } while (voteStatus != 1);
        }

        /**
         * Menentukan response server berdasarkan request client
         * @param request request client
         */
        private JSONObject getResponse(JSONObject request) throws JSONException {
            JSONObject response;
            String method = request.getString(methodKey);
            String status = getStatus(request);
            response = packResponse(method, status);

            return response;
        }

        /**
         * Membuat response yang sesuai dengan method dan status
         * @param method method dari response yang akan dikirim
         * @param status status dari response yang akan dikirim
         * @return
         * @throws JSONException
         */
        private JSONObject packResponse(String method, String status) throws JSONException {
            JSONObject response = new JSONObject();

            int responseLength;
            if (status.equals(statusResponseValues.get("ok"))) {
                responseLength = okResponseKeys.get(method).size();
                for (int i=0; i<responseLength; i++) {
                    String key = okResponseKeys.get(method).get(i);
                    switch (key) {
                        case "status":
                            response.put(key, status);
                            break;
                        case "description":
                            response.put(key, getDescription(method, status));
                            break;
                        case "player_id":
                            response.put(key, clientId);
                            break;
                        case "clients":
                            response.put(key, getClientsInfo());
                            break;
                    }
                }
            } else {
                if (status.equals(statusResponseValues.get("fail"))) {
                    responseLength = failResponseKeys.size();
                } else {
                    responseLength = errorResponseKeys.size();
                }
                for (int i=0; i<responseLength; i++) {
                    String key = failResponseKeys.get(i);
                    switch(key) {
                        case "status":
                            response.put(key, status);
                            break;
                        case "description":
                            response.put(key, getDescription(method, status));
                            break;
                    }
                }
            }

            return response;
        }

        /**
         * Memeriksa apakah request valid
         * @return bernilai true apabila request valid
         */
        private boolean isRequestValid(JSONObject request) {
            try {
                if (isRequestKeyValid(request)) {
                    return true;
                } else {
                    return false;
                }
            } catch (JSONException e) {
                e.printStackTrace();
                return false;
            }
        }

        /**
         * Memeriksa apakah json request memiliki sintaks yang salah
         * @return bernilai true apabila request memiliki sintaks yang salah
         */
        private boolean isRequestKeyValid(JSONObject request) throws JSONException {
            String method = request.getString(methodKey);
            List<String> validKeys = Client.clientToServerRequestKeys.get(method);

            if (request.length() == validKeys.size()) {
                int i = 0;
                while (i < request.length()) {
                    String validKey = validKeys.get(i);
                    if (request.isNull(validKey)) {
                        return false;
                    }
                }
                return true;
            } else {
                return false;
            }
        }

        /**
         * Mengembalikan status berdasarkan request
         * @param request
         * @return status
         */
        private String getStatus(JSONObject request) throws JSONException {
            String status = "";
            if (isRequestValid(request)) {
                String method = request.getString(methodKey);
                switch (method) {
                    case "join":
                        if (isUsernameExist(username)) {
                            status = "fail";
                        } else {
                            status =  "ok";
                            username = request.getString("username");
                        }
                        break;
                    case "ready":
                        status = "ok";
                        isReady = true;
                        break;
                    case "accepted_proposal":
                        status = "ok";
                        leaderId = request.getInt("kpu_id");
                        break;
                    case "vote_werewolf":
                        int voteStatus = request.getInt("vote_status");
                        if (voteStatus == 1) {
                            status = "ok";
                        } else {
                            status = "fail";
                        }
                        break;
                }
            } else {
                status = "error";
            }
            return status;
        }

        private String getDescription(String method, String status) {
            return descriptionResponseValues.get(method + " " + status);
        }

        private JSONObject getClientInfo() throws JSONException {
            JSONObject clientInfo = new JSONObject();
            clientInfo.put("player_id", clientId);
            clientInfo.put("is_alive", isAlive);
            clientInfo.put("address", clientSocket.getInetAddress().getHostAddress());
            clientInfo.put("port", clientSocket.getPort());
            clientInfo.put("username", username);

            return clientInfo;
        }
    }

    public JSONArray getClientsInfo() throws JSONException {
        JSONArray clientsInfo = new JSONArray();
        for (int i=0; i<clients.size(); i++) {
            clientsInfo.put(clients.get(i).getClientInfo());
        }

        return clientsInfo;
    }

    /**
     * Memanggil ClientController
     */
    public void controlClient(Socket clientSocket) throws IOException {
        int clientId = nextClientId;
        nextClientId++;
        ClientController clientController = new ClientController(clientSocket, clientId);
        clients.add(clientController);
        Thread thread = new Thread(clientController);
        thread.start();
    }

    /**
     * Menjalankan server
     */
    public void run() throws IOException {
        InetAddress ip = InetAddress.getLocalHost();
        String hostName = ip.getHostName();

        System.out.println("Server address: " + ip.getHostAddress());
        System.out.println("Server host name : " + hostName);
        while (true) {
            Socket clientSocket = serverSocket.accept();
            System.out.println("Just connected to " + clientSocket.getRemoteSocketAddress());
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
