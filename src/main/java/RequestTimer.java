
import messages.Message;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.TimerTask;

public class RequestTimer extends TimerTask {
    private final Message _request;
    private final FileManager _fileMgr;
    private final  DataOutputStream _out;
    private final int _remotePeerId;
    private final Message _message;

    public RequestTimer(Message request, FileManager fileMgr, DataOutputStream out, Message message, int remotePeerId) {
        super();
        _request = request;
        _fileMgr = fileMgr;
        _out = out;
        _remotePeerId = remotePeerId;
        _message = message;
    }

    @Override
    public void run() {
        if (! _fileMgr.hasPart(_request.getPieceIndex())) {
            try {
                _message.write(_out);
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }
}


