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
import com.google.gson.JsonObject;

import org.greenrobot.eventbus.annotation.Params;
import org.greenrobot.eventbus.annotation.Subscribe;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
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
                            service = new Service(parser.getAttributeValue(null, "url"),
                                    Class.forName(parser.getAttributeValue(null, "class")));
                        } else if (parser.getName().equalsIgnoreCase("method")) {
                            method = new Method(parser.getAttributeValue(null, "id"),
                                    parser.getAttributeValue(null, "name"));
                        } else if (parser.getName().equalsIgnoreCase("data")) {
                            data = new Method.Data(parser.getAttributeValue(null, "id"),
                                    parser.getAttributeValue(null, "key"),
                                    Class.forName(parser.getAttributeValue(null, "type")));
                        } else if (parser.getName().equalsIgnoreCase("page")) {
                            page = new Page(parser.getAttributeValue(null, "id"),
                                    parser.getAttributeIntValue(null, "requestCode", -1));
                        } else if (parser.getName().equalsIgnoreCase("bundle")) {
                            bundle = new Page.Bundle(parser.getAttributeValue(null, "id"),
                                    parser.getAttributeValue(null, "key"),
                                    (Class<? extends Serializable>) Class.forName(parser.getAttributeValue(null, "type")));
                        }
                        break;
                    case XmlPullParser.END_TAG://判断当前事件是否是标签元素结束事件
                        if (parser.getName().equalsIgnoreCase("service")) {
                            if (findServiceByUrl(service.getUrl()) == null) {
                                serviceList.add(service);
                                service = null;
                            } else {
                                throw new Exception("[" + service.getUrl() + "] already register service [" + service + "]");
                            }
                        } else if (parser.getName().equalsIgnoreCase("method")) {
                            if (findMethodById(service, method.getId()) == null) {
                                service.getMethods().add(method);
                                method = null;
                            } else {
                                throw new Exception("[" + method.getId() + "] already register method [" + method + "]");
                            }
                        } else if (parser.getName().equalsIgnoreCase("data")) {
                            method.getDatas().add(data);
                            data = null;
                        } else if (parser.getName().equalsIgnoreCase("page")) {
                            if (findPageById(service, page.getId()) == null) {
                                service.getPages().add(page);
                                page = null;
                            } else {
                                throw new Exception("[" + method.getId() + "] already register page [" + page + "]");
                            }
                        } else if (parser.getName().equalsIgnoreCase("bundle")) {
                            page.getBundles().add(bundle);
                            bundle = null;
                        }
                        break;
                }
                event = parser.next();//进入下一个元素
            }
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            Log.i(TAG, "Length:" + serviceList.size() + " Service：" + serviceList.toString());
        }
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
            if (url != null) {
                Service service = findServiceByUrl(url.getUrl());
                Method method = findMethodById(service, url.getId());
                if (method != null) {
                    String mName = method.getName();
                    Class<?> clazz = service.getClazz();
                    for (java.lang.reflect.Method item : clazz.getDeclaredMethods()) {
                        if (item.getName().equals(mName)) {
                            execMethod(item, context, callBack, jsonObject, method.getDatas());
                            break;
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(EventBus.TAG, e.getMessage());
            e.printStackTrace();
        }
    }

    public void call(Context context, EUrl url, JsonObject params) {
        this.call(context, url, params, null);
    }


    public void open(Context context, EUrl url, JsonObject jsonObject) {
        try {
            if (url != null) {
                Service service = findServiceByUrl(url.getUrl());
                Page page = findPageById(service, url.getId());
                if (page != null) {
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
                }
            }
        } catch (Exception e) {
            Log.e(EventBus.TAG, e.getMessage());
            e.printStackTrace();
        }
    }

    private void execMethod(java.lang.reflect.Method method, Context context, OnMethodCallBack callBack, JsonObject json, List<Method.Data> datas) throws Exception {
        int step = 2;
        Annotation[][] annotations = method.getParameterAnnotations();
        switch (annotations.length - step) {
            case 0:
                method.invoke(null,
                        context, callBack);
                break;
            case 1:
                method.invoke(null,
                        context, callBack,
                        getValue(json, annotations[step + 0], datas));
                break;
            case 2:
                method.invoke(null,
                        context, callBack,
                        getValue(json, annotations[step + 0], datas),
                        getValue(json, annotations[step + 1], datas));
                break;
            case 3:
                method.invoke(null,
                        context, callBack,
                        getValue(json, annotations[step + 0], datas),
                        getValue(json, annotations[step + 1], datas),
                        getValue(json, annotations[step + 2], datas));
                break;
            case 4:
                method.invoke(null,
                        context, callBack,
                        getValue(json, annotations[step + 0], datas),
                        getValue(json, annotations[step + 1], datas),
                        getValue(json, annotations[step + 2], datas),
                        getValue(json, annotations[step + 3], datas));
                break;
            case 5:
                method.invoke(null,
                        context, callBack,
                        getValue(json, annotations[step + 0], datas),
                        getValue(json, annotations[step + 1], datas),
                        getValue(json, annotations[step + 2], datas),
                        getValue(json, annotations[step + 3], datas),
                        getValue(json, annotations[step + 4], datas));
                break;
        }
    }

    private Object getValue(JsonObject json, Annotation[] items, List<Method.Data> datas) {
        if (json == null || datas.size() == 0) return null;
        Params ann = null;
        for (Annotation item : items) {
            if (item instanceof Params) {
                ann = (Params) item;
                break;
            }
        }
        if (ann != null) {
            for (Method.Data data : datas) {
                if (data.getKey().equalsIgnoreCase(ann.value())) {
                    String id = data.getId();
                    Class<?> clazz = data.getType();

                    JsonElement value = json.get(id);
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
                    break;
                }
            }
        }
        return null;
    }

    private Bundle getBundleByPage(Page page, JsonObject json) throws Exception {
        if (page != null && json != null && page.getBundles().size() > 0) {
            Bundle bundle = new Bundle();
            for (Page.Bundle item : page.getBundles()) {
                String id = item.getId();
                String key = item.getKey();
                Class<? extends Serializable> clazz = item.getType();

                JsonElement value = json.get(id);
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

    public Service findServiceByUrl(String url) {
        if (url != null) {
            for (Service item : serviceList) {
                if (url.equalsIgnoreCase(item.getUrl())) {
                    return item;
                }
            }
        }
        return null;
    }

    public Method findMethodById(Service service, String id) {
        if (service != null && id != null) {
            for (Method item : service.getMethods()) {
                if (id.equalsIgnoreCase(item.getId())) {
                    return item;
                }
            }
        }
        return null;
    }

    public Page findPageById(Service service, String id) {
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
