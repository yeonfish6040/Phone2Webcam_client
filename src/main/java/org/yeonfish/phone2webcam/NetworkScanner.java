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

    public NetworkScanner(int scan_start, int scan_end) {
        this.scan_start = scan_start;
        this.scan_end = scan_end;
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
        this.reachableHosts = new ArrayList<>();
        this.taskCompleted = new ArrayList<>();

        for (int i = this.scan_start; i <= this.scan_end; i++) {
            String host = baseIp + "." + i;
            (new Thread(new HostReachableTest(host, this.reachableHosts, this.taskCompleted))).start();
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

    private String getBaseIpAddress() throws IOException {
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

        return myip.split("\\.")[0] + "." + myip.split("\\.")[1] + "." + myip.split("\\.")[2];
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
        if (isHostReachable(host)) {
            hostList.add(host);
            taskCompleted.add(1);
        }else {
            taskCompleted.add(0);
        }
    }

    public static boolean isHostReachable(String host) {
        try {
            InetAddress address = InetAddress.getByName(host);
            return address.isReachable(5000); // Timeout in milliseconds
        } catch (Exception e) {
            return false;
        }
    }

}
