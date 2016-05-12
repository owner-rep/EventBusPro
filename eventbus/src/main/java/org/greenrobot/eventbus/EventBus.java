/*
 * Copyright (C) 2012-2016 Markus Junginger, greenrobot (http://greenrobot.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.greenrobot.eventbus;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.XmlResourceParser;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

import org.greenrobot.eventbus.annotation.Params;
import org.greenrobot.eventbus.annotation.Subscribe;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;

/**
 * EventBus is a central publish/subscribe event system for Android. Events are posted ({@link #post(String, JsonObject)}) to the
 * bus, which delivers it to subscribers that have a matching handler method for the event type. To receive events,
 * subscribers must register themselves to the bus using {@link #register(Object)}. Once registered, subscribers
 * receive events until {@link #unregister(Object)} is called. Event handling methods must be annotated by
 * {@link Subscribe}, must be public, return nothing (void), and have exactly one parameter
 * (the event).
 *
 * @author Markus Junginger, greenrobot
 */
public class EventBus {

    /**
     * Log tag, apps may override it.
     */
    public static String TAG = "EventBus";

    static volatile EventBus defaultInstance;

    private static final EventBusBuilder DEFAULT_BUILDER = new EventBusBuilder();
    private static final Map<Class<?>, List<Class<?>>> eventTypesCache = new HashMap<>();

    private final List<Service> serviceList = new ArrayList<>();
    private final Map<String, CopyOnWriteArrayList<Subscription>> subscriptionsByEventType;
    private final Map<Object, List<String>> typesBySubscriber;

    private final ThreadLocal<PostingThreadState> currentPostingThreadState = new ThreadLocal<PostingThreadState>() {
        @Override
        protected PostingThreadState initialValue() {
            return new PostingThreadState();
        }
    };

    private final HandlerPoster mainThreadPoster;
    private final BackgroundPoster backgroundPoster;
    private final AsyncPoster asyncPoster;
    private final SubscriberMethodFinder subscriberMethodFinder;
    private final ExecutorService executorService;

    private final boolean logNoSubscriberMessages;

    /**
     * Convenience singleton for apps using a process-wide EventBus instance.
     */
    public static EventBus getDefault() {
        if (defaultInstance == null) {
            synchronized (EventBus.class) {
                if (defaultInstance == null) {
                    defaultInstance = new EventBus();
                }
            }
        }
        return defaultInstance;
    }

    public static EventBusBuilder builder() {
        return new EventBusBuilder();
    }

    /**
     * For unit test primarily.
     */
    public static void clearCaches() {
        SubscriberMethodFinder.clearCaches();
        eventTypesCache.clear();
    }

    /**
     * Creates a new EventBus instance; each instance is a separate scope in which events are delivered. To use a
     * central bus, consider {@link #getDefault()}.
     */
    public EventBus() {
        this(DEFAULT_BUILDER);
    }

    EventBus(EventBusBuilder builder) {
        subscriptionsByEventType = new HashMap<>();
        typesBySubscriber = new HashMap<>();
        mainThreadPoster = new HandlerPoster(this, Looper.getMainLooper(), 10);
        backgroundPoster = new BackgroundPoster(this);
        asyncPoster = new AsyncPoster(this);
        subscriberMethodFinder = new SubscriberMethodFinder();
        logNoSubscriberMessages = builder.logNoSubscriberMessages;
        executorService = builder.executorService;
    }

    /**
     * Registers the given subscriber to receive events. Subscribers must call {@link #unregister(Object)} once they
     * are no longer interested in receiving events.
     * <p/>
     * Subscribers have event handling methods that must be annotated by {@link Subscribe}.
     * The {@link Subscribe} annotation also allows configuration like {@link
     * ThreadMode} and priority.
     */
    public void register(Object subscriber) {
        Class<?> subscriberClass = subscriber.getClass();
        List<SubscriberMethod> subscriberMethods = subscriberMethodFinder.findSubscriberMethods(subscriberClass);
        synchronized (this) {
            for (SubscriberMethod subscriberMethod : subscriberMethods) {
                subscribe(subscriber, subscriberMethod);
            }
        }
    }

