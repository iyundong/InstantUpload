package cn.rainx.ptp.db;

import com.orm.SugarApp;
import com.orm.SugarRecord;
import com.orm.dsl.Unique;

/**
 * Created by rainx on 2017/8/26.
 */

public class Uuid extends SugarRecord {
    @Unique
    String key;
    String value;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
