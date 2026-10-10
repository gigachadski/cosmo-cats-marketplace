package com.cosmocats.marketplace.domain;

import java.util.ArrayList;
import java.util.List;

public class Order {

    public Long id;
    public String customerEmail;
    public List<Product> products = new ArrayList<>();
    public Double total = 0.0;
    // "NEW", "PAID", "SHIPPED", "DELIVERED"
    public String status = "NEW";

    public void addProduct(Product product) {
        products.add(product);
        total = total + product.price;
    }
}
