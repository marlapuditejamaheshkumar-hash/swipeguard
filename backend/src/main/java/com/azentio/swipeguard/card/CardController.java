package com.azentio.swipeguard.card;

import com.azentio.swipeguard.audit.AuditEvent;
import com.azentio.swipeguard.audit.AuditRepository;
import com.azentio.swipeguard.common.BadRequestException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/cards")
public class CardController {

    private static final Pattern CARD_REF = Pattern.compile("[0-9a-f]{64}");

    public record ControlRequest(@NotBlank @Size(max = 500) String reason, UUID alertId) { }

    public record CardStateResponse(String cardRef, String state) { }

    private final CardControlService service;
    private final AuditRepository auditRepository;

    public CardController(CardControlService service, AuditRepository auditRepository) {
        this.service = service;
        this.auditRepository = auditRepository;
    }

    @PostMapping("/{cardRef}/block")
    public CardStateResponse block(@PathVariable String cardRef,
                                   @RequestHeader(value = "X-Actor", required = false) String actor,
                                   @Valid @RequestBody ControlRequest body) {
        requireCardRef(cardRef);
        service.block(cardRef, requireActor(actor), body.reason().trim(), body.alertId());
        return new CardStateResponse(cardRef, "BLOCKED");
    }

    @PostMapping("/{cardRef}/unblock")
    public CardStateResponse unblock(@PathVariable String cardRef,
                                     @RequestHeader(value = "X-Actor", required = false) String actor,
                                     @Valid @RequestBody ControlRequest body) {
        requireCardRef(cardRef);
        service.unblock(cardRef, requireActor(actor), body.reason().trim(), body.alertId());
        return new CardStateResponse(cardRef, "ACTIVE");
    }

    @GetMapping("/{cardRef}/audit")
    public List<AuditEvent> audit(@PathVariable String cardRef) {
        requireCardRef(cardRef);
        return auditRepository.findByCard(cardRef);
    }

    private static String requireActor(String actor) {
        if (actor == null || actor.isBlank() || actor.length() > 100) {
            throw new BadRequestException("X-Actor", "is required (max 100 characters)");
        }
        return actor.trim();
    }

    private static void requireCardRef(String cardRef) {
        if (!CARD_REF.matcher(cardRef).matches()) {
            throw new BadRequestException("cardRef", "must be a 64-character hex reference");
        }
    }
}