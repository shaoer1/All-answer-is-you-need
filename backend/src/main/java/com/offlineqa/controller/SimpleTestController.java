package com.offlineqa.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/simple")
public class SimpleTestController {

    @GetMapping("/test")
    public Map<String, String> test() {
        System.out.println("Simple test called");
        return Map.of("message", "Hello, World!");
    }
}
