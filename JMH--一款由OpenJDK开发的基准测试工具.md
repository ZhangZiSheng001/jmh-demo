# 什么是JMH

JMH 是 OpenJDK 团队开发的一款基准测试工具，一般用于代码的性能调优，精度甚至可以达到纳秒级别，适用于 java 以及其他基于 JVM 的语言。和 Apache JMeter 不同，**JMH 测试的对象可以是任一方法，颗粒度更小**，而不仅限于rest api。

使用时，我们只需要通过配置告诉 JMH 测试哪些方法以及如何测试，JMH 就可以为我们**自动生成基准测试的代码**。

# JMH生成基准测试代码的原理

我们只需要通过配置（主要是注解）告诉 JMH 测试哪些方法以及如何测试，JMH 就可以为我们自动生成基准测试的代码。

那么 JMH 是如何做到的呢？

要使用 JMH，**我们的 JMH 配置项目必须是 maven 项目**。在一个 JMH配置项目中，我们可以在`pom.xml`看到以下配置。JMH 自动生成基准测试代码的本质就是**使用 maven 插件的方式，在 package 阶段对配置项目进行解析和包装**。

```xml
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-shade-plugin</artifactId>
                <version>2.2</version>
                <executions>
                    <execution>
                        <phase>package</phase>
                        <goals>
                            <goal>shade</goal>
                        </goals>
                        <configuration>
                            <finalName>${uberjar.name}</finalName>
                            <transformers>
                                <transformer implementation="org.apache.maven.plugins.shade.resource.ManifestResourceTransformer">
                                    <mainClass>org.openjdk.jmh.Main</mainClass>
                                </transformer>
                            </transformers>
                            <filters>
                                <filter>
                                    <artifact>*:*</artifact>
                                    <excludes>
                                        <exclude>META-INF/*.SF</exclude>
                                        <exclude>META-INF/*.DSA</exclude>
                                        <exclude>META-INF/*.RSA</exclude>
                                    </excludes>
                                </filter>
                            </filters>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
```

# 从入门例子开始

下面会先介绍整个使用流程，再通过一个入门例子来演示如何使用 JMH。

## 步骤

如果我有一个 A 项目，我希望对这个项目里的某些方法进行 JMH 测试，可以这么做：

1. **创建单独的 JMH 配置项目**B。

新建一个独立的配置项目 B（**建议使用 archetype 生成，可以确保配置正确**），B 依赖了 A。

当然，我们也可以直接将项目 A 作为 JMH 配置项目，但这样做会导致 JMH 渗透到 A 项目中，所以，最好不要这么做。

2. **配置项目B**。

在 B 项目里面，我们可以使用 JMH 的注解或对象来指定测试哪些方法以及如何测试，等等。

3. **构建和运行**。

在正确配置 pom.xml 的前提下，使用 mvn 命令打包 B 项目，JMH 会为我们自动生成基准测试代码，并单独打包成 benchmarks.jar。运行 benchmarks.jar，基准测试就可以跑起来了。

注意，JMH 也支持使用 Java API 的方式来运行，但官方并不推荐，所以，本文也不会介绍。

下面开始入门例子。

## 项目环境说明

maven：3.6.3

操作系统：win10

JDK：8u231

JMH：1.25

## 创建 JMH 配置项目

为了保证配置的正确性，建议使用 archetype 生成 JMH 配置项目。cmd 运行下面这段代码：

```powershell
mvn archetype:generate ^
-DinteractiveMode=false ^
-DarchetypeGroupId=org.openjdk.jmh ^
-DarchetypeArtifactId=jmh-java-benchmark-archetype ^
-DarchetypeVersion=1.25 ^
-DgroupId=cn.zzs.jmh ^
-DartifactId=jmh-test01 ^
-Dversion=1.0.0
```
注：如果使用 linux，请将“^”替代为“\”。

命令执行后，在当前目录下生成了一个 maven 项目，如下。这个项目就是本文说到的 JMH 配置项目。这里 archetype 还提供了一个例子`MyBenchmark`。

