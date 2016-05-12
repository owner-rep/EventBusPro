package org.greenrobot.eventbus;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Glen on 2016/5/10.
 */
public class Method {
    private String id;
    private String name;
    private List<Data> dataList;

    public Method(String id, String name) {
        this.id = id;
        this.name = name;
        dataList = new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<Data> getDataList() {
        return dataList;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) return false;
        return toString().equals(o.toString());
    }

    @Override
    public String toString() {
        return "Method{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", dataList=" + dataList +
                '}';
    }

    public static class Data {
        private String id;
        private String key;
        private Class<? extends Serializable> type;
        private Boolean isNull;

        public Data(String id, String key, Class<? extends Serializable> type, boolean isNull) {
            this.id = id;
            this.key = key;
            this.type = type;
            this.isNull = isNull;
        }

        public String getId() {
            return id;
        }

        public String getKey() {
            return key;
        }

        public Class<? extends Serializable> getType() {
            return type;
        }

        public Boolean getNull() {
            return isNull;
        }

        @Override
        public String toString() {
            return "Data{" +
                    "id='" + id + '\'' +
                    ", key='" + key + '\'' +
                    ", type=" + type +
                    ", isNull=" + isNull +
                    '}';
        }
    }
}
