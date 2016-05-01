package com.idea.gui;

import com.idea.connection.Client;
import com.idea.connection.ClientInfo;
import org.json.JSONException;
import org.json.JSONObject;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.DatagramPacket;
import java.util.ArrayList;
import java.util.Map;
import java.util.Random;

/**
 * Created by Devina Ekawati on 5/1/2016.
 */
public class GUIController {
    MainFrame frame;
    StartPanel startPanel;
    MainPanel mainPanel;
    Client client;
    VoteFrame voteFrame;

    String hostName;
    String username;
    int port;
    int udpPort;

    boolean isWaiting = false;
    boolean isSuccess;
    boolean voteNow;
    int voteSequence;

    JDialog waitingDialog;

    public GUIController() {

        waitingDialog = new JDialog(frame);
        mainPanel = new MainPanel();
        startPanel = new StartPanel();
        voteFrame = new VoteFrame();

        frame = MainFrame.getInstance();
        startGame();
        frame.pack();
        frame.setVisible(true);

    }

    public void startGame(){

        startPanel.loginButton.addActionListener(e -> {
            try {
//                hostName = startPanel.ipHostNameTextField.getText();
//                port = Integer.parseInt(startPanel.serverPortTextField.getText());
//                udpPort = Integer.parseInt(startPanel.udpPortTextField.getText());
//                username = startPanel.usernameTextField.getText();

                hostName = "DEVINA-PC";
                port = 2000;
                java.util.Random rand = new Random();
                udpPort = rand.nextInt((3100-3000) +1) +3000;
                username = String.valueOf(udpPort);

                client = new Client(hostName, port, udpPort);

                client.joinGame();
                showReadyOptionPane();
            } catch (IOException e1) {
                e1.printStackTrace();
            } catch (JSONException e1) {
                e1.printStackTrace();
            }
        });
        frame.setContentPane(startPanel);
    }

