import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created with IntelliJ IDEA.
 * User: mengjun
 * Date: 14-3-10
 * Time: 上午10:05
 * To change this template use File | Settings | File Templates.
 */
public class XInputStreamImpl extends InputStream implements XInputStream {
    private int head = 0;
    private int tail = 0;
    private int readCount = 0;
    private int readLimit = 0;
    private final byte[] buffer;
    private final InputStream is;

    public XInputStreamImpl(InputStream is) {
        this(is, 512 *  1024);
    }

    public XInputStreamImpl(InputStream is, int size) {
        this.is = is;
        this.buffer = new byte[size];
    }

    @Override
    public boolean hasMore() throws IOException {
        if(this.head < this.tail) return true;
        return this.available() > 0;
    }

    @Override
    public void setReadLimit(int limit) throws IOException {
        this.readCount = 0;
        this.readLimit = limit;
    }

    public int readInt(final int length) throws IOException {
        int r = 0;
        for(int i = 0; i < length; ++i) {
            final int v = this.read();
            r |= (v << (i << 3));
        }
        return r;
    }

    public long readLong(final int length) throws IOException {
        long r = 0;
        for(int i = 0; i < length; ++i) {
            final long v = this.read();
            r |= (v << (i << 3));
        }
        return r;
    }

    public byte[] readBytes(final int length) throws IOException {
        final byte[] r = new byte[length];
        this.read(r, 0, length);
        return r;
    }

    public StringColumn readNullTerminatedString() throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        while(true) {
            final int v = this.read();
            bos.write((byte) v);
            if(v == 0) break;
        }
        return new StringColumn(bos.toByteArray());
    }

    @Override
    public long skip(final long n) throws IOException {
        if(this.readLimit > 0 && (this.readCount + n) > this.readLimit) {
            this.readCount += doSkip(this.readLimit - this.readCount);
            throw new IOException();
        } else {
            this.readCount += doSkip(n);
            return n; // always skip the number of bytes specified by parameter "n"
        }
    }

    @Override
    public int available() throws IOException {
        if(this.readLimit > 0) {
            return this.readLimit - this.readCount;
        } else {
            return this.tail - this.head + this.is.available();
        }
    }

    @Override
    public int read() throws IOException {
        if(this.readLimit > 0 && (this.readCount + 1) > this.readLimit) {
            throw new IOException();
        } else {
            if(this.head >= this.tail) doFill();
            final int r = this.buffer[this.head++] & 0xFF;
            ++this.readCount;
            return r;
        }
    }

    @Override
    public int read(final byte b[], final int off, final int len) throws IOException {
        if(this.readLimit > 0 && (this.readCount + len) > this.readLimit) {
            this.readCount += doRead(b, off, this.readLimit - this.readCount);
            throw new IOException();
        } else {
            this.readCount += doRead(b, off, len);
            return len; // always read the number of bytes specified by parameter "len"
        }
    }

    private void doFill() throws IOException {
        this.head = 0;
        this.tail = this.is.read(this.buffer, 0, this.buffer.length);
        if(this.tail <= 0) throw new EOFException();
    }

    private long doSkip(final long n) throws IOException {
        long total = n;
        while(total > 0) {
            final int availabale = this.tail - this.head;
            if(availabale >= total) {
                this.head += total;
                break;
            } else {
                total -= availabale;
                doFill();
            }
        }
        return n;
    }

    private int doRead(final byte[] b, final int off, final int len) throws IOException {
        int total = len;
        int index = off;
        while(total > 0) {
            final int available = this.tail - this.head;
            if(available >= total) {
                System.arraycopy(this.buffer, this.head, b, index, total);
                this.head += total;
                break;
            } else {
                System.arraycopy(this.buffer, this.head, b, index, available);
                index += available;
                total -= available;
                doFill();
            }
        }
        return len;
    }
}
