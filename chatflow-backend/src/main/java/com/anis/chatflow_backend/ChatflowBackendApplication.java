package com.anis.chatflow_backend;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.mongodb.core.MongoTemplate;

@SpringBootApplication
public class ChatflowBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(ChatflowBackendApplication.class, args);
	}

	@Bean
	ApplicationRunner startupSuccessLogger(MongoTemplate mongoTemplate) {
		return new ApplicationRunner() {
			@Override
			public void run(ApplicationArguments args) {
				mongoTemplate.executeCommand("{ ping: 1 }");
				System.out.println("SUCCESS: MongoDB connected");
				System.out.println("SUCCESS: Server started on port 8080");
			}
		};
	}

}
