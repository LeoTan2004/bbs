package edu.xtu.bbs.user.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
@RequestMapping("/home")
public class Hello {
    @GetMapping("/")
    public Map<String, String> hello() {
        return Map.of("message", "Hello, BBS User Service is running!");
    }
}
