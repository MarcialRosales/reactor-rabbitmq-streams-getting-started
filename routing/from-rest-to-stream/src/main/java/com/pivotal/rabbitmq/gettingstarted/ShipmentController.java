package com.pivotal.rabbitmq.gettingstarted;

import com.pivotal.rabbitmq.gettingstarted.schemas.Shipment;
import com.pivotal.rabbitmq.source.Pipeline;
import com.pivotal.rabbitmq.source.Sender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/shipment")
public class ShipmentController {
    public static Logger log = LoggerFactory.getLogger(ShipmentController.class.getName());

    @Autowired
    Sender<Shipment> shipments;

    @PostMapping
    public Mono<ShipmentConfirmation> post(@RequestBody Mono<ShipmentRequest> request){
        return request
                .map(r -> {
                    String id = UUID.randomUUID().toString();
                    log.info("Processing shipment request {} -> {}", r, id);
                    return newShipment(id, r);
                })
                .flatMap(r-> shipments.send(r))
                .doOnNext(s -> log.info("Shipment {} sent ", s.getId()))
                .map(this::toShipmentConfirmation)
                ;
    }
    private Shipment newShipment(String id, ShipmentRequest request) {
        Shipment.Builder shipment = Shipment.newBuilder();
        shipment.setId(id);
        shipment.setCategory1(request.getCategory1());
        shipment.setCategory2(request.getCategory2());
        shipment.setValue(request.getValue());
        shipment.setTransport(request.getTransport());
        return shipment.build();
    }
    private ShipmentConfirmation toShipmentConfirmation(Shipment shipment) {
        return new ShipmentConfirmation(shipment.getId().toString());
    }
}