```powershell
└─jmh-test01
    │  pom.xml
    │
    └─src
        └─main
            └─java
                └─cn
                    └─zzs
                        └─jmh
                                MyBenchmark.java
```

## 配置 JMH 配置项目

### 配置 pom.xml

因为是使用 archetype 生成的项目，所以pom.xml 文件已经包含了比较完整的 JMH 配置，如下(省略部分)。如果自己手动创建配置项目，则需要拷贝下面这些内容。

```xml
    <dependencies>
        <dependency>
            <groupId>org.openjdk.jmh</groupId>
            <artifactId>jmh-core</artifactId>
            <version>${jmh.version}</version>
        </dependency>
        <dependency>
            <groupId>org.openjdk.jmh</groupId>
            <artifactId>jmh-generator-annprocess</artifactId>
            <version>${jmh.version}</version>
            <scope>provided</scope>
        </dependency>
    </dependencies>

    <properties>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <jmh.version>1.25</jmh.version>
        <javac.target>1.8</javac.target>
        <uberjar.name>benchmarks</uberjar.name>
    </properties>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-shade-plugin</artifactId>
                <version>3.2.1</version>
                <executions>
                    <execution>
                        <phase>package</phase>
                        <goals>
                            <goal>shade</goal>
                        </goals>
                        <configuration>
                            <finalName>${uberjar.name}</finalName>
                            <transformers>
                                <transformer implementation="org.apache.maven.plugins.shade.resource.ManifestResourceTransformer">
                                    <mainClass>org.openjdk.jmh.Main</mainClass>
                                </transformer>
                                <transformer implementation="org.apache.maven.plugins.shade.resource.ServicesResourceTransformer"/>
                            </transformers>
                            <filters>
                                <filter>
                                    <artifact>*:*</artifact>
                                    <excludes>
                                        <exclude>META-INF/*.SF</exclude>
                                        <exclude>META-INF/*.DSA</exclude>
                                        <exclude>META-INF/*.RSA</exclude>
                                    </excludes>
                                </filter>
                            </filters>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
```

### 配置Benchmark方法

项目里的 MyBenchmark 类就是一个简单的示例，testMethod 方法就是一个 Benchmark 方法。我们可以直接在 testMethod 方法中编写测试代码，也可以调用父项目的方法。

testMethod 方法上加了`@Benchmark`注解，**`@Benchmark`注解用来告诉 JMH 在 mvn package 时生成这个方法的基准测试代码**。

当然，我们还可以增加其他的配置来影响 JMH 如何生成基准测试代码，这里暂时不展开。

```java
package cn.zzs.jmh;

import org.openjdk.jmh.annotations.Benchmark;

public class MyBenchmark {

    @Benchmark
    public void testMethod() {
        // place your benchmarked code here
    }

}
```

### 打包和运行

分别运行以下命令，完成对项目的打包：

```powershell
cd jmh-test01
mvn clean package
```

这时，target 目录下，不仅生成了项目本身的 jar 包，还生成了一个 benchmarks.jar。这个包就是 JMH 为我们生成的基准测试代码。

```powershell
└─jmh-test01
    │  pom.xml
    │
    ├─src
    │  └─main
    │      └─java
    │          └─cn
    │              └─zzs
    │                  └─jmh
    │                          MyBenchmark.java
    │
    └─target
          benchmarks.jar
          jmh-test01-1.0.0.jar
```

运行以下命令:

```powershell
java -jar target/benchmarks.jar
```

这时，我们的基准测试就开始运行了。

