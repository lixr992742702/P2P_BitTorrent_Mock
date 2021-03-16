import Constant.Constant;
import peer.PeerInfo;
import peer.RemotePeerInfo;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class peerProcess {
    public static void main (String[] args) throws IOException {
        final int peerId = Integer.parseInt(args[0]);
        new Constant();
        PeerInfo _totalPeerInfo = new PeerInfo();
        _totalPeerInfo.initPeer();

        if (_totalPeerInfo.isContainPeer(peerId)) {
            RemotePeerInfo _peer = _totalPeerInfo.searchPeer(peerId);
            Process peerProc = new Process(peerId, _peer.getPort(), _peer._hasFile, _totalPeerInfo.peerlist);
            peerProc.init();
            Thread t = new Thread(peerProc);
            t.start();

            List<RemotePeerInfo> PeerConnected = _totalPeerInfo.peerlist.stream().filter((item)->item.getPeerId()<peerId).collect(Collectors.toList());
            peerProc.connectToPeers(PeerConnected);

            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
