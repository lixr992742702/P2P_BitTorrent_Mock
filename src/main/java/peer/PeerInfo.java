package peer;

import java.io.*;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicBoolean;

public class PeerInfo {
    public static String CONFIG_FILE_NAME = "PeerInfo.cfg";
    public ArrayList<RemotePeerInfo> peerlist ;
    public final String COMMENT_CHAR = "#";
    public final Collection<RemotePeerInfo> _peerInfoVector = new LinkedList<>();

    public PeerInfo() {
        this.peerlist = new ArrayList<>();
    }

    public void initPeer() throws IOException {
        BufferedReader in = new BufferedReader(new FileReader("PeerInfo.cfg"));
        String str = in.readLine();
        while (str != null) {
            String arr[] = str.split(" ");
            peerlist.add(new RemotePeerInfo(arr[0], arr[1], arr[2], arr[3].equals("1")?true:false));
            str = in.readLine();
        }
    }

    public void read (Reader reader) throws  IOException, ParseException {
        BufferedReader in = new BufferedReader(reader);
        int i = 0;
        for (String line; (line = in.readLine()) != null;i++) {
            line = line.trim();
            if ((line.length() <= 0) || (line.startsWith (COMMENT_CHAR))) {
                continue;
            }
            String[] tokens = line.split("\\s+");
            if (tokens.length != 4) {
                throw new ParseException(line, i);
            }
            final boolean bHasFile = (tokens[3].trim().compareTo("1") == 0);
            _peerInfoVector.add (new RemotePeerInfo(tokens[0].trim(), tokens[1].trim(),
                    tokens[2].trim(), bHasFile));
        }
    }

    public Collection<RemotePeerInfo> getPeerInfo () {
        return new LinkedList<>(_peerInfoVector);
    }

    public boolean isContainPeer (int id) {
        AtomicBoolean flag = new AtomicBoolean(false);
        peerlist.stream().forEach((peer)->{
            if(peer.getPeerId() == id){
                flag.set(true);
            }
        });
        return flag.get();
    }

    public RemotePeerInfo searchPeer (int id) {
        return peerlist.stream().filter((item)->item.getPeerId() == id).findAny().orElse(null);
    }

}
