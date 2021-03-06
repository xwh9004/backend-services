package com.example.user;

import com.example.user.server.UserServer;
import org.mybatis.spring.annotation.MapperScan;
import org.mybatis.spring.annotation.MapperScans;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * <p><b>Description:</b>
 *
 * <p><b>Company:</b>
 *
 * @author created by Jesse Hsu at 17:50 on 2020/11/2
 * @version V0.1
 * @classNmae UserApplication
 */
@MapperScan("com.example.user.mapper")
@SpringBootApplication(scanBasePackageClasses =UserApplication.class )
public class UserApplication {

    public static void main(String[] args) throws Exception {

        ConfigurableApplicationContext application = SpringApplication.run(UserApplication.class);

//        UserServer server= application.getBean(UserServer.class);
//
//        server.run();

    }


}
