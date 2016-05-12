package org.greenrobot.eventbus;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Glen on 2016/5/10.
 */
public class Service {
    private String url;
    private Class<?> clazz;
    private List<Method> methods;
    private List<Page> pages;

    public Service(String url, Class<?> clazz) {
        this.url = url;
        this.clazz = clazz;
        methods = new ArrayList<>();
        pages = new ArrayList<>();
    }

    public String getUrl() {
        return url;
    }

    public Class<?> getClazz() {
        return clazz;
    }

    public List<Method> getMethods() {
        return methods;
    }

    public List<Page> getPages() {
        return pages;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) return false;
        return toString().equals(o.toString());
    }

    @Override
    public String toString() {
        return "Service{" +
                "url='" + url + '\'' +
                ", clazz=" + clazz +
                ", methods=" + methods +
                ", pages=" + pages +
                '}';
    }
}