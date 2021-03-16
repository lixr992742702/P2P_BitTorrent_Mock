package peer;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class RemotePeerInfo {
    public Map<String, String> peerInfo = new HashMap<>();
    public final boolean _hasFile;
    public AtomicInteger _bytesDownloadedFrom;
    public BitSet _receivedParts;
    public final AtomicBoolean _interested;
    public RemotePeerInfo (int peerId) {
        this (Integer.toString (peerId), "127.0.0.1", "0", false);
    }

    public RemotePeerInfo(String pId, String pAddress, String pPort, boolean hasFile) {
        peerInfo.put("_peerId",pId);
        peerInfo.put("_peerAddress",pAddress);
        peerInfo.put("_peerPort",pPort);
        _hasFile = hasFile;
        _bytesDownloadedFrom = new AtomicInteger (0);
        _receivedParts = new BitSet();
        _interested = new AtomicBoolean (false);
    }

    public int getPeerId() {
        return Integer.parseInt(peerInfo.get("_peerId"));
    }

    public int getPort() {
        return Integer.parseInt(peerInfo.get("_peerPort"));
    }

    @Override
    public boolean equals (Object obj) {
        return obj != null && obj instanceof RemotePeerInfo ? ((RemotePeerInfo) obj).peerInfo.get("_peerId").equals(peerInfo.get("_peerId")) : false;
    }

    @Override
    public int hashCode() {
        return  291 + Objects.hashCode(this.peerInfo.get("_peerId"));
    }

    @Override
    public String toString() {
        return peerInfo.get("_peerId") + " address:" + peerInfo.get("_peerAddress") + " port: " + peerInfo.get("_peerPort");
    }

    public static Collection<Integer> toIdSet (Collection<RemotePeerInfo> peers) {
        return peers.stream().map((peer)->peer.getPeerId()).collect(Collectors.toSet());
    }
}
