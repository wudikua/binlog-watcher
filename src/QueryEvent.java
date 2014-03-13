
/**
 * Created with IntelliJ IDEA.
 * User: mengjun
 * Date: 14-3-10
 * Time: 下午1:39
 * To change this template use File | Settings | File Templates.
 */
public class QueryEvent {

    public long threadId;

    public long elapsedTime;

    public int databaseNameLength;

    public int errorCode;

    public int statusVariablesLength;

    public String databaseName;

    public String sql;

    @Override
    public String toString() {
        String s = new String("database:"+this.databaseName+"sql: "+this.sql);
        return s;
    }
}
