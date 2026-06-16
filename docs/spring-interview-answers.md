# Spring 面试五题

---

## 7. IoC 和 DI、Spring 怎么管理 Bean

> 控制反转就是把对象的创建权从自己手里交出去。不用自己 new，交给 Spring 容器去管。依赖注入就是 Spring 把需要的东西自动塞给你——写 @Autowired，Spring 就把对应的 Bean 注入进来。
>
> Spring 管理 Bean 分三步：启动时扫描指定包下的类，用反射创建 Bean 放进 IoC 容器（一个 Map），需要的时候根据类型或名字从 Map 里取出来注入。

---

## 8. @Autowired 和 @Resource 的区别

> @Autowired 是 Spring 自带的，按类型注入。如果同一个类型有多个实现，需要配合 @Qualifier 指定名字。@Resource 是 JDK 自带的，默认按名字注入，找不到再按类型。
>
> 日常开发两个都能用。项目里用的全是 @Autowired，因为每个接口只有一个实现类，不需要区分名字。

---

## 9. @SpringBootApplication 包含哪些注解、自动装配

> @SpringBootApplication 是一个组合注解。里面三个核心：@SpringBootConfiguration 标识这是配置类、@EnableAutoConfiguration 开启自动装配、@ComponentScan 扫描当前包和子包的组件。
>
> 自动装配的原理是 @EnableAutoConfiguration 去读所有 jar 包里的 META-INF/spring.factories 文件，里面注册了一堆 xxxAutoConfiguration。每个 AutoConfiguration 类上都有条件注解——@ConditionalOnClass 判断 classpath 有没有这个类，@ConditionalOnMissingBean 判断容器里有没有用户自定义的 Bean。用户自定义了就用自己的，没定义才用默认的。

---

## 10. Bean 的生命周期

> Bean 默认是单例的，整个容器只有一个实例。想改成多例就在类上加 @Scope("prototype")——每次 getBean 都 new 一个新的。
>
> 生命周期关键步骤：实例化（new）→ 属性赋值（注入其他 Bean）→ 初始化前（BeanPostProcessor 前置处理）→ 自定义初始化（@PostConstruct）→ 初始化后（BeanPostProcessor 后置处理）→ Bean 就绪，可以使用 → 容器关闭时销毁（@PreDestroy）。

---

## 11. @Transactional 失效场景和事务传播行为

> 三种常见失效场景：this 调用不经过 AOP 代理，事务不生效。非 public 方法 AOP 拦不到。异常被 catch 了没抛出去，Spring 感知不到不会回滚。
>
> 事务传播行为就是方法调方法时，事务怎么传递。最常用的两个：REQUIRED（默认）——如果有事务就加入，没有就新建。REQUIRES_NEW——不管外面有没有事务，自己开一个新的。前者适合同一事务内的连续操作，后者适合错误了不该回滚主业务的子操作。
