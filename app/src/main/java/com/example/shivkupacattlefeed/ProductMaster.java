package com.example.shivkupacattlefeed;

public class ProductMaster {
    public String id;
    public String brand;
    public String productName;
    public String dealerId;

    public ProductMaster() {
        // Required for Firebase
    }

    public ProductMaster(String id, String brand, String productName, String dealerId) {
        this.id = id;
        this.brand = brand;
        this.productName = productName;
        this.dealerId = dealerId;
    }
}
