package app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
@EnableAsync
@EnableScheduling
@EnableRetry
@SpringBootApplication
@EnableFeignClients(basePackages = "app.lunch")
public class SchoolLunchApplication {

	public static void main(String[] args) {
		SpringApplication.run(SchoolLunchApplication.class, args);

	}

}
