package com.kafka.ride;

public class RideRequest {
    public String rideId;
    public String userId;
    public String pickup;
    public String drop;
    public String paymentMethod;
    public double amount;

    public RideRequest() {
    }

    public RideRequest(String rideId, String userId, String pickup, String drop, String paymentMethod, double amount) {
        this.rideId = rideId;
        this.userId = userId;
        this.pickup = pickup;
        this.drop = drop;
        this.paymentMethod = paymentMethod;
        this.amount = amount;
    }

    public String getRideId() {
        return rideId;
    }

    public void setRideId(String rideId) {
        this.rideId = rideId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getPickup() {
        return pickup;
    }

    public void setPickup(String pickup) {
        this.pickup = pickup;
    }

    public String getDrop() {
        return drop;
    }

    public void setDrop(String drop) {
        this.drop = drop;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }
}
