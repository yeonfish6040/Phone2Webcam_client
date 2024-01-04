package org.yeonfish.phone2webcam.custom;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.*;

public class UDPTools {
    DatagramSocket dgramSocket;

    // initialize
    public UDPTools(int port) throws UnknownHostException, SocketException {
        this.dgramSocket = new DatagramSocket(port);
    }

    public void sendPacket(InetAddress host, int port, byte[] data) throws IOException {

        // header size: 24byte; -> real packet size: chunkSize+24 -> 1024
        // if you want to use this you should use at least 1024 bytes to receivePacket()  ex) receivePacket(1024, 0)
        int chunkSize = 1000;
        int totalChunks = ((int) Math.ceil((double) data.length / chunkSize));

        for (int i = 0; i < totalChunks; i++) {
            int offset = i * chunkSize;
            int length = Math.min(chunkSize, data.length - offset);

            byte[] chunk = new byte[length];
            System.arraycopy(data, offset, chunk, 0, length);

            // add header: header contains chunk count and chunk's sequence number
            byte[] sPacket = new SequencedPacket(i, chunk).toSequencedPacketData();
            this.dgramSocket.send(new DatagramPacket(sPacket, sPacket.length, host, port));
        }

        // this packet contains chunk count -> chunkCount*-1
        byte[] sPacket = new SequencedPacket((totalChunks+1)*-1, null).toSequencedPacketData();
        this.dgramSocket.send(new DatagramPacket(sPacket, sPacket.length, host, port));
    }

    public CustomPacket receivePacket(int bytes, int timeout) throws IOException {
        byte[] buffer = new byte[bytes];
        DatagramPacket dgramPacketRecv = new DatagramPacket(buffer, buffer.length);

        int timeoutTmp = this.dgramSocket.getSoTimeout(); // save previous timeout setting
        int packetCount = -1;                             // packet count will be updated when specific packet received
        int currentPacketCount = 0;
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try {
            this.dgramSocket.setSoTimeout(timeout); // set timeout

            Map<Integer, byte[]> data = new HashMap<>(); // chunks.
            while (true) {
                this.dgramSocket.receive(dgramPacketRecv);
                byte[] receivedData = new byte[dgramPacketRecv.getLength()];
                System.arraycopy(dgramPacketRecv.getData(), 0, receivedData, 0, dgramPacketRecv.getLength()); // cut empty part of packet
                currentPacketCount++;

                SequencedPacket sequencedPacket = new SequencedPacket(receivedData);

                if (sequencedPacket.getSequenceNumber() < 0) {
                    packetCount = sequencedPacket.getSequenceNumber()*-1;                     // update packet count when specific packet received
                }else
                    data.put(sequencedPacket.getSequenceNumber(), sequencedPacket.getData()); // if usual packet, just save into data.

                if (packetCount == currentPacketCount) break;
            }

            // sort packets
            List<Integer> keySet = new ArrayList<>(data.keySet());
            Collections.sort(keySet);

            // assembly packet
            for (Integer i:keySet) {
                byteArrayOutputStream.write(data.get(i), 0, data.get(i).length);
            }

            this.dgramSocket.setSoTimeout(timeoutTmp); // undo timeout
        } catch (SocketTimeoutException exception) {
            this.dgramSocket.setSoTimeout(timeoutTmp); // undo timeout
        } catch (Exception exception) {
            exception.printStackTrace();
        }

        // just i wanted to use both of datagram packet and assembled data
        CustomPacket cPacket = new CustomPacket();
        cPacket.packet = dgramPacketRecv;
        cPacket.data = byteArrayOutputStream.toByteArray();
        byteArrayOutputStream.close();

        return cPacket;
    }

    public void close() {
        this.dgramSocket.close();
    }
}

class SequencedPacket {

    private int sequenceNumber;
    private byte[] data;

    // create new sequenced packet data
    public SequencedPacket(int sequenceNumber, byte[] data) {
        this.sequenceNumber = sequenceNumber;
        this.data = data;
    }

    // decode(?) sequenced packet
    public SequencedPacket(byte[] serialized) {
        byte[] header = new byte[24];

        System.arraycopy(serialized, 0, header, 0, 24);
        if (serialized.length != 24) {
            this.data = new byte[serialized.length-24];
            System.arraycopy(serialized, 24, this.data, 0, serialized.length-24);
        }else
            this.data = null;

        this.sequenceNumber = ByteBuffer.wrap(header).getInt();
    }

    public int getSequenceNumber() {
        return this.sequenceNumber;
    }

    public byte[] getData() {
        return data;
    }

    public byte[] toSequencedPacketData() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] header = ByteBuffer.allocate(24).putInt(this.sequenceNumber).array();

        baos.write(header);

        if (this.data != null)
            baos.write(this.data);

        byte[] sequencedPacketData = baos.toByteArray();
        baos.close();

        return sequencedPacketData;
    }
}