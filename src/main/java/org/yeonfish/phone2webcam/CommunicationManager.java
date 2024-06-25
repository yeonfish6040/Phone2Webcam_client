package org.yeonfish.phone2webcam;

import org.yeonfish.phone2webcam.custom.CustomPacket;
import org.yeonfish.phone2webcam.util.UDPTools;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class CommunicationManager {

    private HashMap<String, String> clients = new HashMap<>();
    private UDPTools udpTools;
    private InetAddress host = null;
    private int port;
    private int frameCount;
    private long time = 0L;

    public CommunicationManager(int port) throws UnknownHostException, SocketException {
        this.port = port;
        this.udpTools = new UDPTools(this.port);
//        this.host = InetAddress.getByName(host);
    }

    public HashMap<String, String> register() throws IOException {
        boolean isRegistered = false;
        UDPTools.broadcast("P2WC-register", this.port+1);

        boolean timeout = false;
        String data = "";
        List<String> hosts = new ArrayList<>();
        CustomPacket dgramPacketRecv = null;
        while (!timeout) {
            try {
                dgramPacketRecv = this.udpTools.receivePacket(1024, 1000);
                data = new String(dgramPacketRecv.data, StandardCharsets.UTF_8);
                if (data.contains("P2WC-registerResult_SUCCESS")) {
                    isRegistered = true; // P2WC-registerResult_SUCCESS|width|height
                    clients.put(dgramPacketRecv.packet.getAddress().toString(), data);
                    System.out.println("\nConnected to "+dgramPacketRecv.packet.getAddress().toString());
                }
            } catch (SocketTimeoutException exception) {
                timeout = true;
            }
        }


        if (isRegistered)
            return clients;
        else {
            return null;
        }
    }

    public void openStreaming(JFrame frame, JLabel imgLabel) throws IOException {
        if (this.host == null)
            this.host = InetAddress.getByName(clients.keySet().iterator().next().replace("/", ""));
        while (true) {
            try {
                if (time == 0L) {
                    time = System.currentTimeMillis();
                }else if (System.currentTimeMillis() - time >= 1000) {
                    time = System.currentTimeMillis();
                    System.out.write(("\rFps: "+String.valueOf(this.frameCount)).getBytes()); System.out.flush();
                    this.frameCount = 0;
                }

                this.udpTools.sendPacket(this.host, this.port, ("P2WC-request").getBytes(StandardCharsets.UTF_8));

                CustomPacket dgramPacketRecv = this.udpTools.receivePacket(1024, 1000);

                ByteArrayInputStream bis = new ByteArrayInputStream(dgramPacketRecv.data);
                BufferedImage image = ImageIO.read(bis); bis.close();

                if (image == null) continue;

                image = resize(image, image.getWidth() / 2, image.getHeight() / 2);
                imgLabel.setIcon(new ImageIcon(image));

                frame.pack();
                this.frameCount++;
            }catch (Exception exception) {
                exception.printStackTrace();
            }
        }
    }

    public void close() {
        this.udpTools.close();
    }

    private static BufferedImage resize(BufferedImage inputImage, int width, int height) throws IOException {
        BufferedImage outputImage = new BufferedImage(width, height, inputImage.getType());

        Graphics2D graphics2D = outputImage.createGraphics();
        graphics2D.drawImage(inputImage, 0, 0, width, height, null);
        graphics2D.dispose();

        return outputImage;
    }
}