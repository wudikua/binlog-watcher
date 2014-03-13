import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Created with IntelliJ IDEA.
 * User: mengjun
 * Date: 14-3-12
 * Time: 下午5:41
 * To change this template use File | Settings | File Templates.
 */
public class XOutputStreamImpl extends BufferedOutputStream implements XOutputStream {

    public XOutputStreamImpl(OutputStream out, int size) {
        super(out, size);
    }

    public XOutputStreamImpl(OutputStream out) {
        super(out);
    }

    @Override
    public void writeInt(int value, int length) throws IOException {
        for(int i = 0; i < length; i++) {
            super.write(0x000000FF & (value >>> (i << 3)));
        }
    }

    @Override
    public void writeLong(long value, int length) throws IOException {
        for(int i = 0; i < length; i++) {
            super.write((int)(0x00000000000000FF & (value >>> (i << 3))));
        }
    }

    public void writeBytes(int value, int length) throws IOException {
        for(int i = 0; i < length; i++) {
            super.write(value);
        }
    }

    public void writeBytes(byte[] value) throws IOException {
        super.write(value, 0, value.length);
    }

}
