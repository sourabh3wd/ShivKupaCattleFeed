package com.example.shivkupacattlefeed;

public class Order {
    public String orderId;
    public String productId;
    public String productName;
    public String farmerId;
    public String farmerName;
    public String farmerMobile;
    public String dealerId;
    public String quantity;
    public String totalPrice;
    public String paidAmount; // किती पैसे मिळाले
    public String balanceAmount; // किती बाकी आहेत
    public String status; // Sold, Pending, etc.
    public String paymentMethod; // Cash, Credit (Udhar)
    public String dueDate;
    public String remark; // शेरा
    public long timestamp;

    public Order() {
    }

    public Order(String orderId, String productId, String productName, String farmerId, String farmerName, String farmerMobile, String dealerId, String quantity, String totalPrice, String paidAmount, String balanceAmount, String status, String paymentMethod, String dueDate, long timestamp) {
        this.orderId = orderId;
        this.productId = productId;
        this.productName = productName;
        this.farmerId = farmerId;
        this.farmerName = farmerName;
        this.farmerMobile = farmerMobile;
        this.dealerId = dealerId;
        this.quantity = quantity;
        this.totalPrice = totalPrice;
        this.paidAmount = paidAmount;
        this.balanceAmount = balanceAmount;
        this.status = status;
        this.paymentMethod = paymentMethod;
        this.dueDate = dueDate;
        this.timestamp = timestamp;
    }

    public Order(String orderId, String productId, String productName, String farmerId, String farmerName, String farmerMobile, String dealerId, String quantity, String totalPrice, String paidAmount, String balanceAmount, String status, String paymentMethod, String dueDate, String remark, long timestamp) {
        this(orderId, productId, productName, farmerId, farmerName, farmerMobile, dealerId, quantity, totalPrice, paidAmount, balanceAmount, status, paymentMethod, dueDate, timestamp);
        this.remark = remark;
    }
}
