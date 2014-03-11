import java.io.Serializable;

/**
 * Created with IntelliJ IDEA.
 * User: mengjun
 * Date: 14-3-11
 * Time: 下午2:40
 * To change this template use File | Settings | File Templates.
 */
public class StringColumn implements Serializable {
    private static final long serialVersionUID = 1009717372407166422L;
    private final byte[] value;

    public StringColumn(byte[] value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return new String(this.value);
    }
}
