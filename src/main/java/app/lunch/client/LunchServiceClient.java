package app.lunch.client;

import app.lunch.client.dto.LunchOrder;
import app.lunch.client.dto.LunchOrderRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.UUID;

@FeignClient(name = "lunch-service", url = "${lunch-svc.base-url}")
public interface LunchServiceClient {

    @GetMapping("/children/{childId}/lunches")
    List<LunchOrder> getLunches(@PathVariable UUID childId);

    @PostMapping("/children/{childId}/lunches")
    LunchOrder createLunch(@PathVariable UUID childId, @RequestBody LunchOrderRequest lunchRequest);

    @DeleteMapping("/children/{childId}/lunches/{lunchId}")
    void deleteLunch(@PathVariable UUID childId, @PathVariable UUID lunchId);
}
