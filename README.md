# yuki-mvc
Manually rewrite spring-mvc

SpringMVC本质上是一个DispatcherServlet,这个 Servlet 继承自 HttpServlet

spring mvc 的设计思路
1.读取配置
为了读取web.xml中的配置，我们用到ServletConfig这个类，它代表当前Servlet在web.xml中的配置信息。通过web.xml中加载我们自己写的HikariDispatcherServlet和读取配置文件

2.初始化阶段
DispatcherServlet的initStrategies方法会初始化多种组件，按顺序包含

2.1加载配置文件

2.2扫描用户配置包下面所有的类

2.3拿到扫描到的类，通过反射机制，实例化。并且放到ioc容器中(Map的键值对  beanName-bean) beanName默认是首字母小写

2.4初始化HandlerMapping，这里其实就是把url和method对应起来放在一个k-v的Map中,在运行阶段取出

3.运行阶段
每一次请求将会调用doGet或doPost方法，所以统一运行阶段都放在doDispatch方法里处理，它会根据url请求去HandlerMapping中匹配到对应的Method，然后利用反射机制调用Controller中的url对应的方法，并得到结果返回。按顺序包括以下功能：

3.1异常的拦截

3.2获取请求传入的参数并处理参数

3.3通过初始化好的handlerMapping中拿出url对应的方法名，反射调用