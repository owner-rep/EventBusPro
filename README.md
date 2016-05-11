## EventBusPro
### 在res的xml文件中声明服务列表service.xml，如下：
```
<?xml version="1.0" encoding="utf-8"?>
<service-list>
    <service url="method" class="com.uddream.glen.test.Plug">
        <method id="login" name="startLogin">
            <data id="id" key="userId" type="java.lang.String" />
            <data id="type" key="loginType" type="java.lang.Integer" />
        </method>
    </service>

    <service url="page" class="com.uddream.glen.test.TestActivity">
        <page id="share" requestCode="123">
            <bundle id="url" key="url2" type="java.lang.String" />
            <bundle id="content" key="content2" type="java.lang.String" />
        </page>
        <page id="share_to_wx" requestCode="123">
            <bundle id="url" key="url" type="java.lang.String" />
            <bundle id="content" key="content" type="java.lang.Integer" />
        </page>
    </service>
</service-list>
```
### 在Application中注册服务
```
EventBus.getDefault().register(this, R.xml.service);
```

### 服务实现方式
```
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
```

### 调用服务，执行方法
```
JsonObject params = new JsonObject();
params.addProperty("id", "123");
params.addProperty("type", 456);
EventBus.getDefault().call(this, new EUrl("method", "login"), params, new OnMethodCallBack() {
    @Override
    public void onSuccess(JsonObject msg) {
        Log.d("onSuccess", msg.toString());
    }

    @Override
    public void onFailure(JsonObject msg, Exception e) {

    }
});
```

### 调用服务打开页面
```
JsonObject params = new JsonObject();
params.addProperty("url", "http://blog.uddream.cn");
params.addProperty("content", "hello blog by uddream");
EventBus.getDefault().open(this, new EUrl("page", "share"), params);
```
