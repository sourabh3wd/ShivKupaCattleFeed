package com.example.shivkupacattlefeed;

public class Product {
    public String id;
    public String category;
    public String brand;
    public String variant;
    public String price;       // Price per Unit
    public String quantity;    // Total quantity
    public String unit;        // Bag, Kg, Nos
    public String weightPerUnit;
    public String description;
    public String dealerId;
    public String imageUrl; // मालाच्या फोटोसाठी

    public Product() {
    }

    public Product(String id, String category, String brand, String variant, String price, String quantity, String unit, String weightPerUnit, String description, String dealerId) {
        this.id = id;
        this.category = category;
        this.brand = brand;
        this.variant = variant;
        this.price = price;
        this.quantity = quantity;
        this.unit = unit;
        this.weightPerUnit = weightPerUnit;
        this.description = description;
        this.dealerId = dealerId;
    }

    public Product(String id, String category, String brand, String variant, String price, String quantity, String unit, String weightPerUnit, String description, String dealerId, String imageUrl) {
        this(id, category, brand, variant, price, quantity, unit, weightPerUnit, description, dealerId);
        this.imageUrl = imageUrl;
    }
}
