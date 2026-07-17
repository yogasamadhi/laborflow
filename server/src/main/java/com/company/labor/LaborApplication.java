package com.company.labor;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan({
        "com.company.labor.module.system.mapper",
        "com.company.labor.module.employee.mapper",
        "com.company.labor.module.salary.mapper",
        "com.company.labor.module.file.mapper",
        "com.company.labor.module.project.mapper",
        "com.company.labor.module.compensation.mapper",
        "com.company.labor.security.mapper"
})
public class LaborApplication {
    public static void main(String[] args) {
        SpringApplication.run(LaborApplication.class, args);
    }
}
