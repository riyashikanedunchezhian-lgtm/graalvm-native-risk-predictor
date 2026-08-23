package com.example;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class ReflectiveThing {

    public void loadDynamically(String className) throws Exception {
        Class<?> clazz = Class.forName(className); // non-constant arg on purpose
        Method m = clazz.getDeclaredMethod("run");
        m.invoke(clazz.getDeclaredConstructor().newInstance());
    }

    public Object makeProxy(ClassLoader loader, Class<?>[] interfaces, java.lang.reflect.InvocationHandler handler) {
        return Proxy.newProxyInstance(loader, interfaces, handler);
    }

    public java.io.InputStream loadResource(String path) {
        return getClass().getResourceAsStream(path); // non-constant path on purpose
    }
}
