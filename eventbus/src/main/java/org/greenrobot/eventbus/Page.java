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
    private List<Bundle> bundleList;

    public Page(String id, Integer requestCode) {
        this.id = id;
        this.requestCode = requestCode;
        bundleList = new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public Integer getRequestCode() {
        return requestCode;
    }

    public List<Bundle> getBundleList() {
        return bundleList;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) return false;
        return toString().equals(o.toString());
    }

    @Override
    public String toString() {
        return "Page{" +
                "id='" + id + '\'' +
                ", requestCode=" + requestCode +
                ", bundleList=" + bundleList +
                '}';
    }

    public static class Bundle {
        private String id;
        private String key;
        private Class<? extends Serializable> type;
        private Boolean isNull;

        public Bundle(String id, String key, Class<? extends Serializable> type, Boolean isNull) {
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

        public Boolean getNull() {
            return isNull;
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
                    ", isNull=" + isNull +
                    '}';
        }
    }
}
