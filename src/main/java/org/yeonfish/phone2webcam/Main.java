package org.yeonfish.phone2webcam;

import org.yeonfish.phone2webcam.util.Stopwatch;
import org.yeonfish.phone2webcam.util.UDPTools;

import javax.swing.*;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class Main {
    public static void main(String[] args) throws IOException, InterruptedException {
        // initialize
        ArrayList<CommunicationManager> cManagers = new ArrayList<>();

        JFrame frame = new JFrame("Webcam");
        frame.setSize(400, 800);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JLabel label = new JLabel();
        frame.getContentPane().add(label);
        frame.pack();
        frame.setVisible(true);


        CommunicationManager cManager = new CommunicationManager(19001);
        // find and return reachable hosts
        while (true) {
            HashMap<String, String> result = cManager.register();
            if (result != null) break;
            System.out.println("Cannot find Phone2Webcam server");
            System.out.println();
        }

        cManager.openStreaming(frame, label);
    }
}
