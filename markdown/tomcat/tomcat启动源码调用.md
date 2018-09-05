### tomcat启动停止源码方法调用流程

``````text
org.apache.catalina.startup.Bootstrap.main()

Bootstrap类加载时,初始化系统变量catalina路径

load(args)命令(-config , -nonaming,-help,start,configtest,stop)
mian(args)命令(startd,stopd,start,stop,configtest)

启动解析:
--->main(args) 只有一个线程进行启动 通过命令模式做出对应的操作默认start 
    --->init() 创建Catalina对象,设置Catalina的父加载器是sharedLoader器
        --->initClassLoaders() 初始化commonLoader,catalinaLoader,sharedLoader如果catalina.properties不配置的情况下默认commonLoader
        --->SecurityClassLoad.securityClassLoad(catalinaLoader) 设置catalinaLoader类加载器可加载的目录(权限设置)
    --->daemon.setAwait(true);  设置是否阻止catalina
    --->load(args)根据命令来做出相应的处理,
        --->Catalina.load()
            --->initDirs() 初始化临时目录,没有则打印临时目录日志
            --->initNaming()初始化命名服务的基本配置
            --->createStartDigester()创建对server.xml的处理的对象,指定解析catalina的servre为standardServer对象
            --->configFile()创建server.xml file对象
            --->digester.push(this); 如果为size为0则该catalina设置为root节点
            --->digester.parse(inputSource);解析server.xml里的Server初始化StandardServer
            --->getServer().init();初始化server
                --->LifecycleBase.init
                    --->setStateInternal()初始化之前执行监听事件
                    --->initInternal()初始化
                        --->standardServer.initInternal
                            --->jmx.initInternal 使变量可以动态配置
                            --->执行子容器的所有init方法
                    --->setStateInternal()初始化之后执行监听事件
    --->start()
        --->catalina未被初始化的情况下先初始化
        --->catalina.start()
            --->如果server为空，就行load加载    
                --->LifecycleBase.start如果当前状态为new状态需要初始化server
                --->LifecycleBase.stop 如果当前状态为Faild 需要停止server
                --->LifecycleBase.setStateInternal启动之前执行监听事件
                --->LifecycleBase.startInternal 执行子容器的所有start方法server,service,connector...
                --->判断是否启动成功
                --->LifecycleBase.setStateInternal成功启动之后执行监听事件
                --->创建CatalinaShutdownHook对象添加运行时关闭程序需要调用stop和destroy
                    --->standardServer.await方法进行再8005端口进行监听是否发送过来关闭命令则停止监听调用
        --->如果发送来的是关闭命令则调用catalina.stop()方法




stop 分为两种结束一种是直接运行时关闭,一种是通过命令关闭
    --->stop()
        --->catalina.stop按我自己理解实际这个方法是提供给Runtime.getRuntime().addShutdownHook(shutdownHook)使用
            --->server.stop
                --->LifecycleBase.setStateInternal停止服务前执行的监听事件
                --->LifecycleBase.stopInternal 执行子容器的所有stop方法server,service,connector...
                --->LifecycleBase.setStateInternal停止服务后执行的监听事件
                --->setStateInternal销毁服务前执行的监听事件
                --->destroyInternal 执行子容器的所有destroy方法server,service,connector...
                --->setStateInternal销毁服务后执行的监听事件

    --->stopServer()
        --->catalina.stopServer()是远程关闭的一种方式
            --->创建socket客户端发送shutdown命令给远程8005端口服务器进行一个关闭

org.apache.catalina.startup.Bootstrap：启动tomcat的入口,初始化classLoader并且设置权限,初始化文件目录,调用catalina,接收命令做出相应操作
org.apache.catalina.startup.Catalina: server的容器,主要做加载配置,初始化server,启动server,停止server等一系列操作
org.apache.catalina.util.LifecycleMBeanBase jmx动态配置
org.apache.catalina.util.LifecycleBase 容器生命周期的控制的基本实现
org.apache.catalina.util.Lifecycle 容器生命周期和监听器行为的定义

生命周期流程:

 *            start()
 *  -----------------------------
 *  |                           |
 *  | init()                    |
 * NEW -»-- INITIALIZING        |
 * | |           |              |     ------------------«-----------------------
 * | |           |auto          |     |                                        |
 * | |          \|/    start() \|/   \|/     auto          auto         stop() |
 * | |      INITIALIZED --»-- STARTING_PREP --»- STARTING --»- STARTED --»---  |
 * | |         |                                                            |  |
 * | |destroy()|                                                            |  |
 * | --»-----«--    ------------------------«--------------------------------  ^
 * |     |          |                                                          |
 * |     |         \|/          auto                 auto              start() |
 * |     |     STOPPING_PREP ----»---- STOPPING ------»----- STOPPED -----»-----
 * |    \|/                               ^                     |  ^
 * |     |               stop()           |                     |  |
 * |     |       --------------------------                     |  |
 * |     |       |                                              |  |
 * |     |       |    destroy()                       destroy() |  |
 * |     |    FAILED ----»------ DESTROYING ---«-----------------  |
 * |     |                        ^     |                          |
 * |     |     destroy()          |     |auto                      |
 * |     --------»-----------------    \|/                         |
 * |                                 DESTROYED                     |
 * |                                                               |
 * |                            stop()                             |
 * ----»-----------------------------»------------------------------

```

