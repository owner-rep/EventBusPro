## EventBusPro
### 在res的xml文件中声明服务列表service.xml，如下：
```
<?xml version="1.0" encoding="utf-8"?>
<service-list>
    <service url="method" class="com.uddream.glen.test.Plug">
        <method id="login" name="startLogin">
            <data id="id" key="userId" type="java.lang.String" isNull="true"/>
            <data id="type" key="loginType" type="java.lang.Integer" />
            <data id="model" key="m" type="com.uddream.glen.test.Model2" />
        </method>
    </service>

    <service url="page" class="com.uddream.glen.test.TestActivity">
        <page id="share" requestCode="123">
            <bundle id="url" key="url2" type="java.lang.String"  isNull="true"/>
            <bundle id="content" key="content2" type="java.lang.String" />
        </page>
        <page id="share_2" requestCode="123">
            <bundle id="url" key="url" type="java.lang.String" />
            <bundle id="content" key="content" type="java.lang.Integer" />
        </page>
    </service>
</service-list>
```

### 在Application中注册服务,每个提供服务的子工程都需要进行服务注册，按照上述xml形式进行声明，使用下面代码进行注册
```
EventBus.getDefault().register(this, R.xml.service);
```

### 工程A，对外提供服务，具体服务实现方式，必须声明公开静态方法，类上加Service注解
```
//工程A向外提供方法服务
@Service
public class Plug {

    /**
     * @param type
     */
    @SuppressWarnings("unused")
    public static void startLogin(Context context, OnMethodCallBack callBack, @Params("m") Model2 model2, @Params("userId") String id, @Params("loginType") Integer type) {
        Log.e("success", id + type + model2);
        if (callBack != null) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("id", id);
            jsonObject.addProperty("type", type);
            jsonObject.add("model2", new Gson().toJsonTree(model2));
            callBack.onSuccess(jsonObject);
        }
    }
}

// 工程A提供页面服务
public class TestActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getIntent() != null) {
            Bundle bundle = getIntent().getExtras();
            if (bundle != null) {
                Log.e("bundle url", bundle.getString("url2"));
                Log.e("bundle content", bundle.getString("content2"));
            }
        }
    }
}
```

### 工程B，调用工程A对外提供的服务，执行方法
```
 JsonObject params = new JsonObject();
            params.add("model", new Gson().toJsonTree(new Model1("glen", "123456")));
            params.addProperty("id", "123");
            params.addProperty("type", 456);
            EventBus.getDefault().call(context, new EUrl("method", "login"), params, new OnMethodCallBack() {
                @Override
                public void onSuccess(JsonObject msg) {
                    Log.e("onSuccess", msg.toString());
                }

                @Override
                public void onFailure(JsonObject msg, Exception e) {

                }
            });
}
```

### 工程C，调用工程A对外提供的服务，打开页面
```
JsonObject params = new JsonObject();
params.addProperty("url", "http://blog.uddream.cn");
params.addProperty("content", "hello blog by uddream");
EventBus.getDefault().open(this, new EUrl("page", "share"), params);
```

## EventBus 订阅者服务
```
跟原有的EventBuse总线一样，采用如下方案进行注册和解除注册
EventBus.getDefault().register(this);
EventBus.getDefault().unregister(this);
```
```
//通过url来定义资源，原来是通过参数类型
@Subscribe(url = "test",  threadMode = ThreadMode.MAIN)
public void onReceive(JsonObject receiver) {
    Log.e("onReceive", receiver.toString() + "ui thread: " + (Looper.getMainLooper() == Looper.myLooper()));
}
```
```
//发送和接收订阅事件都是采用JsonObject对象传递
if (EventBus.getDefault().hasSubscriberForEvent("test")) {
    EventBus.getDefault().post("test", new JsonObject());
}
```