    public void showReadyOptionPane() {
        String message = "Are you ready?";
        String title = "Ready Confirmation";
        Object[] options = {"Yes", "No"};
        int answerId = JOptionPane.showOptionDialog(null, message, title, JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null, options, options[1]);
        if (options[answerId].equals("Yes")) {
            MainFrame frame = MainFrame.getInstance();
            frame.getContentPane().remove(startPanel);
            try {
                client.readyUp();

                frame.setContentPane(mainPanel);
                frame.validate();

                mainPanel.usernameLabel.setText("Hi, " + username);

                JPanel pnlDialog = new JPanel();
                waitingDialog.setBackground(new java.awt.Color(210, 204, 204));
                waitingDialog.setPreferredSize(new Dimension(250, 100));
                waitingDialog.setLocationRelativeTo(frame);

                JProgressBar bar = new JProgressBar();
                bar.setIndeterminate(true);
                bar.setStringPainted(true);
                bar.setString("Waiting for Other Players");

                pnlDialog.add(bar);

                waitingDialog.add(pnlDialog);
                waitingDialog.pack();
                waitingDialog.setVisible(true);

                BackgroundTask1 task = new BackgroundTask1();
                task.execute();

            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    public void playGame() throws IOException, JSONException, InterruptedException {
        Thread.sleep(1000);

        mainPanel.roleLabel.setText("Your role is " + client.role);
        mainPanel.narration.setText("The day has came. All villagers wake up.");
        ArrayList<JLabel> friendsArrayLabel = new ArrayList<>();
        ArrayList<JLabel> playersArrayLabel = new ArrayList<>();

        if (client.role.equals("werewolf")) {
            mainPanel.friendsPanel.setLayout(new GridLayout(client.friends.size()+1, 1));
            JLabel friendTitleLabel = new JLabel("Your friends: ");
            friendTitleLabel.setFont(new java.awt.Font("Tahoma", 0, 24));
            friendTitleLabel.setForeground(new java.awt.Color(75, 86, 106));
            friendsArrayLabel.add(friendTitleLabel);
            mainPanel.friendsPanel.add(friendTitleLabel);

            for (int i = 0; i < client.friends.size(); i++) {
                JLabel friendLabel = new JLabel(client.friends.get(i));
                friendLabel.setFont(new java.awt.Font("Tahoma", 0, 24));
                friendLabel.setForeground(new java.awt.Color(75, 86, 106));
                friendsArrayLabel.add(friendLabel);
                mainPanel.friendsPanel.add(friendLabel);
            }
        }
        client.askClientList();
        mainPanel.playersPanel.setLayout(new GridLayout(client.clientsInfo.size()+1, 1));
        JLabel playerTitleLabel = new JLabel("Your friends: ");
        playerTitleLabel.setFont(new java.awt.Font("Tahoma", 0, 24));
        playerTitleLabel.setForeground(new java.awt.Color(75, 86, 106));
        friendsArrayLabel.add(playerTitleLabel);
        mainPanel.playersPanel.add(playerTitleLabel);

        for (Map.Entry<Integer, ClientInfo> players : client.clientsInfo.entrySet()) {
            ClientInfo player = players.getValue();
            JLabel playerLabel = new JLabel(player.getUsername());
            playerLabel.setFont(new java.awt.Font("Tahoma", 0, 24));
            playerLabel.setForeground(new java.awt.Color(75, 86, 106));
            playersArrayLabel.add(playerLabel);
            mainPanel.playersPanel.add(playerLabel);
        }

        while (!client.isGameOver) {
            // Pemilihan leader
            client.askClientList();
            client.setIsProposer();
            client.isLeader = false;
            client.isPreparedProposer = false;
            client.promise[0] = 0;
            client.promise[1] = -1;
            if (client.isProposer) {
                // Mengirim proposal ke acceptor
                client.prepareProposalToClient();
                if (client.isPreparedProposer) {
                    Thread.sleep(1000);
                    client.acceptProposalToClient();
                }
            } else {
                // Menerima proposal dari proposer
                client.countReceiveProposal = 4;
                do {
                    DatagramPacket datagramPacket = client.receiveFromClient();
                    JSONObject data = client.getData(datagramPacket);
                    if (data.getString("method").equals("prepare_proposal")) {
                        client.receivedProposal(data, datagramPacket.getAddress(), datagramPacket.getPort());
                    } else if (data.getString("method").equals("accept_proposal")) {
                        client.receiveAcceptProposal(data, datagramPacket.getAddress(), datagramPacket.getPort());
                    }
                } while (client.countReceiveProposal > 0);

                // Mengirim proposal yang diaccept ke server
                client.acceptProposalToServer();
            }

            // Menerima informasi leader dari server
            client.getLeaderSelected();

            // Voting siapa yang akan dibunuh
            do {
                if (client.phase.equals("day")) {
                    if (client.isAlive) {
                        voteNow = false;
                        BackgroundTask2 task2 = new BackgroundTask2();
                        task2.execute();
                        Thread.sleep(1000);
                        client.askClientList();
                    }
                    Thread.sleep(1000);
                    if (client.isLeader) {
                        client.voteResultCivilian();
                    } else {
                        if (client.isAlive) {
                            voteFrame.setVisible(true);
                            isSuccess = false;
                            voteSequence = 0;
                            voteFrame.okButton.addActionListener(new ActionListener() {
                                @Override
                                public void actionPerformed(ActionEvent e) {
                                    try {
                                        isSuccess = client.voteCivilian(voteFrame.usernameTextField.getText());
                                        if (isSuccess && voteSequence >= 2) {
                                            voteFrame.dispose();
                                        } else {
                                            JOptionPane.showMessageDialog(voteFrame, "Vote failed", "Error", JOptionPane.ERROR_MESSAGE);
                                            voteSequence++;
                                        }
                                    } catch (JSONException e1) {
                                        e1.printStackTrace();
                                    } catch (IOException e1) {
                                        e1.printStackTrace();
                                    }
                                }
                            });

                        }
                    }
                } else {
                    if (client.isAlive && client.role.equals("werewolf")) {
                        voteNow = false;
                        BackgroundTask2 task2 = new BackgroundTask2();
                        task2.execute();
                        Thread.sleep(1000);
                        client.askClientList();
                    } else {
                        System.out.println(client.commands.get("civilian_wait"));
                    }
                    Thread.sleep(1000);
                    if (client.isLeader) {
                        client.voteResultWerewolf();
                    } else {
                        if (client.isAlive && client.role.equals("werewolf")) {
                            voteFrame.setVisible(true);
                            isSuccess = false;
                            voteFrame.okButton.addActionListener(new ActionListener() {
                                @Override
                                public void actionPerformed(ActionEvent e) {
                                    try {
                                        isSuccess = client.voteWerewolf(voteFrame.usernameTextField.getText());
                                        if (isSuccess) {
                                            voteFrame.dispose();
                                        } else {
                                            JOptionPane.showMessageDialog(voteFrame, "Vote failed", "Error", JOptionPane.ERROR_MESSAGE);
                                        }
                                    } catch (JSONException e1) {
                                        e1.printStackTrace();
                                    } catch (IOException e1) {
                                        e1.printStackTrace();
                                    }
                                }
                            });
                        }
                    }
                }

                if (client.role.equals("werewolf")) {
                    int j = 0;
                    for (int i = 0; i < friendsArrayLabel.size(); i++) {
                        if (friendsArrayLabel.get(i).getText().equals(client.deadPlayer.get(j))) {
                            friendsArrayLabel.get(i).setForeground(Color.red);
                            j++;
                        }
                    }
                }

                int j = 0;
                for (int i = 0; i < playersArrayLabel.size(); i++) {
                    if (playersArrayLabel.get(i).getText().equals(client.deadPlayer.get(j))) {
                        playersArrayLabel.get(i).setForeground(Color.red);
                        j++;
                    }
                }

                if (client.phase.equals("day")) {
                    Thread.sleep(1000);
                    client.askClientList();
                    Thread.sleep(1000);
                }
                JSONObject request = client.receiveFromServer();
                switch (request.getString("method")) {
                    case "change_phase":
                        client.changePhase(request);
                        if (client.phase.equals("day")) {
                            Thread.sleep(1000);
                            client.askClientList();
                        }
                        mainPanel.narration.setText(client.narration);
                        break;
                    case "game_over":
                        client.gameOver(request);
                        mainPanel.narration.setText("Game over! The winner is " + client.winner);
                        break;
                }
            } while (client.phase.equals("night") && !client.isGameOver);
        }
    }

    private class BackgroundTask1 extends SwingWorker<Void, Void> {

        @Override
        protected Void doInBackground() throws Exception {
            client.startGame();
            return null;
        }

        @Override
        public void done() {
            isWaiting = false;
            waitingDialog.setVisible(false);
            waitingDialog.dispose();
            try {
                playGame();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }

    private class BackgroundTask2 extends SwingWorker<Void, Void> {

        @Override
        protected Void doInBackground() throws Exception {
            client.voteNow();
            return null;
        }

        @Override
        public void done() {
            voteNow = true;
        }

    }

    public static void main(String[] args) {
        new GUIController();
    }
}