```powershell
D:\growUp\git_repository\java-tools\jmh-demo\jmh-test01>java -jar target/benchmarks.jar
# JMH version: 1.25
# VM version: JDK 1.8.0_231, Java HotSpot(TM) 64-Bit Server VM, 25.231-b11
# VM invoker: D:\growUp\installation\jdk1.8.0_231\jre\bin\java.exe
# VM options: <none>
# Warmup: 5 iterations, 10 s each
# Measurement: 5 iterations, 10 s each
# Timeout: 10 min per iteration
# Threads: 1 thread, will synchronize iterations
# Benchmark mode: Throughput, ops/time
# Benchmark: cn.zzs.jmh.MyBenchmark.testMethod

# Run progress: 0.00% complete, ETA 00:08:20
# Fork: 1 of 5
# Warmup Iteration   1: 3955731078.669 ops/s
# Warmup Iteration   2: 3910971792.656 ops/s
# Warmup Iteration   3: 3881001464.578 ops/s
# Warmup Iteration   4: 3916172600.571 ops/s
# Warmup Iteration   5: 3956321997.093 ops/s
Iteration   1: 3942596162.384 ops/s
Iteration   2: 3962073081.983 ops/s
Iteration   3: 3956347169.335 ops/s
Iteration   4: 3935835073.222 ops/s
Iteration   5: 3934716909.315 ops/s

# ······

# Run progress: 80.00% complete, ETA 00:01:40
# Fork: 5 of 5
# Warmup Iteration   1: 3398845405.179 ops/s
# Warmup Iteration   2: 3716777120.646 ops/s
# Warmup Iteration   3: 3414803497.798 ops/s
# Warmup Iteration   4: 3621211396.229 ops/s
# Warmup Iteration   5: 3616308570.681 ops/s
Iteration   1: 3898056365.287 ops/s
Iteration   2: 3935143498.460 ops/s
Iteration   3: 3943901632.014 ops/s
Iteration   4: 3906292827.077 ops/s
Iteration   5: 3918607665.065 ops/s


Result "cn.zzs.jmh.MyBenchmark.testMethod":
  3949010528.035 ±(99.9%) 16881035.344 ops/s [Average]
  (min, avg, max) = (3898056365.287, 3949010528.035, 3975167080.768), stdev = 22535699.213
  CI (99.9%): [3932129492.691, 3965891563.378] (assumes normal distribution)

# Run complete. Total time: 00:08:21

Benchmark                Mode  Cnt           Score          Error  Units
MyBenchmark.testMethod  thrpt   25  3949010528.035 ± 16881035.344  ops/s
```

在头部分打印了`MyBenchmark.testMethod`这个 Benchmark 方法的配置信息，如下：

```powershell
# JMH version: 1.25
# VM version: JDK 1.8.0_231, Java HotSpot(TM) 64-Bit Server VM, 25.231-b11
# VM invoker: D:\growUp\installation\jdk1.8.0_231\jre\bin\java.exe
# VM options: <none>  
# Warmup: 5 iterations, 10 s each  ---------------预热5个迭代，每个迭代10s
# Measurement: 5 iterations, 10 s each------------正式测试5个迭代，每个迭代10s
# Timeout: 10 min per iteration-------------------每个迭代的超时时间10min
# Threads: 1 thread, will synchronize iterations--使用1个线程测试
# Benchmark mode: Throughput, ops/time------------使用吞吐量作为测试指标
# Benchmark: cn.zzs.jmh.MyBenchmark.testMethod
```

在最后打印了这个 Benchmark 方法的测试结果，如下。它的吞吐是 3949010528.035 ± 16881035.344  ops/s。注意，**一个 Benchmark 的测试结果是没有意义的，只有多个 Benchmark 对比才可能得出结论**。

```powershell

Benchmark                Mode  Cnt           Score          Error  Units
MyBenchmark.testMethod  thrpt   25  3949010528.035 ± 16881035.344  ops/s
```

# 详细配置

