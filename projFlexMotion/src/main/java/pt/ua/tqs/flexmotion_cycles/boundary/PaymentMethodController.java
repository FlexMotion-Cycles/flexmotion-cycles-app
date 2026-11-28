package pt.ua.tqs.flexmotion_cycles.boundary;

import java.security.Principal;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import pt.ua.tqs.flexmotion_cycles.dto.PaymentMethodRequest;
import pt.ua.tqs.flexmotion_cycles.dto.PaymentMethodResponse;
import pt.ua.tqs.flexmotion_cycles.services.PaymentMethodService;

/** Cards linked by the authenticated end user in the payment sandbox (US-14). */
@RestController
@RequestMapping("/api/payment-methods")
public class PaymentMethodController {

    private final PaymentMethodService paymentMethodService;

    public PaymentMethodController(PaymentMethodService paymentMethodService) {
        this.paymentMethodService = paymentMethodService;
    }

    /** 201 with the linked card; 402 when the sandbox declines it (BR2). */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentMethodResponse link(@Valid @RequestBody PaymentMethodRequest request,
            Principal principal) {
        return PaymentMethodResponse.from(
                paymentMethodService.link(principal.getName(), request.cardToken()));
    }

    @GetMapping
    public List<PaymentMethodResponse> list(Principal principal) {
        return paymentMethodService.cardsOf(principal.getName()).stream()
                .map(PaymentMethodResponse::from)
                .toList();
    }
}
