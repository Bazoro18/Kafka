package com.kafka.ride;
public class RideRequest {
    private String rideId;
    private String userId;
    private String pickup;
    private String drop;
    private String paymentMethod;
    private double amount;

    // Default constructor (required for Jackson)
    public RideRequest() {}

    public RideRequest(String rideId, String userId, String pickup, String drop, String paymentMethod, double amount) {
        this.rideId = rideId;
        this.userId = userId;
        this.pickup = pickup;
        this.drop = drop;
        this.paymentMethod = paymentMethod;
        this.amount = amount;
    }

    // Getters and setters
    public String getRideId() { return rideId; }
    public String getUserId() { return userId; }
    public String getPickup() { return pickup; }
    public String getDrop() { return drop; }
    public String getPaymentMethod() { return paymentMethod; }
    public double getAmount() { return amount; }

    public void setRideId(String rideId) { this.rideId = rideId; }
    public void setUserId(String userId) { this.userId = userId; }
    public void setPickup(String pickup) { this.pickup = pickup; }
    public void setDrop(String drop) { this.drop = drop; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public void setAmount(double amount) { this.amount = amount; }
}
