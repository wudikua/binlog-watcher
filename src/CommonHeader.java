import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created with IntelliJ IDEA.
 * User: mengjun
 * Date: 14-3-6
 * Time: 上午11:25
 * To change this template use File | Settings | File Templates.
 */
public class CommonHeader {

    public long timeStamp;

    public int type;

    public long serviceId;

    public int eventLength;

    public long nextPosition;

    public int flag;

    @Override
    public String toString() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String sd = sdf.format(new Date(this.timeStamp));
        String s = new String("time:"+sd+" type:"+this.type+" serviceId;"+this.serviceId+" nextPosition:"+this.nextPosition+" flag:"+this.flag);
        return s;
    }
}
