package com.uddream.glen.test;

import android.content.Context;
import android.util.Log;

import com.google.gson.JsonObject;

import org.greenrobot.eventbus.OnMethodCallBack;
import org.greenrobot.eventbus.annotation.Params;

/**
 * Created by Glen on 2016/5/9.
 */
public class Plug {

    /**
     * @param context  第一个参数必须为context
     * @param callBack 第二个参数必须为callback
     * @param id       从这往后可以再追加最多五个参数，必须为对象，不能出现类似int、floag、double等
     * @param type
     */
    @SuppressWarnings("unused")
    public static void startLogin(Context context, OnMethodCallBack callBack, @Params("userId") String id, @Params("loginType") Integer type) {
        Log.d("success", id + type);
        if (callBack != null) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("id", id);
            jsonObject.addProperty("type", type);
            callBack.onSuccess(jsonObject);
        }
    }
}