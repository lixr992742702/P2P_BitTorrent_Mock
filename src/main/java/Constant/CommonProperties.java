package Constant;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class CommonProperties {
    public  Map<String,Integer> proprities = new HashMap<>();
    public  String Filename;

    public CommonProperties() throws IOException {
        String arr[] = new String[6];
        BufferedReader in = new BufferedReader(new FileReader("Common.cfg"));
        String str = in.readLine();
        int index = 0 ;
        while (str != null) {
            String temp[] = str.split(" ");
            arr[index] = temp[1];
            index ++;
            str = in.readLine();
        }

        proprities.put("NumberOfPreferredNeighbors",Integer.parseInt(arr[0]));
        proprities.put("UnchokingInterval",Integer.parseInt(arr[1]));
        proprities.put("OptimisticUnchokingInterval",Integer.parseInt(arr[2]));
        proprities.put("FileSize",Integer.parseInt(arr[4]));
        proprities.put("PieceSize",Integer.parseInt(arr[5]));
        this.Filename = arr[3];
    }
}
