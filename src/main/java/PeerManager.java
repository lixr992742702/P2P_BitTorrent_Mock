import Constant.CommonProperties;
import log.EventLogger;
import peer.RemotePeerInfo;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class PeerManager implements Runnable {

    public final int _numberOfPreferredNeighbors;
    public final int _unchokingInterval;
    public final int _bitmapsize;
    public final EventLogger _eventLogger;
    public final List<RemotePeerInfo> _peers = new ArrayList<>();
    public final Set<RemotePeerInfo> _preferredPeers = new HashSet<>();
    public final OptimisticUnchoker _optUnchoker;
    public final Collection<Process> process_collection = new LinkedList<>();
    public final AtomicBoolean _randomlySelectPreferred = new AtomicBoolean(false);

    public PeerManager(int peerId, Collection<RemotePeerInfo> peers, int bitmapsize, CommonProperties conf) {
        _peers.addAll(peers);
        _numberOfPreferredNeighbors = conf.proprities.get("NumberOfPreferredNeighbors");
        _unchokingInterval = conf.proprities.get("UnchokingInterval")*1000;
        _optUnchoker = new OptimisticUnchoker(conf, peerId, process_collection);
        _bitmapsize = bitmapsize;
        _eventLogger = new EventLogger (peerId);
    }

    public synchronized void addInterestPeer(int remotePeerId) {
        RemotePeerInfo peer = searchPeer(remotePeerId);
        if (peer == null) {
            return;
        }
        peer._interested.set(true);
    }

    public long getUnchokingInterval() {
        return _unchokingInterval;
    }

    public synchronized void removeInterestPeer(int remotePeerId) {
        RemotePeerInfo peer = searchPeer(remotePeerId);
        if (peer == null) {
            return;
        }
        peer._interested.set(false);
    }

    synchronized List<RemotePeerInfo> getInterestedPeers() {
        return _peers.stream().filter((peer)->peer._interested.get()).collect(Collectors.toList());
    }

    public synchronized boolean isInteresting(int peerId, BitSet bitset) {
        RemotePeerInfo peer  = searchPeer(peerId);
        if(peer == null){
            return false;
        }
        BitSet pBitset = (BitSet) peer._receivedParts.clone();
        pBitset.andNot(bitset);
        return ! pBitset.isEmpty();
    }

    public synchronized void receivedPart(int peerId, int size) {
        RemotePeerInfo peer  = searchPeer(peerId);
        if (peer == null) {
            return;
        }
        peer._bytesDownloadedFrom.addAndGet(size);
    }

    public synchronized boolean canUploadToPeer(int peerId) {
        RemotePeerInfo peerInfo = new RemotePeerInfo(peerId);
        return (_preferredPeers.contains(peerInfo) ||
                _optUnchoker._optmisticallyUnchokedPeers.contains(peerInfo));
    }

    public synchronized void bitfieldArrived(int peerId, BitSet bitfield) {
        RemotePeerInfo peer  = searchPeer(peerId);
        peer._receivedParts = peer != null ? bitfield : peer._receivedParts;
        neighborsCompletedDownload();
    }

    public synchronized void haveArrived(int peerId, int partId) {
        RemotePeerInfo peer  = searchPeer(peerId);
        if (peer != null) {
            peer._receivedParts.set(partId);
        }
        neighborsCompletedDownload();
    }

    public synchronized BitSet getReceivedParts(int peerId) {
        RemotePeerInfo peer  = searchPeer(peerId);
        return peer != null ? (BitSet) peer._receivedParts.clone() : new BitSet();
    }

    synchronized private RemotePeerInfo searchPeer(int peerId) {
        return _peers.stream().filter((item)->item.getPeerId() == peerId).findAny().orElse(null);
    }

    synchronized private void neighborsCompletedDownload(){
        for (RemotePeerInfo peer : _peers) {
            if (peer._receivedParts.cardinality() < _bitmapsize) {
                return;
            }
        }
        process_collection.stream().forEach((item)->item.neighborsCompletedDownload());
    }

    public synchronized void registerListener(Process Process) {
        process_collection.add(Process);
    }



    @Override
    public void run() {

        _optUnchoker.start();

        while (true) {
            try {
                Thread.sleep(_unchokingInterval);
            } catch (InterruptedException ex) {
            }

            List<RemotePeerInfo> interestedPeers = getInterestedPeers();
            if (_randomlySelectPreferred.get()) {
                Collections.shuffle(interestedPeers);
            } else {
                Collections.sort(interestedPeers,(o1,o2)->(o2)._bytesDownloadedFrom.get() - (o1)._bytesDownloadedFrom.get());
            }

            List<RemotePeerInfo> optUnchokablePeers ;
            Set<Integer> chokedPeersIDs = new HashSet<>();
            Set<Integer> preferredNeighborsIDs = new HashSet<>();
            Map<Integer, Long> downloadedBytes = new HashMap<>();

            synchronized (this) {
                _peers.stream().forEach((item)->{
                    downloadedBytes.put (item.getPeerId(), item._bytesDownloadedFrom.longValue());
                    item._bytesDownloadedFrom.set(0);
                });
                _preferredPeers.clear();
                _preferredPeers.addAll(interestedPeers.subList(0, Math.min(_numberOfPreferredNeighbors, interestedPeers.size())));
                if (_preferredPeers.size() > 0) {
                    _eventLogger.changeNeighbours("preferred",_preferredPeers);
                }

                List<RemotePeerInfo> chokedPeers = new ArrayList<>(_peers);
                chokedPeers.removeAll(_preferredPeers);
                chokedPeersIDs.addAll(RemotePeerInfo.toIdSet(chokedPeers));
                optUnchokablePeers = _numberOfPreferredNeighbors >= interestedPeers.size() ? new ArrayList<>() : interestedPeers.subList(_numberOfPreferredNeighbors, interestedPeers.size());
                preferredNeighborsIDs.addAll (RemotePeerInfo.toIdSet(_preferredPeers));
            }

            process_collection.stream().forEach((item)->{
                item.chockedPeers(chokedPeersIDs);
                item.unchockedPeers(preferredNeighborsIDs);
            });

            _optUnchoker.setChokedNeighbors(optUnchokablePeers);
        }
    }
}