    /**
     * <p/>
     * register system service
     *
     * @param context
     * @param xmId
     */
    public void register(Context context, int xmId) {
        XmlResourceParser parser = context.getResources().getXml(xmId);
        try {
            Service service = null;
            Method method = null;
            Method.Data data = null;
            Page page = null;
            Page.Bundle bundle = null;

            int event = parser.getEventType();//产生第一个事件
            while (event != XmlPullParser.END_DOCUMENT) {
                switch (event) {
                    case XmlPullParser.START_TAG://判断当前事件是否是标签元素开始事件
                        if (parser.getName().equalsIgnoreCase("service")) {
                            service = new Service(
                                    parser.getAttributeValue(null, "url"),
                                    Class.forName(parser.getAttributeValue(null, "class")));
                            if (service.getUrl() == null)
                                throw new EventBusException("Service[" + service + "] url not null");
                        } else if (parser.getName().equalsIgnoreCase("method")) {
                            method = new Method(
                                    parser.getAttributeValue(null, "id"),
                                    parser.getAttributeValue(null, "name"));
                            if (method.getId() == null || method.getName() == null)
                                throw new EventBusException("Service.Method[" + method + "] id or name not null");
                            if (!isExistPublicStaticMethod(service.getClazz(), method.getName())) {
                                throw new EventBusException("Service.Method[" + method + "] not find public static method, the method name is " + method.getName());
                            }
                        } else if (parser.getName().equalsIgnoreCase("data")) {
                            data = new Method.Data(
                                    parser.getAttributeValue(null, "id"),
                                    parser.getAttributeValue(null, "key"),
                                    (Class<? extends Serializable>) Class.forName(parser.getAttributeValue(null, "type")),
                                    parser.getAttributeBooleanValue(null, "isNull", true));
                            if (data.getId() == null || data.getKey() == null)
                                throw new EventBusException("Service.Method.Data[" + data + "] id or key not null");
                        } else if (parser.getName().equalsIgnoreCase("page")) {
                            page = new Page(
                                    parser.getAttributeValue(null, "id"),
                                    parser.getAttributeIntValue(null, "requestCode", -1));
                            if (page.getId() == null)
                                throw new EventBusException("Service.Page[" + page + "] id  not null");
                        } else if (parser.getName().equalsIgnoreCase("bundle")) {
                            bundle = new Page.Bundle(parser.getAttributeValue(null, "id"),
                                    parser.getAttributeValue(null, "key"),
                                    (Class<? extends Serializable>) Class.forName(parser.getAttributeValue(null, "type")),
                                    parser.getAttributeBooleanValue(null, "isNull", true));
                            if (bundle.getId() == null || bundle.getKey() == null)
                                throw new EventBusException("Service.Page.Bundle[" + bundle + "] id or key not null");
                        }
                        break;
                    case XmlPullParser.END_TAG://判断当前事件是否是标签元素结束事件
                        if (parser.getName().equalsIgnoreCase("service")) {
                            if (serviceList.contains(service)) break;

                            if (findServiceByUrl(service.getUrl()) == null) {
                                serviceList.add(service);
                                service = null;
                            } else {
                                throw new EventBusException("Service[" + service + "] the url is already registered");
                            }
                        } else if (parser.getName().equalsIgnoreCase("method")) {
                            if (service.getMethods().contains(method)) break;

                            if (findMethodById(service, method.getId()) == null) {
                                service.getMethods().add(method);
                                method = null;
                            } else {
                                throw new EventBusException("Service.Method[" + method + "] the id already registered");
                            }
                        } else if (parser.getName().equalsIgnoreCase("data")) {
                            for (Method.Data item : method.getDataList()) {
                                if (item.getKey().equals(data.getKey()) || item.getId().equals(data.getId())) {
                                    throw new EventBusException("Service.Method.Data[" + data + "] the id or key already registered");
                                }
                            }

                            method.getDataList().add(data);
                            data = null;
                        } else if (parser.getName().equalsIgnoreCase("page")) {
                            if (service.getPages().contains(page)) break;

                            if (findPageById(service, page.getId()) == null) {
                                service.getPages().add(page);
                                page = null;
                            } else {
                                throw new EventBusException("Service.Page[" + page + "] the id already registered");
                            }
                        } else if (parser.getName().equalsIgnoreCase("bundle")) {
                            for (Page.Bundle item : page.getBundleList()) {
                                if (item.getKey().equals(bundle.getKey()) || item.getId().equals(bundle.getId())) {
                                    throw new EventBusException("Service.Page.Bundle[" + bundle + "] the id or key already registered");
                                }
                            }

                            page.getBundleList().add(bundle);
                            bundle = null;
                        }
                        break;
                }
                event = parser.next();//进入下一个元素
            }
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            throw new EventBusException(e);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Log.d(TAG, "Length:" + serviceList.size() + " Service：" + serviceList.toString());
    }

    private boolean isExistPublicStaticMethod(Class<?> clazz, String name) throws EventBusException {
        if (clazz != null && !clazz.isAnnotationPresent(org.greenrobot.eventbus.annotation.Service.class)) {
            throw new EventBusException("Class[" + clazz.getName() + "] not add Service Annotation");
        }
        try {
            while (clazz != null) {
                java.lang.reflect.Method[] methods = clazz.getDeclaredMethods();
                for (java.lang.reflect.Method item : methods) {
                    if (name.equals(item.getName())) {
                        int modifiers = item.getModifiers();
                        if ((modifiers & Modifier.PUBLIC) != 0 && (modifiers & Modifier.STATIC) != 0) {
                            return true;
                        }
                    }
                }
                clazz = clazz.getSuperclass();
            }
        } catch (Exception e) {
            Log.w(TAG, e.getMessage());
        }
        return false;
    }

    // Must be called in synchronized block
    private void subscribe(Object subscriber, SubscriberMethod subscriberMethod) {
        String eventType = subscriberMethod.eventType;
        Subscription newSubscription = new Subscription(subscriber, subscriberMethod);
        CopyOnWriteArrayList<Subscription> subscriptions = subscriptionsByEventType.get(eventType);
        if (subscriptions == null) {
            subscriptions = new CopyOnWriteArrayList<>();
            subscriptionsByEventType.put(eventType, subscriptions);
        } else {
            if (subscriptions.contains(newSubscription)) {
                throw new EventBusException("Subscriber " + subscriber.getClass() + " already registered to event "
                        + eventType);
            }
        }

        int size = subscriptions.size();
        for (int i = 0; i <= size; i++) {
            if (i == size || subscriberMethod.priority > subscriptions.get(i).subscriberMethod.priority) {
                subscriptions.add(i, newSubscription);
                break;
            }
        }

        List<String> subscribedEvents = typesBySubscriber.get(subscriber);
        if (subscribedEvents == null) {
            subscribedEvents = new ArrayList<>();
            typesBySubscriber.put(subscriber, subscribedEvents);
        }
        subscribedEvents.add(eventType);
    }

    public synchronized boolean isRegistered(Object subscriber) {
        return typesBySubscriber.containsKey(subscriber);
    }

    /**
     * Only updates subscriptionsByEventType, not typesBySubscriber! Caller must update typesBySubscriber.
     */
    private void unSubscribeByEventType(Object subscriber, String eventType) {
        List<Subscription> subscriptions = subscriptionsByEventType.get(eventType);
        if (subscriptions != null) {
            int size = subscriptions.size();
            for (int i = 0; i < size; i++) {
                Subscription subscription = subscriptions.get(i);
                if (subscription.subscriber == subscriber) {
                    subscription.active = false;
                    subscriptions.remove(i);
                    i--;
                    size--;
                }
            }
        }
    }

    /**
     * Unregisters the given subscriber from all event classes.
     */
    public synchronized void unregister(Object subscriber) {
        List<String> subscribedTypes = typesBySubscriber.get(subscriber);
        if (subscribedTypes != null) {
            for (String eventType : subscribedTypes) {
                unSubscribeByEventType(subscriber, eventType);
            }
            typesBySubscriber.remove(subscriber);
        } else {
            Log.w(TAG, "Subscriber to unregister was not registered before: " + subscriber.getClass());
        }
    }

    /**
     * Posts the given event to the event bus.
     */
    public void post(String url, JsonObject event) {
        PostingThreadState postingState = currentPostingThreadState.get();
        List<PostEvent> eventQueue = postingState.eventQueue;
        eventQueue.add(new PostEvent(url, event));

        if (!postingState.isPosting) {
            postingState.isMainThread = Looper.getMainLooper() == Looper.myLooper();
            postingState.isPosting = true;
            if (postingState.canceled) {
                throw new EventBusException("Internal error. Abort state was not reset");
            }
            try {
                while (!eventQueue.isEmpty()) {
                    postSingleEvent(eventQueue.remove(0), postingState);
                }
            } finally {
                postingState.isPosting = false;
                postingState.isMainThread = false;
            }
        }
    }

    /**
     * call the register service by the event bus.
     */
    public void call(Context context, EUrl url, JsonObject jsonObject, OnMethodCallBack callBack) {
        try {
            Service service = findServiceByUrl(url.getUrl());
            if (service == null)
                throw new EventBusException("EventBus Call Method, but not find Service by url[" + url.getUrl() + "]");
            Method method = findMethodById(service, url.getId());
            if (method == null) {
                throw new EventBusException("EventBus Call Method, but not find Service.Method by id[" + url.getId() + "] from " + service);
            }
            String mName = method.getName();
            Class<?> clazz = service.getClazz();
            List<Method.Data> dataList = method.getDataList();

            List<Object> params = new ArrayList<>();
            java.lang.reflect.Method execMethod = findExecMethod(clazz, mName, context, callBack, jsonObject, dataList, params);
            if (execMethod == null) {
                throw new EventBusException("EventBus Call Method, but not find ExecMethod by name and paramList[" + method.getName() + "] from " + service);
            }

            switch (params.size()) {
                case 0:
                    execMethod.invoke(null);
                    break;
                case 1:
                    execMethod.invoke(null, params.get(0));
                    break;
                case 2:
                    execMethod.invoke(null, params.get(0), params.get(1));
                    break;
                case 3:
                    execMethod.invoke(null, params.get(0), params.get(1), params.get(2));
                    break;
                case 4:
                    execMethod.invoke(null, params.get(0), params.get(1), params.get(2), params.get(3));
                    break;
                case 5:
                    execMethod.invoke(null, params.get(0), params.get(1), params.get(2), params.get(3), params.get(4));
                    break;
                case 6:
                    execMethod.invoke(null, params.get(0), params.get(1), params.get(2), params.get(3), params.get(4), params.get(5));
                    break;
                case 7:
                    execMethod.invoke(null, params.get(0), params.get(1), params.get(2), params.get(3), params.get(4), params.get(5), params.get(6));
                    break;
                case 8:
                    execMethod.invoke(null, params.get(0), params.get(1), params.get(2), params.get(3), params.get(4), params.get(5), params.get(6), params.get(7));
                    break;
                case 9:
                    execMethod.invoke(null, params.get(0), params.get(1), params.get(2), params.get(3), params.get(4), params.get(5), params.get(6), params.get(7), params.get(8));
                    break;
                case 10:
                    execMethod.invoke(null, params.get(0), params.get(1), params.get(2), params.get(3), params.get(4), params.get(5), params.get(6), params.get(7), params.get(8), params.get(10));
                    break;
                default:
                    throw new EventBusException("we will very sorry, the method has more 10 params, but you translate " + params.size() + " params for " + execMethod.getName());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void call(EUrl url, JsonObject params) {
        this.call(null, url, params, null);
    }

    public void call(EUrl url, JsonObject params, OnMethodCallBack callBack) {
        this.call(null, url, params, callBack);
    }

    public void call(Context context, EUrl url, JsonObject params) {
        this.call(context, url, params, null);
    }


    public void open(Context context, EUrl url, JsonObject jsonObject) {
        try {
            Service service = findServiceByUrl(url.getUrl());
            if (service == null)
                throw new EventBusException("EventBus Open Page, but  not find Service by url[" + url.getUrl() + "]");
            Page page = findPageById(service, url.getId());
            if (page == null) {
                throw new EventBusException("EventBus Open Page, but not find Service.Page by id[" + url.getId() + "] from " + service);
            }
            if (jsonObject == null) {
                for (Page.Bundle item : page.getBundleList()) {
                    if (item.getNull() == false) {
                        throw new EventBusException("Page[" + page + "], the id[" + item.getId() + "] has null value to give " + item.getKey());
                    }
                }
            }

            Intent intent = new Intent(context, service.getClazz());
            Bundle bundle = getBundleByPage(page, jsonObject);
            if (bundle != null) {
                intent.putExtras(bundle);
            }
            if (context instanceof Activity && page.getRequestCode() > 0) {
                ((Activity) context).startActivityForResult(intent, page.getRequestCode());
            } else {
                context.startActivity(intent);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private java.lang.reflect.Method findExecMethod(Class<?> clazz, String name, Context context, OnMethodCallBack callBack, JsonObject json, List<Method.Data> dataList, List<Object> outParams) {
        List<java.lang.reflect.Method> like = null;
        while (clazz != null) {
            java.lang.reflect.Method[] methods = clazz.getDeclaredMethods();
            for (java.lang.reflect.Method method : methods) {
                if (method.getName().equals(name)) {//优先匹配方法名称
                    outParams.clear();
                    int paramsCount = 0;//加过注解的参数个数
                    boolean isSuccess = true;//默认认为声明的服务方法中的参数都可以找到
                    //查找注解参数，未加Params注解的参数只能为Context或者OnMethodCallBack
                    Annotation[][] annotations = method.getParameterAnnotations();
                    Class<?>[] params = method.getParameterTypes();
                    for (int i = 0; annotations != null && i < annotations.length; i++) {
                        Params p = getParamsAnnotation(annotations[i]);
                        if (p == null) {
                            if (Context.class.isAssignableFrom(params[i])) {
                                outParams.add(context);
                            } else if (OnMethodCallBack.class.isAssignableFrom(params[i])) {
                                outParams.add(callBack);
                            } else {
                                isSuccess = false;
                                break;
                            }
                        } else {
                            boolean add = false;
                            for (Method.Data data : dataList) {
                                if (p.value().equalsIgnoreCase(data.getKey())) {
                                    add = true;
                                    paramsCount++;
                                    outParams.add(getValue(data, json));
                                    break;
                                }
                            }
                            if (!add) {
                                isSuccess = false;
                                break;
                            }
                        }
                    }
                    //找到方法，并且找到的方法中加Params注解修饰的参数个数要和xml文件中声明的个数相等，下一版本，这个校验工作放到注册服务时就进行
                    if (isSuccess) {
                        if (paramsCount == dataList.size()) {
                            return method;
                        }
                        like = new ArrayList<>();
                        like.add(method);
                    }
                }
            }
            clazz = clazz.getSuperclass();
        }
        if (like != null) {
            throw new EventBusException("EventBus Call Method, but not find ExecMethod because method params count and xml define params count is not same，please modify under method " + like);
        }
        return null;
    }

    private Params getParamsAnnotation(Annotation[] as) {
        for (int i = 0; as != null && i < as.length; i++) {
            if (as[i] instanceof Params) {
                return (Params) as[i];
            }
        }
        return null;
    }

    private Object getValue(Method.Data data, JsonObject json) {
        if (data == null) return null;

        String id = data.getId();
        Class<?> clazz = data.getType();
        boolean isNull = data.getNull();

        JsonElement value = json == null ? new JsonNull() : json.get(id);
        if (!isNull && value.isJsonNull())
            throw new EventBusException("Method.Data[" + data + "], the id[" + id + "] has null value to give " + data.getType());

        if (!value.isJsonNull()) {
            if (clazz == Boolean.class) {
                return value.getAsBoolean();
            } else if (clazz == Integer.class) {
                return value.getAsInt();
            } else if (clazz == Long.class) {
                return value.getAsLong();
            } else if (clazz == Float.class) {
                return value.getAsFloat();
            } else if (clazz == Double.class) {
                return value.getAsDouble();
            } else if (clazz == String.class) {
                return value.getAsString();
            } else if (clazz == Byte.class) {
                return value.getAsByte();
            } else if (clazz == char.class) {
                return value.getAsCharacter();
            } else {
                return new Gson().fromJson(value, clazz);
            }
        }
        return null;
    }

    private Bundle getBundleByPage(Page page, JsonObject json) throws Exception {
        if (page != null && json != null && page.getBundleList().size() > 0) {
            Bundle bundle = new Bundle();
            for (Page.Bundle item : page.getBundleList()) {
                String id = item.getId();
                String key = item.getKey();
                Class<? extends Serializable> clazz = item.getType();
                boolean isNull = item.getNull();

                JsonElement value = json.get(id);
                if (!isNull && value.isJsonNull())
                    throw new EventBusException("Page.Bundle[" + item + "], the id[" + id + "] has null value to give " + key);

                if (!value.isJsonNull()) {
                    if (clazz == Boolean.class) {
                        bundle.putBoolean(key, value.getAsBoolean());
                    } else if (clazz == Integer.class) {
                        bundle.putInt(key, value.getAsInt());
                    } else if (clazz == Long.class) {
                        bundle.putLong(key, value.getAsLong());
                    } else if (clazz == Float.class) {
                        bundle.putFloat(key, value.getAsFloat());
                    } else if (clazz == Double.class) {
                        bundle.putDouble(key, value.getAsDouble());
                    } else if (clazz == String.class) {
                        bundle.putString(key, value.getAsString());
                    } else if (clazz == Byte.class) {
                        bundle.putByte(key, value.getAsByte());
                    } else if (clazz == char.class) {
                        bundle.putChar(key, value.getAsCharacter());
                    } else {
                        bundle.putSerializable(key, new Gson().fromJson(value, clazz));
                    }
                }
            }
            return bundle;
        }
        return null;
    }

    public boolean hasSubscriberForEvent(String url) {
        CopyOnWriteArrayList<Subscription> subscriptions;
        synchronized (this) {
            subscriptions = subscriptionsByEventType.get(url);
        }
        if (subscriptions != null && !subscriptions.isEmpty()) {
            return true;
        }
        return false;
    }

    private Service findServiceByUrl(String url) {
        if (url != null) {
            for (Service item : serviceList) {
                if (url.equalsIgnoreCase(item.getUrl())) {
                    return item;
                }
            }
        }
        return null;
    }

    private Method findMethodById(Service service, String id) {
        if (service != null && id != null) {
            for (Method item : service.getMethods()) {
                if (id.equalsIgnoreCase(item.getId())) {
                    return item;
                }
            }
        }
        return null;
    }

    private Page findPageById(Service service, String id) {
        if (service != null && id != null) {
            for (Page item : service.getPages()) {
                if (id.equalsIgnoreCase(item.getId())) {
                    return item;
                }
            }
        }
        return null;
    }

    private void postSingleEvent(PostEvent post, PostingThreadState postingState) throws Error {
        CopyOnWriteArrayList<Subscription> subscriptions;
        synchronized (this) {
            subscriptions = subscriptionsByEventType.get(post.url);
        }
        if (subscriptions != null && !subscriptions.isEmpty()) {
            for (Subscription subscription : subscriptions) {
                postingState.event = post;
                postingState.subscription = subscription;
                boolean aborted = false;
                try {
                    postToSubscription(subscription, post, postingState.isMainThread);
                    aborted = postingState.canceled;
                } finally {
                    postingState.event = null;
                    postingState.subscription = null;
                    postingState.canceled = false;
                }
                if (aborted) {
                    break;
                }
            }
        } else {
            if (logNoSubscriberMessages) {
                Log.d(TAG, "No subscribers registered for event " + post.url);
            }
        }
    }

    private void postToSubscription(Subscription subscription, PostEvent post, boolean isMainThread) {
        switch (subscription.subscriberMethod.threadMode) {
            case POSTING:
                invokeSubscriber(subscription, post.event);
                break;
            case MAIN:
                if (isMainThread) {
                    invokeSubscriber(subscription, post.event);
                } else {
                    mainThreadPoster.enqueue(subscription, post.event);
                }
                break;
            case BACKGROUND:
                if (isMainThread) {
                    backgroundPoster.enqueue(subscription, post.event);
                } else {
                    invokeSubscriber(subscription, post.event);
                }
                break;
            case ASYNC:
                asyncPoster.enqueue(subscription, post.event);
                break;
            default:
                throw new IllegalStateException("Unknown thread mode: " + subscription.subscriberMethod.threadMode);
        }
    }

    /**
     * Invokes the subscriber if the subscriptions is still active. Skipping subscriptions prevents race conditions
     * between {@link #unregister(Object)} and event delivery. Otherwise the event might be delivered after the
     * subscriber unregistered. This is particularly important for main thread delivery and registrations bound to the
     * live cycle of an Activity or Fragment.
     */
    void invokeSubscriber(PendingPost pendingPost) {
        Object event = pendingPost.event;
        Subscription subscription = pendingPost.subscription;
        PendingPost.releasePendingPost(pendingPost);
        if (subscription.active) {
            invokeSubscriber(subscription, (JsonObject) event);
        }
    }

    void invokeSubscriber(Subscription subscription, JsonObject event) {
        try {
            subscription.subscriberMethod.method.invoke(subscription.subscriber, event);
        } catch (InvocationTargetException e) {
            Log.e("exception", e.getMessage());
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Unexpected exception", e);
        }
    }

    /**
     * For ThreadLocal, much faster to set (and get multiple values).
     */
    final static class PostingThreadState {
        final List<PostEvent> eventQueue = new ArrayList<>();
        boolean isPosting;
        boolean isMainThread;
        Subscription subscription;
        PostEvent event;
        boolean canceled;
    }

    final static class PostEvent {
        String url;
        JsonObject event;

        public PostEvent(String url, JsonObject event) {
            this.url = url;
            this.event = event;
        }
    }

    ExecutorService getExecutorService() {
        return executorService;
    }
}
