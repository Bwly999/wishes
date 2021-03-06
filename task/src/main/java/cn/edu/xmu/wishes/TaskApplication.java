package cn.edu.xmu.wishes;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication(scanBasePackages = {"cn.edu.xmu.wishes.core", "cn.edu.xmu.wishes.task", "cn.edu.xmu.wishes.user"})
@MapperScan({"cn.edu.xmu.wishes.task.mapper", "cn.edu.xmu.wishes.user.mapper"})
@EnableConfigurationProperties
public class TaskApplication {
    public static void main(String[] args) {
        SpringApplication.run(TaskApplication.class, args);
    }
}
