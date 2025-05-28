package com.marecca.workoutTracker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;

@SpringBootApplication
public class WorkoutTrackerApplication {
	public static void main(String[] args) {
		SpringApplication.run(WorkoutTrackerApplication.class, args);
	}
}
