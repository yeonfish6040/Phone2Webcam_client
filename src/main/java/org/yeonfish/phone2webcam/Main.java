package org.yeonfish.phone2webcam;

import org.yeonfish.phone2webcam.util.Stopwatch;

import javax.swing.*;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Arrays;

public class Main {
    public static void main(String[] args) throws IOException, InterruptedException {
        // initialize
        CommunicationManager cManager = null;

        JFrame frame = new JFrame("Webcam");
        frame.setSize(400, 800);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JLabel label = new JLabel();
        frame.getContentPane().add(label);
        frame.pack();
        frame.setVisible(true);



        // find and return reachable hosts
        while (true) {
            Stopwatch sw = new Stopwatch();

            sw.Flag("Scan start");
            NetworkScanner scanner = new NetworkScanner(0, 255);
            String[] reachable = scanner.scan();
            sw.Flag("Scan finish");

            System.out.println("Reachable hosts: " + Arrays.toString(reachable));

            String myip = InetAddress.getLocalHost().toString().split("/")[1];
            System.out.println("myip: "+myip);

            sw.Flag("Start checking P2W server");

            int i = 0;
            for (String host : reachable) {
                System.out.write(("\rTaskLeft: "+String.valueOf((reachable.length)-i)).getBytes()); System.out.flush();

                int port = 19001;
                CommunicationManager cManagerTmp = new CommunicationManager(myip, host, port);
                String result = cManagerTmp.register();
                if (result != null) {
                    cManager = cManagerTmp;
                    break;
                }

                i++;
            }
            System.out.println();

            sw.Flag("End checking P2W server");

            sw.printProfile();

            if (cManager != null) break;

            System.out.println("\nCannot find Phone2Webcam server");

        }

        cManager.openStreaming(frame, label);
    }
}
