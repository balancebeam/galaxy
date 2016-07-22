package io.anyway.galaxy.testcase;

import io.anyway.galaxy.annotation.TXAction;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Created by yangzz on 16/7/20.
 */

@Component
public class Test {

    @Transactional
    @TXAction(timeout = 60)
    public void fn(){
        System.out.println("123");
    }

    public static void main(String[] args){
        ClassPathXmlApplicationContext ctx= new ClassPathXmlApplicationContext("classpath:applicationContext.xml");
        ctx.refresh();

        System.out.println("!!!Start!!!");
        long startTime = System.currentTimeMillis();
        Test test= ctx.getBean(Test.class);
        test.fn();
        System.out.println("!!!OK!!! Spent time = " + (System.currentTimeMillis() - startTime) + "MS");
    }
}
