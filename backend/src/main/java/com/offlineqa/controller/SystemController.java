package com.offlineqa.controller;

import com.offlineqa.service.SystemStatusService;
import com.offlineqa.service.VolcSearchService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/system")
public class SystemController {

    private final SystemStatusService systemStatusService;
    private final VolcSearchService volcSearchService;

    public SystemController(SystemStatusService systemStatusService, VolcSearchService volcSearchService) {
        this.systemStatusService = systemStatusService;
        this.volcSearchService = volcSearchService;
    }

    @GetMapping("/status")
    public Map<String, Object> status() {
        return systemStatusService.status();
    }

    @GetMapping("/volc-search-test")
    public Map<String, Object> volcSearchTest(@RequestParam("q") String q) {
        return volcSearchService.debugSearch(q);
    }
}
