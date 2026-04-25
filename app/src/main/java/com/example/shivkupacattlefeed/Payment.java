package com.example.shivkupacattlefeed;

public class Payment {
    public String paymentId;
    public String farmerName;
    public String farmerMobile;
    public String dealerId;
    public String amount;
    public String remark;
    public long timestamp;

    public Payment() {
    }

    public Payment(String paymentId, String farmerName, String farmerMobile, String dealerId, String amount, long timestamp) {
        this.paymentId = paymentId;
        this.farmerName = farmerName;
        this.farmerMobile = farmerMobile;
        this.dealerId = dealerId;
        this.amount = amount;
        this.timestamp = timestamp;
    }

    public Payment(String paymentId, String farmerName, String farmerMobile, String dealerId, String amount, String remark, long timestamp) {
        this.paymentId = paymentId;
        this.farmerName = farmerName;
        this.farmerMobile = farmerMobile;
        this.dealerId = dealerId;
        this.amount = amount;
        this.remark = remark;
        this.timestamp = timestamp;
    }
}
