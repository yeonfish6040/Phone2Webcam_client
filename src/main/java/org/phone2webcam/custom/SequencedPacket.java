package org.phone2webcam.custom;

import java.io.Serializable;

public class SequencedPacket implements Serializable {

    private int sequenceNumber;
    private byte[] data;

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
}
