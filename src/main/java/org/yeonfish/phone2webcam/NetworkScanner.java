package org.yeonfish.phone2webcam;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NetworkScanner {
    int scan_start;
    int scan_end;

    public List<String> reachableHosts;
    public List<Integer> taskCompleted;

    public NetworkScanner() {
        this.scan_start = 0;
        this.scan_end = 0;
    }

    public String[] scan() throws IOException, InterruptedException {
        String baseIp = getBaseIpAddress();

        System.out.println(baseIp);

        if (baseIp != null) {
            return scanDevicesOnNetwork(baseIp);
        } else {
            throw new RuntimeException("Unable to determine base IP address.");
        }
    }

    private String[] scanDevicesOnNetwork(String baseIp) throws IOException, InterruptedException {
        int increaseAmount = 0;
        int increaseBitLength = 32-baseIp.length();
        StringBuilder increaseBit = new StringBuilder();
        increaseBit.append("1".repeat(increaseBitLength));
        increaseAmount = Integer.parseInt(increaseBit.toString(), 2);
        this.scan_end = increaseAmount;

        this.reachableHosts = new ArrayList<>();
        this.taskCompleted = new ArrayList<>();

        for (int i = 0; i <= increaseAmount; i++) {
            StringBuffer host = new StringBuffer(baseIp);
            host.append("0".repeat(increaseBitLength-Integer.toBinaryString(i).length()));
            host.append(Integer.toBinaryString(i));
            (new Thread(new HostReachableTest(host.toString(), this.reachableHosts, this.taskCompleted))).start();
        }

        long startTime = System.currentTimeMillis();
        while(!(((this.scan_end-this.scan_start)-taskCompleted.size()) == 0)) {
            System.out.write(("\rTaskLeft: "+String.valueOf((this.scan_end-this.scan_start)-taskCompleted.size())).getBytes());
            System.out.flush();

            if (System.currentTimeMillis()-startTime > 10000) break;

            Thread.sleep(100);
        }
        System.out.println();

        String[] reachableResult = new String[reachableHosts.size()];
        reachableResult = this.reachableHosts.toArray(reachableResult);
        Arrays.sort(reachableResult);
        return reachableResult;
    }

    public static String getBaseIpAddress() throws IOException {
        Matcher match;
        while (true) {
            Process traceRt;
            if (System.getProperty("os.name").toLowerCase().contains("win"))
                traceRt = Runtime.getRuntime().exec("tracert 8.8.8.8");
            else traceRt = Runtime.getRuntime().exec("traceroute 8.8.8.8");
            BufferedReader br = new BufferedReader(new InputStreamReader(traceRt.getInputStream()));

            String ips = br.readLine();

            match = Pattern.compile("(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)", Pattern.MULTILINE).matcher(ips);
            if (match.find()) break;
        }
        String myip = match.group();
        NetworkInterface networkInterface = null;
        Enumeration Interfaces = NetworkInterface.getNetworkInterfaces();
        while(Interfaces.hasMoreElements() && networkInterface == null) {
            NetworkInterface Interface = (NetworkInterface)Interfaces.nextElement();
            Enumeration Addresses = Interface.getInetAddresses();
            while(Addresses.hasMoreElements()) {
                InetAddress Address = (InetAddress)Addresses.nextElement();
                if (Address.getHostAddress().startsWith(myip.split("\\.")[0])) {
                    networkInterface = Interface;
                    break;
                }
            }
        }
        assert networkInterface != null;
        short subnetMask = networkInterface.getInterfaceAddresses().get(1).getNetworkPrefixLength();
        byte[] addr = networkInterface.getInterfaceAddresses().get(1).getAddress().getAddress();

        StringBuilder addrBitString = new StringBuilder();
        for (byte b : addr) {
            addrBitString.append(String.format("%08d", Integer.parseInt(Integer.toBinaryString(b & 0xFF))));
        }

        return addrBitString.toString().substring(0, subnetMask);
    }
}

class HostReachableTest implements Runnable {

    private String host;
    private List<String> hostList;
    private List<Integer> taskCompleted;

    HostReachableTest(String host, List<String> hostList, List<Integer> taskCompleted) {
        this.host = host;
        this.hostList = hostList;
        this.taskCompleted = taskCompleted;
    }

    @Override
    public void run() {
        StringBuffer ipv4Pretty = new StringBuffer();
        for (int i=1;i<=32;i++) {
            ipv4Pretty.append(host.charAt(i-1));
            if (i%8==0 && i != 32) ipv4Pretty.append(".");
        }
        String[] byted = ipv4Pretty.toString().split("\\.");
        host = Integer.parseInt(byted[0], 2) +"."+ Integer.parseInt(byted[1], 2) +"."+ Integer.parseInt(byted[2], 2) +"."+ Integer.parseInt(byted[3], 2);
        byte[] addr = {(byte) Integer.parseInt(byted[0], 2), (byte) Integer.parseInt(byted[1], 2), (byte) Integer.parseInt(byted[2], 2), (byte) Integer.parseInt(byted[3], 2)};
        if (isHostReachable(addr)) {
            hostList.add(host);
            taskCompleted.add(1);
        }else {
            taskCompleted.add(0);
        }
    }

    public static boolean isHostReachable(byte[] host) {
        try {
            InetAddress address = InetAddress.getByAddress(host);
            return address.isReachable(5000); // Timeout in milliseconds
        } catch (Exception e) {
            return false;
        }
    }

}
