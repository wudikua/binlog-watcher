import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * Created with IntelliJ IDEA.
 * User: mengjun
 * Date: 14-3-6
 * Time: 上午10:26
 * To change this template use File | Settings | File Templates.
 */
public class BinLogReader {

    public String binPath;

    public XInputStreamImpl is;

    public BinLogReader(String binPath) throws FileNotFoundException {
        this.binPath = binPath;
        this.is = new XInputStreamImpl(new FileInputStream((new File(this.binPath))));
    }

    public boolean isBinLogFile() throws IOException {
        byte[] binlogMagic = new byte[]{(byte)0xfe, (byte)0x62, (byte)0x69, (byte)0x6e};
        byte[] inputMagic = new byte[4];
        this.is.read(inputMagic, 0, inputMagic.length);
        if (Arrays.equals(binlogMagic, inputMagic)) {
            System.out.println(binPath + " is a bin log file");
            return true;
        }
        return false;
    }

    public CommonHeader parseHeader() throws IOException {
        CommonHeader header = new CommonHeader();
        header.timeStamp = is.readLong(4) * 1000L;
        header.type = is.readInt(1);
        header.serviceId = is.readInt(4);
        header.eventLength = is.readInt(4);
        header.nextPosition = is.readLong(4);
        header.flag = is.readInt(2);
        is.setReadLimit(header.eventLength - 19);
        return header;
    }

    public QueryEvent parseEvent() throws IOException {
        QueryEvent event = new QueryEvent();
        event.threadId = is.readLong(4);
        event.elapsedTime = is.readLong(4);
        event.databaseNameLength = is.readInt(1);
        event.errorCode = is.readInt(2);
        event.statusVariablesLength = is.readInt(2);
        is.readBytes(event.statusVariablesLength);
        event.databaseName = is.readNullTerminatedString();
        event.sql = new StringColumn(is.readBytes(is.available()));
        is.setReadLimit(0);
        return event;
    }

    public static void main(String args[]) throws IOException, InterruptedException {
        String binPath = "C:\\wamp\\mysql\\data\\mysqlbin-log.000002";
        BinLogReader br = new BinLogReader(binPath);
        final ArrayBlockingQueue queue = new ArrayBlockingQueue(1000);
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    QueryEvent e = null;
                    try {
                        e = (QueryEvent) queue.take();
                    } catch (InterruptedException e1) {
                        e1.printStackTrace();
                    }
                    System.out.println(e.sql);
                }
            }
        }).start();
        if (br.isBinLogFile()) {
            while (true) {
                Thread.sleep(1000);
                while (br.is.hasMore()) {
                    CommonHeader header = br.parseHeader();
                    if (header.type == 2) {
                        QueryEvent event = br.parseEvent();
                        queue.put(event);
                    } else {
                        br.is.skip(header.nextPosition - br.is.getHead());
                        br.is.setReadLimit(0);
                    }
                }
            }
        }
    }

}
