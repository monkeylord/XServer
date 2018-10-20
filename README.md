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

### XServer基础结构

#### 架构

XServer架构包含两个部分：Xposed、WebServer

Xposed启动WebServer并提供方法注入能力，WebServer负责界面、RPC及各类业务逻辑。

#### 组件结构

**Xposed入口：**XposedEntry

**应用选择器：**MainActivity

**WebServer：**XServer

**业务逻辑处理：**handler

**对外接口：**api

**其它功能组件：**utils、objectparser

**资源文件：**各类freemarker页面（XServer用freemarker作为模板引擎）

#### WebServer

基于NanoHTTPD和NanoWSD开发，实现HTTP路由表与WebSocket路由表，并整合freemarker作为模板引擎。

定义了两类API接口，HTTP API和WebSocket API（Operation、wsOperation），启动后加载的各类功能API都要在此注册。

模板引擎则很简洁，在assets目录中编写模板，然后*XServer.render(data,templete)*即可。

### API组件

#### Viewer

包含三个API：MethodView、ClassView和wsMethodView

提供对类和方法的浏览，用来了解和查看应用的结构。

wsMethodView提供对方法调用与返回的实时监控，同时将被监控的方法变为远程调用。

#### Tracer

包含两个API：Tracer和wsTracer

提供对应用执行流的方法粒度跟踪，批量监控并记录方法调用与返回。

#### Invoker

包含两个API：Invoke和ObjManager

提供并完成对应用内方法的远程调用，也管理保存的对象实例。

#### Injector

API：TODO

提供动态注入SO，APK的能力，用于启动frida-gadget或动态启动其它Xposed插件。

### Handler组件

#### 处理反射

包含ClassHandler、MethodHandler

用于通过反射获取各种类与方法的对象，提取和处理其中信息。

#### 处理注入

包含两个handler：HookHandler、ObjectHandler

用于管理各类Hook，收集和管理各类对象实例。

目前重构中，似乎并不需要自己来管理注入，交给Xposed就行。

TODO：处理拦截

#### 深度操作

TODO：提供SO和APK加载

TODO：提供内存操作，利用SO实现

TODO：提供C层Hook，利用SO实现，或许可以使用substrate，或许可以现场gcc？

TODO：集成一些比较有用的Hook
