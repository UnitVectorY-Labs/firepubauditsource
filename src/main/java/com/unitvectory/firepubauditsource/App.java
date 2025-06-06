/*
 * Copyright 2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.unitvectory.firepubauditsource;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * The Spring Boot 3 app for the firepubauditsource Cloud Run application.
 * 
 * @author Jared Hatfield (UnitVectorY Labs)
 */
@SpringBootApplication
@ComponentScan(basePackages = {"com.unitvectory.firepubauditsource"})
public class App {
    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }
}