package com.kafka.ride;

public class ValidatorUtils {

    public static boolean isValid(RideRequest ride) {
        return ride != null
                && ride.getAmount() > 0
                && ride.getPaymentMethod() != null
                && ride.getPickup() != null
                && ride.getDrop() != null
                && !ride.getPickup().equals(ride.getDrop());
    }
}
