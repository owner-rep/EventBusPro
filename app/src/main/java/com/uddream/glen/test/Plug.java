package com.uddream.glen.test;

import android.content.Context;
import android.util.Log;

import com.google.gson.JsonObject;

import org.greenrobot.eventbus.OnMethodCallBack;
import org.greenrobot.eventbus.annotation.Params;
import org.greenrobot.eventbus.annotation.Service;

/**
 * Created by Glen on 2016/5/9.
 */
@Service
public class Plug {

    /**
     * @param type
     */
    @SuppressWarnings("unused")
    public static void startLogin(Context context, OnMethodCallBack callBack, @Params("userId") String id, @Params("loginType") Integer type) {
        Log.e("success", id + type);
        if (callBack != null) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("id", id);
            jsonObject.addProperty("type", type);
            callBack.onSuccess(jsonObject);
        }
    }
}