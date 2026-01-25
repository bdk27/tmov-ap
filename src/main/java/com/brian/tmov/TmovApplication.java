package com.brian.tmov;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@SpringBootApplication
@EnableCaching
@EnableScheduling
public class TmovApplication {

	public static void main(String[] args) {
		SpringApplication.run(TmovApplication.class, args);
	}

	@PostConstruct
	public void init() {
		TimeZone.setDefault(TimeZone.getTimeZone("Asia/Taipei"));
		System.out.println("目前時區已設定為: " + TimeZone.getDefault().getID() + " (" + TimeZone.getDefault().getDisplayName() + ")");
	}
}
