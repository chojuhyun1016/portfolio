package com.example.named.lock.cli;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;


@SpringBootApplication
@EnableScheduling
public class CliApplication
{
	public static void main(String[] args)
	{
		SpringApplication.run(CliApplication.class, args);
	}

}
