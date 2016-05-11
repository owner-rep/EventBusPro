package com.uddream.glen.test;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import com.google.gson.JsonObject;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.OnMethodCallBack;
import org.greenrobot.eventbus.ThreadMode;
import org.greenrobot.eventbus.annotation.Subscribe;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        EventBus.getDefault().register(this, R.xml.service);

        EventBus.getDefault().register(this);
    }

    @Override
    protected void onDestroy() {
        EventBus.getDefault().unregister(this);
        super.onDestroy();
    }

    @Subscribe(url = "test", priority = 1, threadMode = ThreadMode.BACKGROUND)
    @SuppressWarnings("unused")
    public void onReceive(JsonObject receiver) {
        Log.e("onReceive", receiver.toString());
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.post:
                EventBus.getDefault().post("test", new JsonObject());
                break;
            case R.id.open: {
                JsonObject params = new JsonObject();
                params.addProperty("url", "http://blog.uddream.cn");
                params.addProperty("content", "hello blog by uddream");
                EventBus.getDefault().open(this, "page/share", params);
            }
            break;
            case R.id.method: {
                JsonObject params = new JsonObject();
                params.addProperty("id", "123");
                params.addProperty("type", 456);
                EventBus.getDefault().call(this, "method/login", params, new OnMethodCallBack() {
                    @Override
                    public void onSuccess(JsonObject msg) {
                        Log.d("onSuccess", msg.toString());
                    }

                    @Override
                    public void onFaile(JsonObject msg, Exception e) {

                    }
                });
            }
            break;
        }
    }
}
