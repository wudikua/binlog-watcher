import java.io.*;
import java.net.Socket;
import java.util.Arrays;
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

    public XOutputStreamImpl os;

    public Socket socket;

    public String userName;

    public String password;

    public BinLogReader(String binPath) throws FileNotFoundException {
        this.binPath = binPath;
        this.is = new XInputStreamImpl(new FileInputStream((new File(this.binPath))));
    }

    public BinLogReader(String host, int port, String userName, String password) throws IOException {
        this.socket = new Socket(host, port);
        this.socket.setKeepAlive(true);
        this.socket.setTcpNoDelay(true);
        this.is = new XInputStreamImpl(this.socket.getInputStream());
        this.os = new XOutputStreamImpl(this.socket.getOutputStream(), 1024);
        this.userName = userName;
        this.password = password;
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
        event.sql = new String(is.readBytes(is.available()));
        is.setReadLimit(0);
        return event;
    }

    public static void fileWatcher(String binPath) throws IOException, InterruptedException {
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

    public static byte[] concat(byte[] data1, byte[] data2) {
        final byte r[] = new byte[data1.length + data2.length];
        System.arraycopy(data1, 0, r, 0, data1.length);
        System.arraycopy(data2, 0, r, data1.length, data2.length);
        return r;
    }

    public boolean connectMysql() throws IOException {
        //读mysql的 greeting pocket
        int length = this.is.readInt(3);
        int sequence = this.is.readInt(1);
        int protocol = this.is.readInt(1);
        String version = this.is.readNullTerminatedString();
        long threadId = this.is.readLong(4);
        byte[] salt1 = new byte[8];
        byte[] salt2 = new byte[12];
        byte[] salt = new byte[20];
        this.is.read(salt1, 0, 8);
        this.is.skip(1);
        int serverCapabilities = this.is.readInt(2);
        int charset = this.is.readInt(1);
        int status = this.is.readInt(2);
        this.is.skip(13);
        this.is.read(salt2, 0, 12);
        System.arraycopy(salt1, 0, salt, 0, salt1.length);
        System.arraycopy(salt2, 0, salt, salt1.length, salt2.length);
        this.is.skip(1);

        //写login packet
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        XOutputStreamImpl tos = new XOutputStreamImpl(bos);
        tos.writeInt(33284, 4);
        tos.writeInt(0, 4);
        tos.writeInt(charset, 1);
        tos.writeBytes(0, 23);
        tos.write("root".getBytes(), 0, this.userName.getBytes().length);
        tos.write("\0".getBytes(),0,1);
        tos.writeInt(20, 1);
        //加密算法 SHA1(password) XOR SHA1("20-bytes random data from server" <concat> SHA1(SHA1(password)))
        byte[] byte1 = CodecUtils.sha(this.password.getBytes());
        byte[] byte2 = CodecUtils.sha((concat(salt, CodecUtils.sha(byte1))));
        tos.writeBytes(CodecUtils.xor(byte1, byte2));
        tos.flush();
        byte[] body = bos.toByteArray();
        this.os.writeInt(body.length, 3);
        this.os.writeInt(sequence + 1, 1);
        this.os.writeBytes(body);
        this.os.flush();

        //读mysql auth result
        int authLength = this.is.readInt(3);
        int authSeq = this.is.readInt(1);
        int ok = this.is.readInt(1);
        if (ok == 0 && authSeq == sequence + 2) {
            this.is.skip(authLength-1);
            System.out.println("与MYSQL建立连接验证成功");
            return true;
        } else {
            int errorCode = this.is.readInt(2);
            this.is.skip(6);
            String errorMsg = this.is.readNullTerminatedString();
            System.out.println(errorCode + " : " +errorMsg);
            return false;
        }
    }

    public boolean slaveRegister() throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        XOutputStreamImpl tos = new XOutputStreamImpl(bos);
        this.os.writeInt(0x12, 1);
        this.os.writeLong(4, 4);
        return true;
    }

    public static void slaveWatcher() throws IOException {
        BinLogReader br = new BinLogReader("127.0.0.1", 3306, "root", "mjak123");
        if (br.connectMysql()) {
            br.slaveRegister();
        }
    }

    public static void main(String args[]) throws IOException, InterruptedException {
//        fileWatcher("C:\\wamp\\mysql\\data\\mysqlbin-log.000001");
          slaveWatcher();
    }

}
