package com.decurtis;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

@RestController
public class HelloController {

    @RequestMapping("/")
    public String index() {
        return "Greetings from Spring Boot2!";
    }

    @RequestMapping("/healthz")
    public String healthz() {
    	/*this endpoint will be used for checking all the necessary checks related to health*/
    	/*for example like all the db connections, redis, elasticsearch connections, java memory, beans availability etc*/ 
        return "I am live!";
    }

}