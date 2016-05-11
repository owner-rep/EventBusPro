package org.greenrobot.eventbus;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Glen on 2016/5/10.
 */
public class Method {
    private String id;
    private String name;
    private List<Data> datas;

    public Method(String id, String name) {
        this.id = id;
        this.name = name;
        datas = new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<Data> getDatas() {
        return datas;
    }

    @Override
    public String toString() {
        return "Method{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", datas=" + datas +
                '}';
    }

    public static class Data {
        private String id;
        private String key;
        private Class<?> type;

        public Data(String id, String key, Class<?> type) {
            this.id = id;
            this.key = key;
            this.type = type;
        }

        public String getId() {
            return id;
        }

        public String getKey() {
            return key;
        }

        public Class<?> getType() {
            return type;
        }

        @Override
        public String toString() {
            return "Data{" +
                    "id='" + id + '\'' +
                    ", key='" + key + '\'' +
                    ", type=" + type +
                    '}';
        }
    }
}
