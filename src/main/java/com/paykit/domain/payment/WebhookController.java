package com.paykit.domain.payment;

import com.razorpay.Utils;
import com.razorpay.RazorpayException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
public class WebhookController {

    private final PaymentService paymentService;
    private final RazorpayProperties razorpayProperties;

    @PostMapping("/razorpay")
    public ResponseEntity<Void> handleRazorpayWebhook(
            @RequestBody String payload,
            @RequestHeader("X-Razorpay-Signature") String signature) {

        try {
            Utils.verifyWebhookSignature(payload, signature, razorpayProperties.getWebhookSecret());
        } catch (RazorpayException e) {
            log.warn("Invalid Razorpay webhook signature");
            return ResponseEntity.badRequest().build();
        }

        JSONObject event = new JSONObject(payload);
        String eventType = event.getString("event");
        JSONObject paymentEntity = event.getJSONObject("payload")
                .getJSONObject("payment")
                .getJSONObject("entity");

        String razorpayPaymentId = paymentEntity.getString("id");
        String razorpayOrderId = paymentEntity.getString("order_id");

        paymentService.handleWebhookEvent(eventType, razorpayPaymentId, razorpayOrderId);

        return ResponseEntity.ok().build();
    }
}
