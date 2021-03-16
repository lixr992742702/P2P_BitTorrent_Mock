package log;

import peer.RemotePeerInfo;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;

public class EventLogger {
    private  int _peerId;
    private  String log_msg = "";

    public EventLogger (int peerId) {
        this._peerId = peerId;
    }

    public void logInput ()  {
        System.out.println(this.log_msg);
        String Filename =  "log_peer_[" + _peerId + "].log";
        try {
            File file = new File(Filename);
            if (!file.exists()) {
                file.createNewFile();
            }
            System.out.println(log_msg);
            FileWriter fw = new FileWriter(file.getName(),true);
            Timestamp timestamp = new Timestamp(System.currentTimeMillis());
            String strn = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(timestamp);
            fw.write("["+ strn + "]: "+ this.log_msg+"\n");
            fw.flush();
            fw.close();
        }catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void peerConnection (int peerId, boolean isConnected) {
        this.log_msg = "[" + this._peerId + (isConnected?"] is connected from Peer [":"]makes a connnection to Peer [")+ peerId+"]";
        logInput();
    }

    public void changeNeighbours(String str, Collection<RemotePeerInfo> preferredNeighbors){
        this.log_msg = "Peer [" + this._peerId + "] has the "+str+" neighbors [" + CollectionToString(preferredNeighbors)  + "]";
        logInput();
    }

    public void printMessage(String str, int peerId){
        String msg = "";
        switch (str){
            case "choked":
                msg = "is choked by";
                break;
            case "unchoked":
                msg = "is unchoked by";
                break;
            case "interested":
                msg = "receive the 'interested' message from";
                break;
            case "not interested":
                msg = "receive the 'not interested' message from";
                break;
        }
        this.log_msg = "Peer [" + this._peerId + "] "+msg+" [" + peerId + "]";
        logInput();
    }

    public void haveMessage (int peerId, int piece_index) {
        this.log_msg = "Peer [" + this._peerId + "] received the 'have' message from [" + peerId + "] for the piece [" + piece_index +"]";
        logInput();
    }

    public void pieceDownloadedMessage (int peerId, int piece_index, int currNumberOfPieces) {
        this.log_msg = "Peer [" + this._peerId + "] have downloaded the piece [" + piece_index + "] for the piece [" + peerId +
                "], and now totally have [" + currNumberOfPieces+ "] pieces";
        logInput();
    }

    public void fileDownloadedMessage () {
        this.log_msg = "["+this._peerId + "] has downloaded the complete file";
        logInput();
    }

    public void SystemExitMessage() {
        this.log_msg = "["+this._peerId + "] has system exit";
        logInput();
    }

    public void BreakPointOut(String out) {
        this.log_msg = "["+this._peerId + "] has a break point out and print: " +out;
        logInput();
    }

    private String CollectionToString (Collection<RemotePeerInfo> peers){
        StringBuilder sb = new StringBuilder ();
        AtomicBoolean isFirst = new AtomicBoolean(false);
        peers.stream().forEach((item)->{
            String str = isFirst.get() ? "," + item.getPeerId() : String.valueOf(item.getPeerId());
            isFirst.set(true);
            sb.append(str);
        });
        return sb.toString();
    }
}
