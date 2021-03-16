import Constant.CommonProperties;
import log.EventLogger;
import messages.Message;
import peer.RemotePeerInfo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class Process implements Runnable {
    private final int _peerId;
    private final int _port;
    private final boolean _hasFile;
    private final CommonProperties _conf = new CommonProperties();
    private final FileManager _fileMgr;
    private final PeerManager _peerMgr;
    private final EventLogger _eventLogger;
    private final AtomicBoolean _fileCompleted = new AtomicBoolean(false);
    private final AtomicBoolean _peersFileCompleted = new AtomicBoolean(false);
    private final AtomicBoolean _terminate = new AtomicBoolean(false);
    private final Collection<ConnectionHandler> _connHandlers =
            Collections.newSetFromMap(new ConcurrentHashMap<>());

    public Process(int peerId, int port, boolean hasFile, Collection<RemotePeerInfo> peerInfo) throws IOException {
        _port = port;
        _peerId = peerId;
        _hasFile = hasFile;
        _fileMgr = new FileManager(_peerId, _conf);
        ArrayList<RemotePeerInfo> remotePeers = new ArrayList<>(peerInfo);
        for (RemotePeerInfo ri : remotePeers) {
            if (Integer.parseInt(ri.peerInfo.get("_peerId")) == peerId) {
                remotePeers.remove(ri);
                break;
            }
        }
        _peerMgr = new PeerManager(_peerId, remotePeers, _fileMgr.getBitmapSize(), _conf);
        _eventLogger = new EventLogger(peerId);
        _fileCompleted.set(_hasFile);
    }

    void init() throws IOException {
        _fileMgr.registerListener(this);
        _peerMgr.registerListener(this);

        if (_hasFile) {
            _fileMgr.splitFile();
            _fileMgr.setAllParts();
        }

        Thread t = new Thread(_peerMgr);
        t.setName(_peerMgr.getClass().getName());
        t.start();
    }

    @Override
    public void run() {
        try {
            ServerSocket serverSocket = new ServerSocket(_port);
            while (!_terminate.get()) {
                try {
                    addConnHandler(new ConnectionHandler(_peerId, serverSocket.accept(), _fileMgr, _peerMgr));

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    void connectToPeers(Collection<RemotePeerInfo> peersToConnectTo) {
        Iterator<RemotePeerInfo> iter = peersToConnectTo.iterator();
        while (iter.hasNext()) {
            do {
                Socket socket = null;
                RemotePeerInfo peer = iter.next();
                try {
                    socket = new Socket(peer.peerInfo.get("_peerAddress"), peer.getPort());
                    if (addConnHandler(new ConnectionHandler(_peerId, true,
                            socket, _fileMgr, _peerMgr))) {
                        iter.remove();
                    }
                } catch (ConnectException ex) {
                    ex.printStackTrace();
                    if (socket != null) {
                        try {
                            socket.close();
                        } catch (IOException ex1)
                        {}
                    }
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                    if (socket != null) {
                        try {
                            socket.close();
                        } catch (IOException ex1)
                        {}
                    }
                }
            }
            while (iter.hasNext());

            iter = peersToConnectTo.iterator();
        }
    }

    public void neighborsCompletedDownload() {
        try{
            _peersFileCompleted.set(true);

            _eventLogger.BreakPointOut(" Remove all the parts");
            _eventLogger.BreakPointOut(" neighborsCompletedDownload");
        } finally {
            if (_peersFileCompleted.get()) {
                //_terminate.set(true);
                removeAllparts();

                System.exit(0);
            }
        }
    }

    public synchronized void fileCompleted() {
        _eventLogger.fileDownloadedMessage();
        _eventLogger.BreakPointOut("fileCompleted");
        _fileCompleted.set(true);
        if (_peersFileCompleted.get()) {
            _eventLogger.SystemExitMessage();
            _terminate.set(true);
        }
    }

    public synchronized void pieceArrived(int partIdx) {
        _connHandlers.stream().forEach(item->{
            item.send(new Message("Have", ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(partIdx).array()));
            if (!_peerMgr.isInteresting(item.getRemotePeerId(), _fileMgr.getReceivedParts())) {
                item.send(new Message("NotInterested"));
            }
        });
    }

    private synchronized boolean addConnHandler(ConnectionHandler connHandler) {
        if (!_connHandlers.contains(connHandler)) {
            _connHandlers.add(connHandler);
            new Thread(connHandler).start();
            try {
                wait(10);
            } catch (InterruptedException e) { }
        }
        return true;
    }

    public synchronized void chockedPeers(Collection<Integer> chokedPeersIds) {
        _connHandlers.stream().forEach(item->{
            if(chokedPeersIds.contains(item.getRemotePeerId())){
                item.send(new Message("Choke"));
            }
        });
    }

    public synchronized void unchockedPeers(Collection<Integer> unchokedPeersIds) {
        _connHandlers.stream().forEach(item->{
            if(unchokedPeersIds.contains(item.getRemotePeerId())){
                item.send(new Message("Unchoke"));
            }
        });
    }

    public synchronized void removeAllparts() {
        int[] peerIds = new int[]{1001,1002, 1003, 1004, 1005, 1006, 1007, 1008, 1009};
        for(int peerId:peerIds){
            deletefile(peerId+"/parts");

        }
    }


    public synchronized void deletefile(String dirPath) {
        File file = new File(dirPath);
        if(file.isFile())
        {
            file.delete();
        }else
        {
            File[] files = file.listFiles();
            if(files == null)
            {
                file.delete();
            }else
            {
                for (int i = 0; i < files.length; i++)
                {
                    deletefile(files[i].getAbsolutePath());
                }
                file.delete();
            }
        }
    }


}
