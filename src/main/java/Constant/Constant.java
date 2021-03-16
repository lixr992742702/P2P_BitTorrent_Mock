package Constant;

import java.util.HashMap;
import java.util.Map;

public class Constant {
    public static Map<String,Byte> type = new HashMap<>();
    public static Map<Byte,String> byte_map = new HashMap<>();
    public Constant(){
        type.put("Choke",(byte) 0);
        type.put("Unchoke", (byte) 1);
        type.put("Interested", (byte) 2);
        type.put("NotInterested", (byte) 3);
        type.put("Have", (byte) 4);
        type.put("BitField", (byte) 5);
        type.put("Request", (byte) 6);
        type.put("Piece", (byte) 7);

        byte_map.put((byte) 0,"Choke");
        byte_map.put((byte) 1,"Unchoke");
        byte_map.put((byte) 2,"Interested");
        byte_map.put((byte) 3,"NotInterested");
        byte_map.put((byte) 4,"Have");
        byte_map.put((byte) 5,"BitField");
        byte_map.put((byte) 6,"Request");
        byte_map.put((byte) 7,"Piece");
    }
}
