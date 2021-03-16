package startUp;

import peer.PeerInfo;
import peer.RemotePeerInfo;
import java.io.*;
import java.text.ParseException;
import java.util.Collection;

public class StartRemotePeers {
//    private final String COMMENT_CHAR = "#";
//    private final Collection<RemotePeerInfo> _peerInfoVector = new LinkedList<>();

    private static void startRemotePeer (String path, Collection<RemotePeerInfo> peers) throws IOException {
        for (RemotePeerInfo peer : peers) {
            System.out.println("Start remote peer " + peer.peerInfo.get("_peerId ")+  " at " + peer.peerInfo.get("_peerAddress"));
            Runtime.getRuntime().exec ("ssh " + peer.peerInfo.get("_peerAddress") + " cd " + path + "; java peerProcess " + peer.peerInfo.get("_peerId"));
        }
        System.out.println ("Started all peers.");
    }

    public static void main(String[] args) {
        final String configFile = (args.length == 0 ? PeerInfo.CONFIG_FILE_NAME : args[0]);
        FileReader in = null;
        try {
            in = new FileReader (configFile);
            PeerInfo peerInfo = new PeerInfo();
            peerInfo.read (in);
            startRemotePeer (System.getProperty("user.dir"), peerInfo.getPeerInfo());
        }
        catch (IOException | ParseException ex) {
            ex.printStackTrace();
        } finally {
            try {
                in.close();
            } catch (Exception e) {}
        }
    }
}
