package org.yeonfish.phone2webcam;

import org.yeonfish.phone2webcam.custom.CustomPacket;
import org.yeonfish.phone2webcam.custom.UDPTools;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

public class CommunicationManager {

    private UDPTools udpTools;
    private InetAddress host;
    private String myip;
    private int port;

    public CommunicationManager(String myip, String host, int port) throws UnknownHostException, SocketException {
        this.myip = myip;
        this.port = port;
        this.udpTools = new UDPTools(this.port);
        this.host = InetAddress.getByName(host);
    }

    public String register() throws IOException {
        boolean isRegistered = false;
        CustomPacket dgramPacketRecv = null;
        this.udpTools.sendPacket(this.host, this.port, ("P2WC-register_"+this.myip).getBytes(StandardCharsets.UTF_8));

        String data = "";
        try {
            dgramPacketRecv = this.udpTools.receivePacket(1024, 2000);
            data = new String(dgramPacketRecv.data, StandardCharsets.UTF_8);
            if (data.contains("P2WC-registerResult_SUCCESS")) isRegistered = true; // P2WC-registerResult_SUCCESS|width|height
        } catch (SocketTimeoutException exception) {
        }


        if (isRegistered)
            System.out.println("\nConnected to "+dgramPacketRecv.packet.getAddress().toString());

        if (isRegistered)
            return dgramPacketRecv.packet.getAddress().toString();
        else {
            this.udpTools.close();

            return null;
        }
    }

    public void openStreaming(JFrame frame, JLabel imgLabel) throws IOException {
        while (true) {
            try {
                this.udpTools.sendPacket(this.host, this.port, ("P2WC-request").getBytes(StandardCharsets.UTF_8));

                CustomPacket dgramPacketRecv = this.udpTools.receivePacket(1024, 1000);
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

    private static BufferedImage resize(BufferedImage inputImage, int width, int height) throws IOException {
        BufferedImage outputImage = new BufferedImage(width, height, inputImage.getType());

        Graphics2D graphics2D = outputImage.createGraphics();
        graphics2D.drawImage(inputImage, 0, 0, width, height, null);
        graphics2D.dispose();

        return outputImage;
    }
}