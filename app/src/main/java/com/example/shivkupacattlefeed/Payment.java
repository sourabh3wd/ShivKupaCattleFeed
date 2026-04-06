package com.example.shivkupacattlefeed;

public class Payment {
    public String paymentId;
    public String farmerName;
    public String dealerId;
    public String amount;
    public long timestamp;

    public Payment() {
    }

    public Payment(String paymentId, String farmerName, String dealerId, String amount, long timestamp) {
        this.paymentId = paymentId;
        this.farmerName = farmerName;
        this.dealerId = dealerId;
        this.amount = amount;
        this.timestamp = timestamp;
    }
}