通过上面的入门例子简单介绍了如何使用 JMH，接下来将继续对Benchmark 方法的配置 。针对这一点，官方没有给出具体的文档，而是提供了 30 多个示例代码供我们学习[JMH Samples](http://hg.openjdk.java.net/code-tools/jmh/file/tip/jmh-samples/src/main/java/org/openjdk/jmh/samples/) 。

这些示例代码并不好读懂，尤其是涉及到 JVM 的部分。其实，只要我们读懂 1-11、20 的例子就行，这些例子已经足够我们日常使用。

至于其他的，大多是介绍 JVM 或者本地机器的某些机制将影响到测试的准确性，以及通过什么方法减少这些影响，非常难懂。本文不会介绍这部分内容，大部分情况下，JMH 已经尽量为我们屏蔽这些因素带来的影响，我们只要使用默认配置就可以。

以下只针对 1-11、20 的例子进行总结和补充。有误的地方，欢迎指正。

在介绍以下内容之前，这里先介绍下一个 Benchmark 方法的组成部分（只是一个大致结果，并不准确），如下。要很好地理解后面的内容，最后掌握这个结构。

```java
//Benchmark
public void Benchmark01(){
    // ······
    // 预热
    // 每个循环为一个iteration
    for(iterations){
        // 每个循环为一个invocation
    	while(!timeout){
            // 调用我们的测试方法
        }
	}
	// ······
    // 测试
    // 每个循环为一个iteration
    for(iterations){
        // 每个循环为一个invocation，这里会统计每次invocation的开销
    	while(!timeout){
            // 调用我们的测试方法
        }
	}
    // ······
}
```

## @Benchmark

`@Benchmark`用于告诉 JMH 哪些方法需要进行测试，只能注解在方法上，有点类似 junit 的`@Test`。在测试项目进行 package 时，JMH 会针对注解了`@Benchmark`的方法生成 Benchmark 方法代码。

```java
    @Benchmark
    public void wellHelloThere() {
        // this method was intentionally left blank.
    }
```

通常情况下，每个 Benchmark 方法都运行在独立的进程中，互不干涉。


## @BenchmarkMode

`@BenchmarkMode`用于指定当前 Benchmark 方法使用哪种模式测试。JMH 提供了4种不同的模式，用于输出不同的结果指标，如下：

| 模式           | 描述                                                         |
| -------------- | ------------------------------------------------------------ |
| Throughput     | 吞吐量，ops/time。单位时间内执行操作的平均次数               |
| AverageTime    | 每次操作所需时间，time/op。执行每次操作所需的平均时间        |
| SampleTime     | 同 AverageTime。区别在于 SampleTime 会统计取样 x% 达到了多少 time/op，如下。<br><img src="https://img2020.cnblogs.com/blog/1731892/202008/1731892-20200829110311905-378550752.png" alt="sample_time_01" style="zoom:67%;" /> |
| SingleShotTime | 同 AverageTime。区别在于 SingleShotTime 只执行一次操作。这种模式的结果存在较大随机性。 |

`@BenchmarkMode`支持数组，也就是说可以为同一个方法同时指定多种模式，生成基准测试代码时，JMH 将按照不同模式分别生成多个独立的 Benchmark 方法。另外，我们可以使用`@OutputTimeUnit`来指定时间单位，可以精确到纳秒级别。

```java
    /*
     * 使用一种模式
     */
    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public void measureThroughput() throws InterruptedException {
        TimeUnit.MILLISECONDS.sleep(100);
    }
    /*
     * 使用多种模式
     */
    @Benchmark
    @BenchmarkMode({Mode.Throughput, Mode.AverageTime, Mode.SampleTime, Mode.SingleShotTime})
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    public void measureMultiple() throws InterruptedException {
        TimeUnit.MILLISECONDS.sleep(100);
    }
    /*
     * 使用所有模式
     */
    @Benchmark
    @BenchmarkMode(Mode.All)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    public void measureAll() throws InterruptedException {
        TimeUnit.MILLISECONDS.sleep(100);
    }
```

## @Warmup和@Measurement

`@Warmup` 和`@Measurement`分别用于配置预热迭代和测试迭代。其中，iterations 用于指定迭代次数，time 和 timeUnit 用于每个迭代的时间，batchSize 表示执行多少次 Benchmark 方法为一个 invocation。

```java
    @Benchmark
    @Warmup(iterations = 5, time = 100, timeUnit = TimeUnit.MILLISECONDS, batchSize = 10)
    @Measurement(iterations = 5, time = 100, timeUnit = TimeUnit.MILLISECONDS, batchSize = 10)
    public double measure() {
        //······
    }
```

## @State

个人理解，State 就是被注入到 Benchmark 方法中的对象，它的数据和方法可以被 Benchmark 方法使用。在 JMH 中，注解了`@State`的类在测试项目进行 package 时可以被注入到 Benchmark 方法中。

### 配置方式

State 的配置方式有两种。

第一种是 Benchmark 不在 State 的类里。这时需要在测试方法的入参列表里显式注入该 State。

```java
public class JMHSample_03_States {

    @State(Scope.Benchmark)
    public static class BenchmarkState {
        volatile double x = Math.PI;
    }

    @State(Scope.Thread)
    public static class ThreadState {
        volatile double x = Math.PI;
    }

    @Benchmark
    public void measureUnshared(ThreadState state) {
        state.x++;
    }

    @Benchmark
    public void measureShared(BenchmarkState state) {
        state.x++;
    }
}
```

第二种是 Benchmark 在 State 的类里。这时不需要在测试方法的入参列表里显式注入该 State。

```java
@State(Scope.Thread)
public class JMHSample_04_DefaultState {

    double x = Math.PI;

    @Benchmark
    public void measure() {
        x++;
    }

}
```

### Scope

Scope 是`@State`的属性，用于描述 State 的作用范围，主要有三种：

| scope     | 描述                                                         |
| --------- | ------------------------------------------------------------ |
| Benchmark | Benchmark 中所有线程都使用同一个 State                       |
| Group     | Benchmark 中同一 Benchmark 组（使用@Group标识，后面再讲）使用一个 State |
| Thread    | Benchmark 中每个线程使用同一个 State                         |

### @Setup 和 @TearDown

这两个注解只能定义在注解了 State 里，其中，`@Setup`类似于 junit 的`@Before`，而`@TearDown`类似于 junit 的`@After`。

```java
@State(Scope.Thread)
public class JMHSample_05_StateFixtures {

    double x;

    @Setup(Level.Iteration)
    public void prepare() {
        System.err.println("init............");
        x = Math.PI;
    }

    @TearDown(Level.Iteration)
    public void check() {
        System.err.println("destroy............");
        assert x > Math.PI : "Nothing changed?";
    }


    @Benchmark
    public void measureRight() {
        x++;
    }

}
```

这两个注解注释的方法的调用时机，主要受 Level 的控制，JMH 提供了三种 Level，如下：

1. Trial

Benchmark 开始前或结束后执行，如下。Level 为 Benchmark 的 Setup 和 TearDown 方法的开销不会计入到最终结果。

```java
//Benchmark
public void Benchmark01(){
    // call Setup method
    // 每个循环为一个iteration
    for(iterations){
        // 每个循环为一个invocation，这里会统计每次invocation的开销
    	while(!timeout){
            // 调用我们的测试方法
        }
	}
    // call TearDown method
}
```

2. Iteration

Benchmark 里每个 Iteration 开始前或结束后执行，如下。Level 为 Iteration 的 Setup 和 TearDown 方法的开销不会计入到最终结果。

```java
//Benchmark
public void Benchmark01(){
    // 每个循环为一个iteration
    for(iterations){
        // call Setup method
        // 每个循环为一个invocation，这里会统计每次invocation的开销
    	while(!timeout){
            // 调用我们的测试方法
        }
        // call TearDown method
	}
}
```

3. Invocation

Iteration 里每次方法调用开始前或结束后执行，如下。**Level 为 Invocation 的 Setup 和 TearDown 方法的开销将计入到最终结果**。

```java
//Benchmark
public void Benchmark01(){
    // 每个循环为一个iteration
    for(iterations){
        // 每个循环为一个invocation，这里会统计每次invocation的开销
    	while(!timeout){
            // call Setup method
            // 调用我们的测试方法
            // call TearDown method
        }
	}
}
```

以上内容基本可以满足 JMH 的日常使用需求，至于其他示例的内容，后面有空再做补充。

# 参考资料

[openjdk官网](http://openjdk.java.net/projects/code-tools/jmh/)

> 相关源码请移步：[https://github.com/ZhangZiSheng001/jmh-demo](https://github.com/ZhangZiSheng001/jmh-demo)

>本文为原创文章，转载请附上原文出处链接：[https://www.cnblogs.com/ZhangZiSheng001/p/13581390.html](https://www.cnblogs.com/ZhangZiSheng001/p/13581390.html) 