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
    private double accuracy = 0.85;
    private double rand = 0;
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
     * Getter atribut accuracy
     * @return atribut accuracy
     */
    public double getAccuracy() {
        return accuracy;
    }

    /**
     * Mengirim paket UDP dengan persentase keberhasilan sebesar accuracy
     * @param packet paket yang dikirim
     * @throws IOException
     */
    public void send(DatagramPacket packet) throws IOException {
        double rand = random.nextDouble();
        this.rand = rand;
        if (rand < accuracy) {
            datagramSocket.send(packet);
        }
    }

    /**
     * Mengecek status keberhasilan pengiriman terakhir
     * @return true jika pengiriman terakhir berhasil dilakukan
     */
    public boolean isSent() {
        if (rand < accuracy) {
            return true;
        } else {
            return false;
        }
    }
}
