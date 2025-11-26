package app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.retry.annotation.EnableRetry;
@EnableRetry
@SpringBootApplication
@EnableFeignClients(basePackages = "app.lunch")
public class SchoolLunchApplication {

	public static void main(String[] args) {
		SpringApplication.run(SchoolLunchApplication.class, args);

	}

}
