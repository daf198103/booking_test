package com.book.bookhost;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@EnableAutoConfiguration
@SpringBootApplication
public class BookhostApplication {

	public static void main(String[] args) {
		SpringApplication.run(BookhostApplication.class, args);
	}

}
