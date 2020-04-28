package com.pivotal.rabbitmq.gettingstarted;

public class ShipmentConfirmation {
    String id;
    String error;

    public ShipmentConfirmation(String id) {
        this.id = id;
    }
    public ShipmentConfirmation(Throwable error) {
        this.error = error.getMessage();
    }

    public ShipmentConfirmation() {
    }

    public String getId() {
        return id;
    }

    public String getError() {
        return error;
    }
}
