package com.votricuong.mayhotel;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MayhotelApplication {

	public static void main(String[] args) {
		SpringApplication.run(MayhotelApplication.class, args);
	}

}
