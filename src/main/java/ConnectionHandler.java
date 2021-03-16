import log.EventLogger;
import messages.Handshake;
import messages.Message;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import Constant.Constant;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.BitSet;

public class ConnectionHandler implements Runnable {
    private static final int PEER_ID_UNSET = -1;
    private final FileManager _fileMgr;
    private final int _localPeerId;
    private final Socket _socket;
    private final AtomicInteger _remotePeerId;
    private final boolean _isConnectingPeer;
    private final BlockingQueue<Message> _queue = new LinkedBlockingQueue<>();
    private final PeerManager _peerMgr;
    private boolean _chokedByRemotePeer;
    private  int remotePeerId;
    private  EventLogger  _eventLogger;
    private final DataOutputStream _out;

    public ConnectionHandler(int localPeerId, Socket socket, FileManager fileMgr, PeerManager peerMgr)
            throws IOException {
        this(localPeerId, false, socket, fileMgr, peerMgr);
    }

    public ConnectionHandler(int localPeerId, boolean isConnectingPeer ,
                             Socket socket, FileManager fileMgr, PeerManager peerMgr) throws IOException {
        _localPeerId = localPeerId;
        _socket = socket;
        _out = new DataOutputStream(_socket.getOutputStream()) ;
        _isConnectingPeer = isConnectingPeer;
        _fileMgr = fileMgr;
        _eventLogger = new EventLogger(_localPeerId);
        _peerMgr = peerMgr;
        _remotePeerId = new AtomicInteger(PEER_ID_UNSET);
    }

    public int getRemotePeerId() {
        return _remotePeerId.get();
    }

    @Override
    public void run() {
        new Thread() {
            private boolean _remotePeerIsChoked = true;
            @Override
            public void run() {
                Thread.currentThread().setName(getClass().getName() + "-" + _remotePeerId + "-sending thread");
                while (true) {
                    try {
                        final Message message = _queue.take();
                        if (message == null || _remotePeerId.get() == PEER_ID_UNSET) {
                            continue;
                        }
                        if (message._type.equals("Choke")) {
                            if (!_remotePeerIsChoked) {
                                _remotePeerIsChoked = true;
                                sendInternal(message);
                            }
                        }else if(message._type.equals("Unchoke")){
                            if (_remotePeerIsChoked) {
                                _remotePeerIsChoked = false;
                                sendInternal(message);
                            }
                        }else{
                            sendInternal(message);
                        }
                    } catch (IOException ex) {
                    } catch (InterruptedException ex) {}
                }
            }
        }.start();

        try {
            DataInputStream in = new DataInputStream(_socket.getInputStream());

            (new Handshake((_localPeerId))).write(_out);

            Handshake rcvdHandshake = new Handshake();
            rcvdHandshake.read(in);

            _remotePeerId.set(ByteBuffer.wrap(rcvdHandshake._peerId).order(ByteOrder.BIG_ENDIAN).getInt());
            Thread.currentThread().setName(getClass().getName() + "-" + _remotePeerId.get());

            _chokedByRemotePeer = true;
            remotePeerId = _remotePeerId.get();

            _eventLogger.peerConnection(_remotePeerId.get(), _isConnectingPeer);

            sendInternal(handle());
            while (true) {
                try {
                    Message message = Message.getInstance(in.readInt() - 1, Constant.byte_map.get(in.readByte()));
                    message.read(in);
                    sendInternal(handle(message));
                } catch (Exception ex) {
                    System.exit(0);
                    System.out.println("socket is closed");
                    break;
                }
                Thread.sleep(5);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            try {
                _socket.close();
                System.exit(0);
            } catch (Exception e) {
            }
        }
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof ConnectionHandler && ((ConnectionHandler) obj)._remotePeerId == _remotePeerId;
    }

    @Override
    public int hashCode() {
        return 287 + _localPeerId;
    }

    public void send(final Message message) {
        _queue.add(message);
    }

    private synchronized void sendInternal(Message message) throws IOException {
        if (message != null && !_socket.isClosed()) {
            message.write(_out);
            _out.flush();
            if (message.getType().equals("Request")) {
                new java.util.Timer().schedule(
                        new RequestTimer(message, _fileMgr, _out, message, _remotePeerId.get()),
                        _peerMgr.getUnchokingInterval() * 2
                );
            }
        }
    }

    public Message handle() {
        BitSet bitset = _fileMgr.getReceivedParts();
        return !bitset.isEmpty()?new Message("BitField",bitset.toByteArray()):null;
    }

    private static byte[] join (int pieceIdx, byte[] second) {
        byte[] concat = new byte[4 + (second == null ? 0 : second.length)];
        System.arraycopy(Message.getPieceIndexBytes (pieceIdx), 0, concat, 0, 4);
        System.arraycopy(second, 0, concat, 4, second.length);
        return concat;
    }

    public byte[] getContent(Message piece) {
        return (piece._payload == null) || (piece._payload.length <= 4) ? null : Arrays.copyOfRange(piece._payload, 4, piece._payload.length);
    }

    public Message handle(Message msg) throws IOException {
        if (msg._type.equals("Choke")) {
            _chokedByRemotePeer = true;
            _eventLogger.printMessage("choked",remotePeerId);
        }else if(msg._type.equals("Unchoke")) {
            _chokedByRemotePeer = false;
            _eventLogger.printMessage("unchoked",remotePeerId);
            return requestPiece();
        }else if(msg._type.equals("Interested")) {
            _eventLogger.printMessage("interested",remotePeerId);
            _peerMgr.addInterestPeer(remotePeerId);
        }else if(msg._type.equals("NotInterested")) {
            _eventLogger.printMessage("not interested",remotePeerId);
            _peerMgr.removeInterestPeer(remotePeerId);
        }else if(msg._type.equals("Have")) {
            final int pieceId = msg.getPieceIndex();
            _eventLogger.haveMessage(remotePeerId, pieceId);
            _peerMgr.haveArrived(remotePeerId, pieceId);
            return new Message(_fileMgr.getReceivedParts().get(pieceId)?"NotInterested":"Interested");
        }else if(msg._type.equals("BitField")) {
            BitSet bitset = BitSet.valueOf(msg._payload);
            _peerMgr.bitfieldArrived(remotePeerId, bitset);
            bitset.andNot(_fileMgr.getReceivedParts());
            return new Message(bitset.isEmpty()?"NotInterested":"Interested");
        }else if(msg._type.equals("Request")&&_peerMgr.canUploadToPeer(remotePeerId)) {
            byte[] piece = _fileMgr.getPiece(msg.getPieceIndex());
            return piece != null ? new Message("Piece",join(msg.getPieceIndex(), piece)) : null;
        }else if(msg._type.equals("Piece")) {
            _fileMgr.addPart(msg.getPieceIndex(), getContent(msg));
            _peerMgr.receivedPart(remotePeerId, getContent(msg).length);
            _eventLogger.pieceDownloadedMessage(remotePeerId, msg.getPieceIndex(), _fileMgr.getNumberOfReceivedParts());
            return requestPiece();
        }
        return null;
    }

    private Message requestPiece() {
        if (!_chokedByRemotePeer) {
            int partId = _fileMgr.getPartToRequest(_peerMgr.getReceivedParts(remotePeerId));
            return partId >= 0 ? new Message ("Request",ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(partId).array()) : null;
        }
        return null;
    }
}
