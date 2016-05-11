package org.greenrobot.eventbus;

import com.google.gson.JsonObject;

/**
 * Created by Glen on 2016/5/10.
 */
public interface OnMethodCallBack {
    void onSuccess(JsonObject msg);

    void onFaile(JsonObject msg, Exception e);
}
