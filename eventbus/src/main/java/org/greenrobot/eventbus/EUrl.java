package org.greenrobot.eventbus;

import java.io.Serializable;

/**
 * Created by Glen on 2016/5/11.
 */
public class EUrl implements Serializable {
    private String url;
    private String id;

    public EUrl(String url, String id) {
        this.url = url;
        this.id = id;
    }

    public String getUrl() {
        return url;
    }

    public String getId() {
        return id;
    }
}
