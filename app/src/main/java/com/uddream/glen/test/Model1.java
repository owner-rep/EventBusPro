package com.uddream.glen.test;

import java.io.Serializable;

/**
 * Created by Glen on 2016/5/12.
 */
public class Model1 implements Serializable {
    String name;
    String pwd;

    public Model1(String name, String pwd) {
        this.name = name;
        this.pwd = pwd;
    }

    @Override
    public String toString() {
        return "Model2{" +
                "name='" + name + '\'' +
                ", pwd='" + pwd + '\'' +
                '}';
    }
}
