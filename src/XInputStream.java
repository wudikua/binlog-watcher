
import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 * User: mengjun
 * Date: 14-3-10
 * Time: 上午9:58
 * To change this template use File | Settings | File Templates.
 */
public interface XInputStream {
    /**
     *
     */
    void close() throws IOException;

    int available() throws IOException;

    boolean hasMore() throws IOException;

    void setReadLimit(int limit) throws IOException;

    /**
     *
     */
    long skip(long n) throws IOException;

    int readInt(int length) throws IOException;

    long readLong(int length) throws IOException;

    byte[] readBytes(int length) throws IOException;

}
