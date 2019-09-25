package hikari.mvc.controller;

import hikari.mvc.annotation.HikariController;
import hikari.mvc.annotation.HikariRequestMapping;
import hikari.mvc.annotation.HikariRequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@HikariController
@HikariRequestMapping("/test")
public class TestController {

    @HikariRequestMapping("doTest")
    public void test1(HttpServletRequest request, HttpServletResponse response,
                      @HikariRequestParam("param") String param) {
        System.out.println(param);
        try {
            response.getWriter().write("doTest method success! param:" + param);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @HikariRequestMapping("/doTest2")
    public void test2(HttpServletRequest request, HttpServletResponse response) {
        try {
            response.getWriter().println("doTest2 method success!");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
