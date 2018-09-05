### tomcat Server组件源码分析
> server其实就是我们的server.xml文件

学习:
* [x] 对Server源码的解读
* [x] 结合"[tomcat启动源码调用](tomcat启动源码调用.md)"会有更深刻的理解

#### 1.对Server源码的解读

&emsp;想要入手Server组件肯定需要从配置文件读起,因为在Catalina中初始化Server的时候是通过读取**conf/server.xml**来进行初始化的
``````xml
<?xml version="1.0" encoding="UTF-8"?>
<!-- 是port是Server监听的端口请求,发送"shutdown"即可关闭服务器,默认是localhost-->
<Server port="8005" shutdown="SHUTDOWN">
  <Listener className="org.apache.catalina.startup.VersionLoggerListener" />
  <!-- Security listener. Documentation at /docs/config/listeners.html
  <Listener className="org.apache.catalina.security.SecurityListener" />
  -->
  <!--APR library loader. Documentation at /docs/apr.html -->
  <Listener className="org.apache.catalina.core.AprLifecycleListener" SSLEngine="on" />
  <!-- Prevent memory leaks due to use of particular java/javax APIs-->
  <Listener className="org.apache.catalina.core.JreMemoryLeakPreventionListener" />
  <Listener className="org.apache.catalina.mbeans.GlobalResourcesLifecycleListener" />
  <Listener className="org.apache.catalina.core.ThreadLocalLeakPreventionListener" />

  <!-- Global JNDI resources
       Documentation at /docs/jndi-resources-howto.html
  -->
  <!-- JNDI相关东西,设置一个内存用户-->
  <GlobalNamingResources>
    <Resource name="UserDatabase" auth="Container"
              type="org.apache.catalina.UserDatabase"
              description="User database that can be updated and saved"
              factory="org.apache.catalina.users.MemoryUserDatabaseFactory"
              pathname="conf/tomcat-users.xml" />
  </GlobalNamingResources>
    /
  <Service name="Catalina">
     <!-- 连接器的监听端口连接配置 http协议-->
    <Connector port="8080" protocol="HTTP/1.1"
               connectionTimeout="20000"
               redirectPort="8443" />

    <!--
    <Connector port="8443" protocol="org.apache.coyote.http11.Http11NioProtocol"
               maxThreads="150" SSLEnabled="true">
        <SSLHostConfig>
            <Certificate certificateKeystoreFile="conf/localhost-rsa.jks"
                         type="RSA" />
        </SSLHostConfig>
    </Connector>
    -->
    <!-- Define an AJP 1.3 Connector on port 8009 -->
    <Connector port="8009" protocol="AJP/1.3" redirectPort="8443" />
     <!-- 创建一个默认Engine-->
    <Engine name="Catalina" defaultHost="localhost">
      <Realm className="org.apache.catalina.realm.LockOutRealm">
        <Realm className="org.apache.catalina.realm.UserDatabaseRealm"
               resourceName="UserDatabase"/>
      </Realm>
      <Host name="localhost"  appBase="webapps"
            unpackWARs="true" autoDeploy="true">
        <Valve className="org.apache.catalina.valves.AccessLogValve" directory="logs"
               prefix="localhost_access_log" suffix=".txt"
               pattern="%h %l %u %t &quot;%r&quot; %s %b" />
      </Host>
    </Engine>
  </Service>
</Server>
``````
&emsp; Server组件的实现是StandardServer我们去看看StandardServer的变量

``````java
public final class StandardServer extends LifecycleMBeanBase implements Server {
        //默认构造方法
        public StandardServer() {
        super();
        globalNamingResources = new NamingResourcesImpl();
        globalNamingResources.setContainer(this);
        if (isUseNaming()) {
            namingContextListener = new NamingContextListener();
            addLifecycleListener(namingContextListener);
        } else {
            namingContextListener = null;
        }
    }
    private javax.naming.Context globalNamingContext = null;
    //对应xml中的<GlobalNamingResources>标签
    private NamingResourcesImpl globalNamingResources = null;
    
}
``````
















