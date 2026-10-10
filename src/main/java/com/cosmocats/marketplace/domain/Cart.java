package com.cosmocats.marketplace.domain;

import java.util.ArrayList;
import java.util.List;

public class Cart {

    public Long id;
    public String customerEmail;
    public List<Product> products = new ArrayList<>();

    public Double getTotal() {
        Double sum = 0.0;
        for (Product p : products) {
            sum += p.price;
        }
        return sum;
    }
}
