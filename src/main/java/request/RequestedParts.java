package request;

import java.util.BitSet;

public class RequestedParts {
    private final BitSet _requestedParts;
    private final long _timeoutInMillis;

    public RequestedParts(int nParts, long unchokingInterval) {
        _requestedParts = new BitSet(nParts);
        _timeoutInMillis = unchokingInterval * 2;
    }

    public synchronized int getPartToRequest(BitSet requestabableParts) {
        requestabableParts.andNot(_requestedParts);
        if (!requestabableParts.isEmpty()) {
            String set = requestabableParts.toString();
            String[] indexes = set.substring(1, set.length()-1).split(",");
            final int partId = Integer.parseInt(indexes[(int)(Math.random()*(indexes.length-1))].trim());
            _requestedParts.set(partId);
            new java.util.Timer().schedule(
                    new java.util.TimerTask() {
                        @Override
                        public void run() {
                        synchronized (_requestedParts) {
                            _requestedParts.clear(partId);
                        }
                        }
                    },
                    _timeoutInMillis
            );
            return partId;
        }
        return -1;
    }
}
