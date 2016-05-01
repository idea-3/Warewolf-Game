package com.idea.connection;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Random;

/**
 * Created by angelynz95 on 01-May-16.
 */
public class UnreliableSender {
    private DatagramSocket datagramSocket;
    private Random random;

    /**
     * Konstruktor
     * @param datagramSocket socket UDP
     * @throws SocketException
     */
    public UnreliableSender(DatagramSocket datagramSocket) throws SocketException {
        this.datagramSocket = datagramSocket;
        random = new Random();
    }

    /**
     * Mengirim paket UDP dengan persentase keberhasilan 85%
     * @param packet paket yang dikirim
     * @throws IOException
     */
    public void send(DatagramPacket packet) throws IOException {
        double rand = random.nextDouble();
        if (rand < 0.85) {
            datagramSocket.send(packet);
        }
    }
}
