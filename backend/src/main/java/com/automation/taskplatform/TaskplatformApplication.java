package com.automation.taskplatform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TaskplatformApplication {

	public static void main(String[] args) {
		SpringApplication.run(TaskplatformApplication.class, args);
	}

}
