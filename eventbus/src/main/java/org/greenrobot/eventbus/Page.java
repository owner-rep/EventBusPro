package org.greenrobot.eventbus;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Glen on 2016/5/10.
 */
public class Page {
    private String id;
    private Integer requestCode;
    private List<Bundle> bundles;

    public Page(String id, Integer requestCode) {
        this.id = id;
        this.requestCode = requestCode;
        bundles = new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public Integer getRequestCode() {
        return requestCode;
    }

    public List<Bundle> getBundles() {
        return bundles;
    }

    @Override
    public String toString() {
        return "Page{" +
                "id='" + id + '\'' +
                ", requestCode=" + requestCode +
                ", bundles=" + bundles +
                '}';
    }

    public static class Bundle {
        private String id;
        private String key;
        private Class<? extends Serializable> type;

        public Bundle(String id, String key, Class<? extends Serializable> type) {
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

        public Class<? extends Serializable> getType() {
            return type;
        }

        @Override
        public String toString() {
            return "Bundle{" +
                    "id='" + id + '\'' +
                    ", key='" + key + '\'' +
                    ", type=" + type +
                    '}';
        }
    }
}
