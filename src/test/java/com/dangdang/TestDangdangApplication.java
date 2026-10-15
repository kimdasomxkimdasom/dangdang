package com.dangdang;

import org.springframework.boot.SpringApplication;

public class TestDangdangApplication {

	public static void main(String[] args) {
		SpringApplication.from(DangdangApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
