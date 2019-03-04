package com.dppware.serviceA;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class KafkaExampleApplication {

	 private static ConfigurableApplicationContext context;
	
	public static void main(String[] args) {
		SpringApplication.run(KafkaExampleApplication.class, args);
	}

	/**
	 * Enables Restart
	 */
	public static void restart() {
        ApplicationArguments args = context.getBean(ApplicationArguments.class);
 
        Thread thread = new Thread(() -> {
            context.close();
            context = SpringApplication.run(KafkaExampleApplication.class, args.getSourceArgs());
        });
 
        thread.setDaemon(false);
        thread.start();
    }
}
