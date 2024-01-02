package org.phone2webcam;

import org.phone2webcam.custom.CustomObjectInputStream;
import org.phone2webcam.custom.CustomPacket;
import org.phone2webcam.custom.SequencedPacket;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
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
            dgramPacketRecv = receivePacket(1024, 2000);
            data = new String(dgramPacketRecv.data, StandardCharsets.UTF_8);
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

    public void openStreaming(JFrame frame, JLabel imgLabel) throws IOException {
        while (true) {
            try {
                sendPacket(this.host, ("P2WC-request").getBytes(StandardCharsets.UTF_8));

                CustomPacket dgramPacketRecv = receivePacket(1024, 1000);
                System.out.println(dgramPacketRecv.data.length);

                ByteArrayInputStream bis = new ByteArrayInputStream(dgramPacketRecv.data);
                BufferedImage image = ImageIO.read(bis); bis.close();

                image = resize(image, image.getWidth() / 2, image.getHeight() / 2);
                imgLabel.setIcon(new ImageIcon(image));

                frame.pack();
            }catch (Exception exception) {
                exception.printStackTrace();
            }
        }
    }

    private void sendPacket(InetAddress host, byte[] data) throws IOException {

        int chunkSize = 1000;
        int totalChunks = ((int) Math.ceil((double) data.length / chunkSize));

        for (int i = 0; i < totalChunks; i++) {
            int offset = i * chunkSize;
            int length = Math.min(chunkSize, data.length - offset);

            byte[] chunk = new byte[length];
            System.arraycopy(data, offset, chunk, 0, length);

            byte[] sPacket = new SequencedPacket(i, chunk).toSequencedPacketData();
            this.dgramSocket.send(new DatagramPacket(sPacket, sPacket.length, host, this.port));
        }

        byte[] sPacket = new SequencedPacket((totalChunks+1)*-1, null).toSequencedPacketData();
        this.dgramSocket.send(new DatagramPacket(sPacket, sPacket.length, host, this.port));
    }

    private CustomPacket receivePacket(int bytes, int timeout) throws IOException {
        byte[] buffer = new byte[bytes];
        DatagramPacket dgramPacketRecv = new DatagramPacket(buffer, buffer.length);

        int timeoutTmp = this.dgramSocket.getSoTimeout();
        int packetCount = -1;
        int currentPacketCount = 0;
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try {
            this.dgramSocket.setSoTimeout(timeout);

            Map<Integer, byte[]> data = new HashMap<>();
            while (true) {
                this.dgramSocket.receive(dgramPacketRecv);
                byte[] receivedData = new byte[dgramPacketRecv.getLength()];
                System.arraycopy(dgramPacketRecv.getData(), 0, receivedData, 0, dgramPacketRecv.getLength());
                currentPacketCount++;

                SequencedPacket sequencedPacket = new SequencedPacket(receivedData);

                if (sequencedPacket.getSequenceNumber() < 0) {
                    packetCount = sequencedPacket.getSequenceNumber()*-1;
                }else
                    data.put(sequencedPacket.getSequenceNumber(), sequencedPacket.getData());

                if (packetCount == currentPacketCount) break;
            }

            List<Integer> keySet = new ArrayList<>(data.keySet());
            Collections.sort(keySet);

            for (Integer i:keySet) {
                byteArrayOutputStream.write(data.get(i), 0, data.get(i).length);
            }

            this.dgramSocket.setSoTimeout(timeoutTmp);
        } catch (SocketTimeoutException exception) {
            this.dgramSocket.setSoTimeout(timeoutTmp);
        } catch (Exception exception) {
            exception.printStackTrace();
        }

        CustomPacket cPacket = new CustomPacket();
        cPacket.packet = dgramPacketRecv;
        cPacket.data = byteArrayOutputStream.toByteArray();
        byteArrayOutputStream.close();

        return cPacket;
    }

    private static BufferedImage resize(BufferedImage inputImage, int width, int height) throws IOException {
        BufferedImage outputImage = new BufferedImage(width, height, inputImage.getType());

        Graphics2D graphics2D = outputImage.createGraphics();
        graphics2D.drawImage(inputImage, 0, 0, width, height, null);
        graphics2D.dispose();

        return outputImage;
    }
}