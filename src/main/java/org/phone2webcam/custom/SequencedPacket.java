package org.phone2webcam.custom;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class SequencedPacket {

    private int sequenceNumber;
    private byte[] data;

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

    public SequencedPacket(int sequenceNumber, byte[] data) {
        this.sequenceNumber = sequenceNumber;
        this.data = data;
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
