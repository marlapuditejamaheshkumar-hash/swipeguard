package com.azentio.swipeguard.authorisation;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/authorisations")
public class AuthorisationController {

    private final AuthorisationService service;

    public AuthorisationController(AuthorisationService service) {
        this.service = service;
    }

    @PostMapping("/check")
    public AuthorisationResponse check(@Valid @RequestBody AuthorisationRequest request) {
        return service.check(request);
    }
}