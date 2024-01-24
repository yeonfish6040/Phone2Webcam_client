package org.yeonfish.phone2webcam;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

    private static String getBaseIpAddress() throws UnknownHostException {
        String myip = InetAddress.getLocalHost().toString().split("/")[1];
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
