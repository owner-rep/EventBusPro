# EventBusPro
## 在res的xml文件中声明服务列表service.xml，如下：
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
## 在Application中注册服务
```
EventBus.getDefault().register(this, R.xml.service);
```

## 调用服务，执行方法
```
EventBus.getDefault().call(this, "method/login", "{'id':'11111', 'type':2222}", null);
```

## 调用服务打开页面
```
EventBus.getDefault().open(this, "page/share", "{'url':'11111', 'content':2222}");
```
