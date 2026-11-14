package com.azentio.swipeguard.alert;

import com.azentio.swipeguard.common.BadRequestException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/alerts")
public class AlertController {

    private final AlertRepository repository;

    public AlertController(AlertRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public AlertPage list(@RequestParam(required = false) AlertStatus status,
                          @RequestParam(defaultValue = "0") int page,
                          @RequestParam(defaultValue = "20") int size) {
        if (page < 0) {
            throw new BadRequestException("page", "must be 0 or greater");
        }
        if (size < 1 || size > 100) {
            throw new BadRequestException("size", "must be between 1 and 100");
        }
        return new AlertPage(repository.find(status, page, size), page, size, repository.count(status));
    }
}