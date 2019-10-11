package hikari.mvc.servlet;

import hikari.mvc.annotation.HikariController;
import hikari.mvc.annotation.HikariRequestMapping;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;

public class HikariDispatcherServlet extends HttpServlet {
    private Properties properties = new Properties();
    private List<String> classNames = new ArrayList<>();
    private Map<String, Object> ioc = new HashMap<>();
    private Map<String, Method> handlerMap = new HashMap<>();
    private Map<String, Object> controllerMap = new HashMap<>();

    @Override
    public void init(ServletConfig config) throws ServletException {
        // 1.load config
        doLoadConfig(config.getInitParameter("contextConfigLocation"));
        // 2.scanner package
        doScanner(properties.getProperty("scanPackage"));
        // 3.instantiate
        doInstance();
        // 4.init HandlerMapping
        initHandlerMapping();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            doDispatch(req, resp);
        } catch (Exception e) {
            resp.getWriter().write("500 ! Server Exception");
        }
    }

    private void doDispatch(HttpServletRequest request, HttpServletResponse response) throws Exception {
        if (handlerMap.isEmpty()) {
            return;
        }
        String url = request.getRequestURI();
        String contextPath = request.getContextPath();

        // Splice url and replace multiple / into one /
        url = url.replace(contextPath, "").replaceAll("/+", "/");
        if (!this.handlerMap.containsKey(url)) {
            response.getWriter().write("404 NOT FOUND!");
            return;
        }

        Method method = handlerMap.get(url);
        // Get the parameter list of the method
        Class<?>[] parameterTypes = method.getParameterTypes();
        // Get the requested parameters
        Map<String, String[]> parameterMap = request.getParameterMap();
        // Save parameter values
        Object[] paramValues = new Object[parameterTypes.length];

        for (int i = 0; i < parameterTypes.length; i++) {
            String requestParam = parameterTypes[i].getSimpleName();
            if ("HttpServletRequest".equals(requestParam)) {
                paramValues[i] = request;
                continue;
            }
            if ("HttpServletResponse".equals(requestParam)) {
                paramValues[i] = response;
                continue;
            }
            if (requestParam.equals("String")) {
                for (Map.Entry<String, String[]> param : parameterMap.entrySet()) {
                    String value = Arrays.toString(param.getValue()).replaceAll("\\[|\\]", "")
                            .replaceAll(",\\s", ",");
                    paramValues[i] = value;
                }
            }
        }

        // Obj is the instance corresponding to the method in the ioc container
        method.invoke(controllerMap.get(url), paramValues);
    }

    /**
     * load config with the specified location
     *
     * @param location config file location
     */
    private void doLoadConfig(String location) {
        try (InputStream resourceAsStream = this.getClass().getClassLoader().getResourceAsStream(location)) {
            assert resourceAsStream != null;
            properties.load(resourceAsStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * scanner package
     *
     * @param packageName package name
     */
    private void doScanner(String packageName) {
        URL url = this.getClass().getClassLoader().getResource("/" + packageName.replaceAll("\\.", "/"));
        assert url != null;
        File dir = new File(url.getFile());
        for (File file : dir.listFiles()) {
            if (file.isDirectory()) {
                doScanner(packageName + "." + file.getName());
            } else {
                String className = packageName + "." + file.getName().replace(".class", "");
                classNames.add(className);
            }
        }
    }

    /**
     * Get the scanned class, instantiate it through the reflection mechanism,
     * and put it in the ioc container (k-v beanName-bean) beanName defaults to the first letter lowercase
     */
    private void doInstance() {
        if (classNames.isEmpty()) {
            return;
        }
        for (String className : classNames) {
            try {
                Class<?> clazz = Class.forName(className);
                if (clazz.isAnnotationPresent(HikariController.class)) {
                    ioc.put(toLowerFirstWord(clazz.getSimpleName()), clazz.newInstance());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Initialize HandlerMapping ( url -- method)
     */
    private void initHandlerMapping() {
        if (ioc.isEmpty()) {
            return;
        }
        try {
            for (Map.Entry<String, Object> entry : ioc.entrySet()) {
                Class<?> clazz = entry.getValue().getClass();
                if (!clazz.isAnnotationPresent(HikariController.class)) {
                    continue;
                }
                String baseUrl = "";
                if (clazz.isAnnotationPresent(HikariRequestMapping.class)) {
                    HikariRequestMapping annotation = clazz.getAnnotation(HikariRequestMapping.class);
                    baseUrl = annotation.value();
                }
                Method[] methods = clazz.getMethods();
                for (Method method : methods) {
                    if (!method.isAnnotationPresent(HikariRequestMapping.class)) {
                        continue;
                    }
                    HikariRequestMapping annotation = method.getAnnotation(HikariRequestMapping.class);
                    String url = annotation.value();
                    url = (baseUrl + "/" + url).replaceAll("/+", "/");
                    handlerMap.put(url, method);
                    controllerMap.put(url, clazz.newInstance());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * lowercase the first letter
     *
     * @param name name
     * @return string
     */
    private String toLowerFirstWord(String name) {
        char[] charArray = name.toCharArray();
        charArray[0] += 32;
        return String.valueOf(charArray);
    }
}
