import Constant.CommonProperties;
import log.EventLogger;
import peer.RemotePeerInfo;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

class OptimisticUnchoker extends Thread {
    public final int _numberOfOptimisticallyUnchokedNeighbors;
    public final int _optimisticUnchokingInterval;
    public final List<RemotePeerInfo> _chokedNeighbors = new ArrayList<>();
    final Collection<RemotePeerInfo> _optmisticallyUnchokedPeers =
            Collections.newSetFromMap(new ConcurrentHashMap<>());
    public final Collection<Process> _listeners;
    public final EventLogger _eventLogger;


    OptimisticUnchoker(CommonProperties conf, int peerId, Collection<Process> listeners) {
        super("OptimisticUnchoker");
        _numberOfOptimisticallyUnchokedNeighbors = 1;
        _optimisticUnchokingInterval = conf.proprities.get("OptimisticUnchokingInterval");
        _eventLogger = new EventLogger (peerId);
        _listeners = listeners;
    }

    synchronized void setChokedNeighbors(Collection<RemotePeerInfo> chokedNeighbors) {
        if(chokedNeighbors == null){
            return;
        }
        _chokedNeighbors.clear();
        _chokedNeighbors.addAll(chokedNeighbors);
    }

    @Override
    public void run() {
        while (true) {
            try {
                Thread.sleep(_optimisticUnchokingInterval);
            } catch (InterruptedException ex) {
            }

            synchronized (this) {
                if (!_chokedNeighbors.isEmpty()) {
                    Collections.shuffle(_chokedNeighbors);
                    _optmisticallyUnchokedPeers.clear();
                    _optmisticallyUnchokedPeers.addAll(_chokedNeighbors.subList(0,
                            Math.min(_numberOfOptimisticallyUnchokedNeighbors, _chokedNeighbors.size())));
                }
            }
            if (_chokedNeighbors.size() > 0) {
                _eventLogger.changeNeighbours("optimistically unchoked",_optmisticallyUnchokedPeers);
            }

            _listeners.stream().forEach(item->item.unchockedPeers(RemotePeerInfo.toIdSet(_optmisticallyUnchokedPeers)));
        }
    }
}