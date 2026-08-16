package com.cms.cms_back.admin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.cms.cms_back")
public class CmsBackApplication {

	public static void main(String[] args) {
		SpringApplication.run(CmsBackApplication.class, args);
	}

}
