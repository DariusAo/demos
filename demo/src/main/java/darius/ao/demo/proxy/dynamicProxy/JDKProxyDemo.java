package darius.ao.demo.proxy.dynamicProxy;


import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class JDKProxyDemo {
    public static void main(String[] args) {
        SayImpl say = new SayImpl();
        DariusInvocationHandler handler = new DariusInvocationHandler(say);
        Say proxy = (Say) ProxyFactory.getProxyInstance(say, handler);
        proxy.hello("darius");
        proxy.mmp("gdz");
    }
}

interface Say {
    void hello(String name);

    void mmp(String name);
}

class SayImpl implements Say {
    @Override
    public void hello(String name) {
        System.out.println("hello, " + name);
    }

    @Override
    public void mmp(String name) {
        System.out.println("mmp, " + name);
    }
}

class DariusInvocationHandler implements InvocationHandler {
    private Object target;

    public DariusInvocationHandler(Object target) {
        this.target = target;
    }

    public void bind(Object target) {
        this.target = target;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        System.out.println("before " + method.getName() + "...");
        Object invoke = method.invoke(target, args);//通过反射执行，目标类的方法
        System.out.println("after " + method.getName() + "...");
        return invoke;
    }
}

class ProxyFactory {
    private ProxyFactory() throws Exception {
        throw new Exception("ProxyFactory shouldn't be initialized");
    }

    public static Object getProxyInstance(Object target, InvocationHandler handler) {
        Object proxy = Proxy.newProxyInstance(target.getClass().getClassLoader(), target.getClass().getInterfaces(), handler);
        return proxy;
    }
}