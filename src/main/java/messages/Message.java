package messages;

import Constant.Constant;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

public class Message {
    public int _length;
    public String _type;
    public byte[] _payload;

    public Message (String type) {
        this (type, null);
    }

    public Message (String type, byte[] payload) {
        _length = (payload == null ? 0 : payload.length) + 1;
        _type = type;
        _payload = payload;
    }

    public String getType() {
        return _type;
    }

    public void read (DataInputStream in) throws IOException {
        if ((_payload != null) && (_payload.length) > 0) {
            in.readFully(_payload, 0, _payload.length);
        }
    }

    public void write (DataOutputStream out) throws IOException {
        out.writeInt (_length);
        out.writeByte (Constant.type.get(_type));
        if ((_payload != null) && (_payload.length > 0)) {
            out.write (_payload, 0, _payload.length);
        }
    }

    public int getPieceIndex() {
        return ByteBuffer.wrap(Arrays.copyOfRange(_payload, 0, 4)).order(ByteOrder.BIG_ENDIAN).getInt();
    }

    public static byte[] getPieceIndexBytes (int pieceIdx) {
        return ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(pieceIdx).array();
    }

    public static Message getInstance (int length, String type) throws ClassNotFoundException {
        if (type.equals("Choke")) {
            return new Message("Choke");
        }else if(type.equals("Unchoke")) {
            return new Message("Unchoke");
        }else if(type.equals("Interested")) {
            return new Message("Interested");
        }else if(type.equals("NotInterested")) {
            return new Message("NotInterested");
        }else if(type.equals("Have")) {
            return new Message ("Have",new byte[length]);
        }else if(type.equals("BitField")) {
            return new Message ("BitField",new byte[length]);
        }else if(type.equals("Request")) {
            return new Message ("Request",new byte[length]);
        }else if(type.equals("Piece")) {
            return new Message ("Piece",new byte[length]);
        }else {
            throw new ClassNotFoundException ("message type not handled: " + type);
        }
    }
}
