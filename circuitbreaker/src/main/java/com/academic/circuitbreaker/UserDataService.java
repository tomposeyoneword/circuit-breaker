package com.academic.circuitbreaker;

import java.util.Random;

import org.springframework.stereotype.Service;

@Service
public class UserDataService {
	
	public UserData getUserData(String name) {
		UserData data = new UserData();
		data.setName(name);

		// Random number between 18 & 50
		Random r = new Random();
		data.setAge(r.nextInt((50 - 18) + 1) + 18);

		return data;
	}

	public static class UserData {
		public String name;
		public int age;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public int getAge() {
			return age;
		}

		public void setAge(int age) {
			this.age = age;
		}
	}
}
