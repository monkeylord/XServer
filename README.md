# XServer
A Xposed Module for Android Penetration Test, with NanoHttpd.

### 背景

渗透测试中经常遇到通信协议的分析或者各类混淆。

此时，若通过静态的代码分析，则定位关键函数时耗时往往很久。此时，直接根据运行时的参数、结果特征直接定位到关键函数，往往能够节省很多时间。

此外，逆向通信协议往往比较麻烦，尤其是测试目的是通信内容而非协议本身时，此时，若不去逆向协议本身，而是直接使用应用内现成的协议，也会省去大量时间。

#### XServer

XServer是一个用于对方法进行分析的Xposed插件，它针对的是“方法”接口。由于人类习惯函数式编程，为了可维护性，往往会把各个功能分别封装进各个类与方法，这成为了程序的弱点。

利用注入和反射，可以记录并拦截方法的调用，也可以在应用自身的运行环境中调用某个具体方法。这就可以对应用的分析起到辅助。

另外，XServer还通过HTTP和WebSocket提供远程动态操作界面，也提供RPC接口供其它工具调用应用内的方法。

### 使用说明

#### 通过Xposed启动

1. 确保Xposed框架已经正确安装
2. 安装XServer并确保在Xposed中启用XServer
3. 在XServer应用选择器中选中目标应用
4. 启动目标应用
   1. 如果XServer没有启动，可能是目标应用早已启动，然后才选择的目标应用，已错过目标应用判断时机。此时，可以关闭目标应用重新打开。
   2. 如果切换目标应用，原目标应用中的XServer依然在工作和占用端口。可以关掉原目标应用再启动新目标应用。
   3. 选择应用后直接重启设备最简单，如果你使用模拟器的话。
5. 通过ADB转发XServer端口：`adb forward tcp:8000 tcp:8000`
   1. 目标应用可能存在多个进程，针对这种情况，XServer在进程PID对应的端口也打开了监听。若8000端口对应的进程不是目标应用主进程，可以使用另一个命令修正：`adb forward tcp:8000 tcp:[目标进程PID]`
6. 通过http://127.0.0.1:8000/ 访问XServer

#### 通过Frida启动

1. 确保Frida-Server已经启动
2. 确保XServer已在设备中
   1. 在目标设备中安装XServer（无需Xposed框架）
   2. 将XServer对应APK放置在`/data/local/tmp/xserver.apk`
3. 使用Frida加载XServer.js以启动XServer：`frida -U [目标应用包名或进程PID] -l XServer.js`
4. 通过ADB转发XServer端口：`adb forward tcp:8000 tcp:8000`
5. 通过http://127.0.0.1:8000/ 访问XServer

#### 通过其他Hook框架启动

XServer内部使用自己定义的Hook接口，因此可以兼容其他Hook框架，只要其他Hook框架实现HookProvider并注册。

1. 将XServer注入并加载到目标应用中

2. 修改XServer属性：assetManager、classLoader

3. 实现HookProvider

   1. 支持动态创建类的框架如Frida可自行实现`monkeylord.XServer.handler.HookHandler$HookProvider`
   2. 也可以通过Hook已有的`monkeylord.XServer.handler.Hook.DummyProvider`来实现

4. 在XServer中注册HookProvider

   ~~~java
   HookHandler.setProvider(yourProviderClass);
   ~~~

5. 启动XServer

   ~~~java
   new XServer(8000);
   ~~~

#### 批量跟踪

1. XServer首页最下方是当前应用已加载的类清单，通过Filter可以根据名称进行过滤
   1. 这个清单不包括系统类
   2. 这个清单对应的是XServer.classLoader中已加载的类
2. 点击Begin MassMonitoring进入批量跟踪界面
3. 在Class Filter中根据类名筛选感兴趣的类（支持正则表达式）
4. 点击Load Methods for Matched Classes来加载符合条件的类中的方法清单
5. 在Method Filter中根据方法名筛选感兴趣的方法（支持正则表达式）
6. 点击Hook Matched Methods监控所有符合以上两个筛选条件的方法。
7. 收起Method Trace Selector观察日志
8. 在Class Tree中可以随时通过勾选和取消勾选来调整某个方法是否被Hook
   1. 某些不重要的方法可能反复触发刷屏，可取消勾选，不再关注这些方法。
9. 可以通过Ctrl+F搜索日志以寻找某些已知的内容是否在某个方法的参数或返回值中。

#### 拦截、修改、重放方法调用

