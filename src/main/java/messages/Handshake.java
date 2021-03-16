package messages;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.Arrays;

public class Handshake {
    public final static String _protocolId = "P2PFILESHARINGPROJ";
    public final byte[] _zeroBits = new byte[10];
    public final byte[] _peerId = new byte[4];

    public Handshake() {
    }

    public Handshake (int peerId) {
        this (ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(peerId).array());
    }

    private Handshake (byte[] peerId) {
        for (int i = 0;i< peerId.length;i++) {
            _peerId[i] = peerId[i];
        }
    }

    public void write(DataOutputStream oos) throws IOException {
        byte[] peerId = _protocolId.getBytes(Charset.forName("US-ASCII"));
        oos.write (peerId, 0, peerId.length);
        oos.write(_zeroBits, 0, _zeroBits.length);
        oos.write(_peerId, 0, _peerId.length);
    }

    public void read (DataInputStream ois) throws IOException {
        ois.read(new byte[_protocolId.length()], 0, _protocolId.length());
        ois.read(_zeroBits, 0, _zeroBits.length);
        ois.read(_peerId, 0, _peerId.length);
    }

}

