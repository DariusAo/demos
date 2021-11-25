package darius.ao.demo.proxy.dynamicProxy;


import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

import java.lang.reflect.Method;

public class CglibProxyDemo {
    public static void main(String[] args) {
        //创建Enhancer对象，类似于JDK动态代理的Proxy类，下一步就是设置几个参数
        Enhancer enhancer = new Enhancer();
        //设置目标类的字节码文件
        enhancer.setSuperclass(Target.class);
        //设置回调函数
        enhancer.setCallback(new DariusMethodInterceptor());

        //这里的creat方法就是正式创建代理类
        Target proxy = (Target) enhancer.create();
        proxy.method0("param0");
        proxy.method1("param1");
        proxy.method2("param2");
    }
}

class Target {
    public static void method0(String param0) {
        System.out.println("this is method0, the param is: " + param0);
    }

    /**
     * final method can't be proxied
     *
     * @param param
     */
    public final void method1(String param) {
        System.out.println("this is method1, the param is: " + param);
    }

    public void method2(String param) {
        System.out.println("this is method2, the param is: " + param);
    }
}


class DariusMethodInterceptor implements MethodInterceptor {

    @Override
    public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
        System.out.println("before " + method.getName());
        Object object = proxy.invokeSuper(obj, args);
        System.out.println("after " + method.getName());
        return object;
    }
}
