# 关于jdk动态代理

代码示例见JDKProxyDemo.java

jdk动态代理又被称为接口代理，被代理的对象必须实现了接口（通常与之相对应的cglib动态代理，又被称为子类代理）

jdk动态代理通过jdk提供的工具方法Proxy.newProxyInstance动态构建全新的代理类的字节码文件并实例化对象返回

```java
 @CallerSensitive
public static Object newProxyInstance(ClassLoader loader,
        Class<?>[]interfaces,
        InvocationHandler h){
        /*
         * Look up or generate the designated proxy class.(查找或指定生成的代理类)
         */
        Class<?> cl=getProxyClass0(loader,intfs);
        ...
        final Constructor<?> cons=cl.getConstructor(constructorParams);
        ...
        return cons.newInstance(new Object[]{h});
}
```

jdk动态代理的本质是：JDK在运行期间帮我们生成了一个代理类的字节码，通过类加载器加载这个字节码，然后执行引擎进行一系列处理后生成代理类，再进行实例化

根据上面代码示例，逐层追踪：

```java
->getProxyClass0()
->return proxyClassCache.get(loader,interfaces)
->
// create subKey and retrieve the possible Supplier<V> stored by that
// subKey from valuesMap
Object subKey=Objects.requireNonNull(subKeyFactory.apply(key,parameter));
->subKeyFactory.apply（进入ProxyClassFactory）
->
/*
 * Generate the specified proxy class.
 */
byte[]proxyClassFile=ProxyGenerator.generateProxyClass(proxyName,interfaces,accessFlags);
try{
    return defineClass0(loader,proxyName,proxyClassFile,0,proxyClassFile.length);
}（进入defineClass0）
->private static native Class<?> defineClass0
```

# 关于cglib动态代理

代码示例见CglibProxyDemo.java

注意：需要额外引入lib目录下的asm和cglib，且二者需要版本相匹配才行

逐层追踪源码：

```java
->Target proxy = (Target) enhancer.create();
->return createHelper();
->
return super.create(...);
...
byte[] b = strategy.generate(this);   // 猜测是这里创建了代理对象的字节码
        ->transform(cg).generateClass(cw);
        ->进入generateClass();    // 回到Enhancer，代理类即是在这个方法里面生成的，在generate方法中又将其转为了字节码
...
gen = ReflectUtils.defineClass(className, b, loader);   // 定义代理类
...
return firstInstance(gen);    // 发现该方法直接抛异常，需要子类实现；另外，根据名字即可猜测该方法是返回参数类的实例
->(回到enhancer)protected Object firstInstance(Class type);
->return createUsingReflection(type);
->setThreadCallbacks(type, callbacks);  // callbacks就是我们开始设置的，被增强后的方法；里面只有一个方法调用，进入
    ->
    Method setter = getCallbacksSetter(type, methodName);
    setter.invoke(null, new Object[]{ callbacks });
return ReflectUtils.newInstance(type);
```

本质上是利用ASM开源包，将真实对象类的class文件加载进来，通过修改字节码生成其子类，覆盖父类相应的方法

（[原文](https://juejin.cn/post/6850418115365470222)）在JDK动态代理中，调用目标对象的方法使用的是反射，而在CGLIB动态代理中使用的是FastClass机制。

- FastClass使用：动态生成一个继承FastClass的类，并向类中写入委托对象，直接调用委托对象的方法。
- FastClass逻辑：在继承FastClass的动态类中，根据方法签名(方法名字+方法参数)得到方法索引，根据方法索引调用目标对象方法。
- FastClass优点：FastClass用于代替Java反射，避免了反射造成调用慢的问题。

# jdk动态代理与cglib动态代理的对比

1. jdk动态代理被代理的对象需要实现接口，而cglib不用
1. 由于cglib是基于继承（子类）来实现的动态动态代理，所以不能代理final的、static的方法
1. 由于cglib是采用FastClass机制（而非反射），所以效率比jdk动态代理高

# 关于为什么FastClass机制效率比JDK动态代理高

Cglib动态代理执行代理方法效率之所以比JDK的高是因为Cglib采用了FastClass机制，它的原理简单来说就是：为代理类和被代理类各生成一个Class，这个Class会为代理类或被代理类的方法分配一个index(int类型)。

这个index当做一个入参，FastClass就可以直接定位要调用的方法直接进行调用，这样省去了反射调用，所以调用效率比JDK动态代理通过反射调用高

与之相对的，你需要大概知道一下反射为什么效率低（[更多内容可以见这里](https://stackoverflow.com/questions/1392351/java-reflection-why-is-it-so-slow?answertab=votes#tab-top)）：

1. 因为通过反射获取到对象，我们无法直接知道这个对象里面存在哪些东西，因此需要遍历去进行查找，所以效率低
1. 编译器无法进行优化
1. 常常需要对数据进行类型的转换、包装等操作