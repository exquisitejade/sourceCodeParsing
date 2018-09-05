### <font face="微软雅黑">tomcat源码导入 Idea</font>

> omcat是Apache 软件基金会（Apache Software Foundation）的Jakarta 项目中的一个核心项目，由Apache、Sun 和其他一些公司及个人共同开发而成。由于有了Sun 的参与和支持，最新的Servlet 和JSP 规范总是能在Tomcat 中得到体现。因为Tomcat 技术先进、性能稳定，而且免费，因而深受Java 爱好者的喜爱并得到了部分软件开发商的认可，成为目前比较流行的Web 应用服务器。

* 看完后将学到以下
    * [x] 源码下载获取
    * [x] 源码导入Idea
    * [x] 源码启动

#### 源码获取路径
   
   1. 官方网站获取 [Apache Tomcat](https://tomcat.apache.org/download-90.cgi)
   2. 本章主要是gitHub获取
        * [gitHub](https://github.com/) 在搜索框中填入"tomcat",显示的第一个则是 ["apache/tomcat"](https://github.com/apache/tomcat) 点击下载即可
        * 也可以从我GitHub上直接下载 ["tomcat"](https://github.com/gitXugx/sourceCodeParsing) 我这边是配置好的可以直接启动


#### 源码导入Idea

  1. 在下载的源码中, build.xml 的同级目录下创建一个 **pom.xml** 
    1. pom.xml 文件中填入
    
```xml 
        <?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">

    <modelVersion>4.0.0</modelVersion>
    <groupId>cn.linxdcn</groupId>
    <artifactId>Tomcat9.0</artifactId>
    <name>Tomcat9</name>
    <version>9.0</version>

    <build>
        <finalName>Tomcat9</finalName>
        <sourceDirectory>java</sourceDirectory>
        <resources>
            <resource>
                <directory>java</directory>
            </resource>
        </resources>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>2.3</version>
                <configuration>
                    <encoding>UTF-8</encoding>
                    <source>1.8</source>
                    <target>1.8</target>
                </configuration>
            </plugin>
        </plugins>
    </build>

    <dependencies>
        <dependency>
            <groupId>org.apache.ant</groupId>
            <artifactId>ant</artifactId>
            <version>1.9.5</version>
        </dependency>
        <dependency>
            <groupId>org.apache.ant</groupId>
            <artifactId>ant-apache-log4j</artifactId>
            <version>1.9.5</version>
        </dependency>
        <dependency>
            <groupId>org.apache.ant</groupId>
            <artifactId>ant-commons-logging</artifactId>
            <version>1.9.5</version>
        </dependency>
        <dependency>
            <groupId>javax.xml.rpc</groupId>
            <artifactId>javax.xml.rpc-api</artifactId>
            <version>1.1</version>
        </dependency>
        <dependency>
            <groupId>wsdl4j</groupId>
            <artifactId>wsdl4j</artifactId>
            <version>1.6.2</version>
        </dependency>
        <dependency>
            <groupId>org.eclipse.jdt.core.compiler</groupId>
            <artifactId>ecj</artifactId>
            <version>4.4</version>
        </dependency>
    </dependencies>
</project>
```
  3. 打开Idea "File -> new -> Module form exiting "选中源码目录 ,选择 maven项目 下一步即可
  4. 选择对应的JDK即可

#### 源码启动

1. 删除webapps目录下面的"examples"因为这个下面web.xml配置的有些监听器和过滤器没有对应的class 所以启动时会报错但是不影响访问
2. 在项目build的时会有些编译不通过,删除即可,因为JVM版本我们可能用的没有他配置高的 所以删除高的版本既可以
3. 点击 edit configution 添加一个application 下面变量填写
   * **Main class :** org.apache.catalina.startup.Bootstrap
   * **Working directory :** 选择你对应的源码目录 
   * **Use classpath of module :** 选择源码
   * **JRE :** 选择对应的jre 
    
4. 在webapps下面创建你的访问项目例如:test目录下创建index.html
5. 访问localhost:8080/index.html

---
 源码下载:[tomcat9.0](https://github.com/gitXugx/sourceCodeParsing)



 










