import Constant.CommonProperties;
import log.EventLogger;
import request.RequestedParts;
import java.io.*;
import java.util.*;

public class FileManager {
    private BitSet _receivedParts;
    private final List<Process> _listeners = new ArrayList<>();
    private final double _dPartSize;
    private final int _bitsetSize;
    private final RequestedParts _partsBeingReq;

    private final File _file;
    private final File  _partsDir;
    private static final String partsLocation = "parts/";

    public FileManager(int peerId, CommonProperties conf) {
        this (peerId, conf.Filename, conf.proprities.get("FileSize"), conf.proprities.get("PieceSize"), conf.proprities.get("UnchokingInterval"));
    }

    FileManager (int peerId, String fileName, int fileSize, int partSize, long unchokingInterval) {
        _dPartSize = partSize;
        _bitsetSize = (int) Math.ceil (fileSize/_dPartSize);
        _receivedParts = new BitSet (_bitsetSize);
        _partsBeingReq = new RequestedParts (_bitsetSize, unchokingInterval);
        _partsDir = new File( peerId + "/" + partsLocation );
        _partsDir.mkdirs();
        _file = new File(_partsDir.getParent() + "/" + fileName);
    }


    public synchronized void addPart (int partIdx, byte[] part) throws IOException {

        final boolean isNewPiece = !_receivedParts.get(partIdx);
        _receivedParts.set (partIdx);

        if (isNewPiece) {
            FileOutputStream fos;
            File ofile = new File(_partsDir.getAbsolutePath() + "/" + partIdx + ".txt");
            try {
                fos = new FileOutputStream(ofile);
                fos.write(part);
                fos.flush();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            for (Process process : _listeners) {
                process.pieceArrived (partIdx);
            }
        }
        if (isFileCompleted()) {
            List<File> list = new ArrayList<>();
            for (int i = 0; i < _receivedParts.cardinality();) {
                list.add(new File(_partsDir.getPath() + "/" + (i++) + ".txt"));
            }
            FileOutputStream fos = new FileOutputStream(_file);
            list.stream().forEach((file)->{
                FileInputStream fis = null;
                try {
                    fis = new FileInputStream(file);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                byte[] fileBytes = new byte[(int) file.length()];
                try {
                    int bytesRead = fis.read(fileBytes, 0, (int) file.length());
                    assert (bytesRead == fileBytes.length);
                    assert (bytesRead == (int) file.length());
                    fos.write(fileBytes);
                    fos.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            _listeners.stream().forEach((item)-> {
                item.fileCompleted();
            });
        }
    }

    public synchronized int getPartToRequest(BitSet availableParts) {
        availableParts.andNot(getReceivedParts());
        return _partsBeingReq.getPartToRequest (availableParts);
    }

    public synchronized BitSet getReceivedParts () {
        return (BitSet) _receivedParts.clone();
    }

    synchronized public boolean hasPart(int pieceIndex) {
        return _receivedParts.get(pieceIndex);
    }

    public synchronized void setAllParts() {
        for (int i = 0; i < _bitsetSize; i++) {
            _receivedParts.set(i, true);
        }
    }

    public synchronized int getNumberOfReceivedParts() {
        return _receivedParts.cardinality();
    }

    public byte[] getPiece(int partId) {
        File file = new File(_partsDir.getAbsolutePath() + "/" + partId + ".txt" );
        byte[] piece = getByteArrayFromFile(file);
        return piece;
    }

    private byte[] getByteArrayFromFile(File file){
        try {
            FileInputStream fis = new FileInputStream(file);
            byte[] fileBytes = new byte[(int) file.length()];
            int bytesRead = fis.read(fileBytes, 0, (int) file.length());
            assert (bytesRead == fileBytes.length);
            assert (bytesRead == (int) file.length());
            return fileBytes;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void registerListener (Process Process) {
        _listeners.add (Process);
    }

    public void splitFile() throws IOException {
        int[] peerIds = new int[]{1001,1006};
        for(int peerId:peerIds){
            File _file = new File(peerId+"/thefile.txt");
            String _file_type = "txt";
            int pieceSize = 32768;
            int FileLength = (int) _file.length();
            int _peerId = peerId;

            FileInputStream in = new  FileInputStream(_file);
            String newFileName;
            FileOutputStream out;
            int chrunkno = 0;
            int curPieceSize = pieceSize;
            byte[] byteChunk;
            String dirname = (_peerId)+"/parts";
            File dir = new File(dirname);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            while (FileLength > 0) {

                curPieceSize = FileLength < pieceSize?FileLength:curPieceSize;
                byteChunk = new byte[curPieceSize];
                int readLength = in.read(byteChunk, 0 , curPieceSize);

                FileLength -= curPieceSize;
                assert (readLength == byteChunk.length);
                newFileName =  (chrunkno) + "." + _file_type;
                File nf = new File(dirname, newFileName);
                if (!nf.exists()) {
                    nf.createNewFile();
                }
                chrunkno++;
                out = new FileOutputStream(dirname+"/"+newFileName);
                out.write(byteChunk);
                out.flush();
                out.close();
            }
            in.close();
        }
    }

    public int getBitmapSize() {
        return _bitsetSize;
    }

    private boolean isFileCompleted() {
        for (int i = 0; i < 67; i++) {
            if (!_receivedParts.get(i)) {
                return false;
            }
        }
        return true;
    }
}
