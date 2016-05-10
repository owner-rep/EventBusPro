## EventBusPro
### 在res的xml文件中声明服务列表service.xml，如下：
```
<?xml version="1.0" encoding="utf-8"?>
<service-list>
    <service url="method" class="org.greenrobot.eventbus.example.Plug">
        <method id="login" name="startLogin">
            <data id="id" key="userId" type="java.lang.String" />
            <data id="type" key="loginType" type="java.lang.Integer" />
        </method>
    </service>

    <service url="page" class="org.greenrobot.eventbus.example.TestActivity">
        <page id="share" requestCode="123">
            <bundle id="1" key="url" type="java.lang.String" />
            <bundle id="2" key="content" type="java.lang.Integer" />
        </page>
        <page id="share_to_wx" requestCode="123">
            <bundle id="url" key="url" type="java.lang.String" />
            <bundle id="content" key="content" type="java.lang.String" />
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
    public static void startLogin(Context context, OnInternalCallBack callBack, @Params("userId") String id, @Params("loginType") Integer type) {
        Log.d("success", id + type);
        if (callBack != null) {
            callBack.onSuccess(id + type);
        }
    }
}
```

### 调用服务，执行方法
```
EventBus.getDefault().call(this, "method/login", "{'id':'11111', 'type':2222}", null);
```

### 调用服务打开页面
```
EventBus.getDefault().open(this, "page/share", "{'url':'11111', 'content':2222}");
```