1. 进入目标方法查看界面
   1. 在MassMonitoring界面中点击感兴趣的方法直接进入
   2. 在首页点击对应的类，再点击进入对应方法
2. 部署并启动中间人工具并设置代理进行抓包
   1. XServer报文会使用系统配置的代理
3. 操作应用，目标方法被调用时可在中间人工具中拦截到/invoke2调用
   1. 方法查看界面下方会也会记录方法调用情况，此处同时可以查看方法被调用时的堆栈情况
   2. 方法查看界面中的Invoke是一种简单触发调用的方法，用于调用应用中一些简单的工具方法
4. 在中间人工具中，可以实时修改/invoke2调用中的参数和返回结果，从而改变方法被调用时的参数/返回结果
5. 可以在中间人工具中直接重放/invoke2请求，这会使对应的方法被再次调用
   1. 对于加解密方法，这可以直接调用加解密
   2. 对于网络请求方法，这可以再次触发网络请求

### XServer基础结构

#### 架构

XServer是一个被注入到目标APP中的、具备Hook等能力的HttpServer。

包含两个部分：HttpServer、Xposed入口

Xposed启动HttpServer并提供Hook能力，WebServer负责界面、RPC及各类业务逻辑。

新版的XServer可以在无Xposed的环境下运行，比如使用Frida来启动和Hook。

#### 关键组件

Xposed入口：XposedEntry

应用选择器：MainActivity

HttpServer：XServer

业务逻辑处理：handler

对外接口：api

其它功能组件：utils、objectparser

资源文件：各类freemarker页面（XServer目前使用freemarker作为模板引擎）

#### HttpServer

对应代码：XServer.java

基于NanoHTTPD和NanoWSD开发，实现HTTP路由表与WebSocket路由表，并整合freemarker作为模板引擎。

定义了两类API接口，HTTP API和WebSocket API（Operation、wsOperation），启动后加载的各类功能API都要在此注册。

模板引擎则很简洁，在assets目录中编写模板，然后`XServer.render(data,templete)`即可。

XServer可以动态注册第三方插件，比如新增API，或者告诉XServer特殊对象如何处理，不过目前并没有定义这部分能力。

### API组件

#### Viewer

包含以下API：MemoryView、ClassView、MethodView和wsMethodView

MemoryView提供基本的内存修改、Dump能力。

ClassView提供对类和方法的浏览，用来了解和查看应用的结构。

MethodView和wsMethodView提供对方法调用与返回的实时监控，同时将被监控的方法变为远程调用。

#### Tracer

包含两个API：Tracer和wsTracer

提供对应用执行流的方法粒度跟踪，批量监控并记录方法调用与返回，以及对应的参数和结果。

用于分析应用执行流，定位关键函数。

#### Invoker

包含两个API：Invoke和Invoke2

提供并完成对应用内方法的远程调用，也管理保存的对象实例。

MethodView界面里的Invoke提供的是基本调用，适用于一些简单的情况。

wsMethodView里使用了Invoke2调用，设置了Burp等代理后，可以拦截并修改对应方法的调用，可以处理复杂对象，也支持在Burp等工具中直接重放、爆破等。建议使用Invoke2。

#### Injector

API：TODO

提供动态注入SO，APK的能力，用于启动frida-gadget或动态启动其它Xposed插件。

### Handler组件

#### 处理反射

包含ClassHandler、MethodHandler

用于通过反射获取各种类与方法的对象，提取和处理其中信息。

#### 处理注入

包含两个handler：HookHandler、ObjectHandler

HookHandler用于为其他组件提供Hook能力，并定义了XServer使用的Hook接口。

ObjectHandler用于管理APP中的各类对象实例，在XServer内部收集和复用各类对象实例。Invoke2依赖此功能，Invoke2处理复杂对象时通常不创建对象，而是通过ObjectHandler复用APP自身使用的对象。

#### 深度操作

目前包含MemoryHandler

提供内存操作，利用内部类Libcore实现，没有使用SO。

可以用于Dump内存寻找Demo，也可以动态修改内存。

TODO：提供SO和APK加载

TODO：提供C层Hook，利用SO实现，或许可以使用substrate，或许可以现场gcc？

TODO：集成一些比较有用的Hook

### 对象处理ObjectParser

定义了XServer对于各类对象的序列化、反序列化逻辑。

对应的逻辑在XServer中注册。

目前包括了字符串、字节数组、整型的处理逻辑，以及使用ObjectHandler来存取对象的处理逻辑。

这部分可以较为方便地自行增添和注册，也欢迎PR。