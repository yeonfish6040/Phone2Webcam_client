package org.phone2webcam;

import org.phone2webcam.custom.CustomPacket;
import org.phone2webcam.custom.SequencedPacket;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class CommunicationManager {

    DatagramSocket dgramSocket;
    private InetAddress host;
    private String myip;
    private int port;

    public CommunicationManager(String myip, String host, int port) throws UnknownHostException, SocketException {
        this.myip = myip;
        this.port = port;
        this.dgramSocket = new DatagramSocket(this.port);
        this.host = InetAddress.getByName(host);
    }

    public String register() throws IOException {
        boolean isRegistered = false;
        CustomPacket dgramPacketRecv = null;
        sendPacket(this.host, ("P2WC-register_"+this.myip).getBytes(StandardCharsets.UTF_8));

        String data = "";
        try {
            dgramPacketRecv = receivePacket(1024, 500);
            data = (new String(dgramPacketRecv.data, StandardCharsets.UTF_8));
            if (data.contains("P2WC-registerResult_SUCCESS")) isRegistered = true; // P2WC-registerResult_SUCCESS|width|height
        } catch (SocketTimeoutException exception) {
        }


        if (isRegistered)
            System.out.println("\nConnected to "+dgramPacketRecv.packet.getAddress().toString());

        if (isRegistered)
            return dgramPacketRecv.packet.getAddress().toString();
        else {
            dgramSocket.close();

            return null;
        }
    }

    public void openStreaming(JLabel imgLabel) throws IOException {
        while (true) {
            try {
                CustomPacket dgramPacketRecv = receivePacket(1024, 1000);
                System.out.println(dgramPacketRecv.data.length);
                ByteArrayInputStream bis = new ByteArrayInputStream(dgramPacketRecv.data);
                BufferedImage image = ImageIO.read(bis); bis.close();
                image = resize(image, image.getWidth() / 5, image.getHeight() / 5);
                imgLabel.setIcon(new ImageIcon(image));
            }catch (Exception exception) {
            }
        }
    }

    private void sendPacket(InetAddress host, byte[] data) throws IOException {

        int chunkSize = 1024;
        int totalChunks = (int) Math.ceil((double) data.length / chunkSize);

        for (int i = 0; i < totalChunks; i++) {
            int offset = i * chunkSize;
            int length = Math.min(chunkSize, data.length - offset);

            byte[] chunk = new byte[length];
            System.arraycopy(data, offset, chunk, 0, length);

            byte[] sPacket = serialize(new SequencedPacket(i, chunk));
            this.dgramSocket.send(new DatagramPacket(sPacket, sPacket.length, host, this.port));
        }

        byte[] sPacket = serialize(new SequencedPacket(-1, null));
        this.dgramSocket.send(new DatagramPacket(sPacket, sPacket.length, host, this.port));
    }

    private CustomPacket receivePacket(int bytes, int timeout) throws IOException {
        byte[] buffer = new byte[bytes];
        DatagramPacket dgramPacketRecv = new DatagramPacket(buffer, buffer.length);

        int timeoutTmp = this.dgramSocket.getSoTimeout();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try {
            this.dgramSocket.setSoTimeout(timeout);

            List<byte[]> data = new ArrayList<>();
            while (true) {
                this.dgramSocket.receive(dgramPacketRecv);
                SequencedPacket sequencedPacket = deserialize(dgramPacketRecv.getData());

                if (sequencedPacket.getSequenceNumber() == -1) {
                    break;
                }

                data.set(sequencedPacket.getSequenceNumber(), sequencedPacket.getData());
            }

            data.forEach(e -> {
                byteArrayOutputStream.write(e, 0, e.length);
            });

            this.dgramSocket.setSoTimeout(timeoutTmp);
        } catch (Exception exception) {
            this.dgramSocket.setSoTimeout(timeoutTmp);
        }

        CustomPacket cPacket = new CustomPacket();
        cPacket.packet = dgramPacketRecv;
        cPacket.data = byteArrayOutputStream.toByteArray();
        byteArrayOutputStream.close();

        return cPacket;
    }

    private byte[] serialize(SequencedPacket sequencedPacket) throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream(); ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(sequencedPacket);
            return bos.toByteArray();
        }
    }

    private SequencedPacket deserialize(byte[] data) throws IOException, ClassNotFoundException {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(data);
             ObjectInputStream ois = new ObjectInputStream(bis)) {
            return (SequencedPacket) ois.readObject();
        }
    }

    private static BufferedImage resize(BufferedImage inputImage, int width, int height) throws IOException {
        BufferedImage outputImage = new BufferedImage(width, height, inputImage.getType());

        Graphics2D graphics2D = outputImage.createGraphics();
        graphics2D.drawImage(inputImage, 0, 0, width, height, null);
        graphics2D.dispose();

        return outputImage;
    }
}