import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 * User: mengjun
 * Date: 14-3-12
 * Time: 下午5:15
 * To change this template use File | Settings | File Templates.
 */
public interface XOutputStream {

    void writeInt(int value, int length) throws IOException;

    void writeLong(long value, int length) throws IOException;

}
