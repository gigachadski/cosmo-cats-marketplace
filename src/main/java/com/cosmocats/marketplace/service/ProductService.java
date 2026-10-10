package com.cosmocats.marketplace.service;

import com.cosmocats.marketplace.domain.Product;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ProductService {

    public static Map<Long, Product> products = new HashMap<>();

    static {
        products.put(1L, new Product(1L, "Star Tuna Can", "Tuna caught near Sirius", 9.99, "food", "seller@cosmocats.space"));
        products.put(2L, new Product(2L, "Galaxy Yarn Ball", "Glows in the dark", 4.5, "toys", "yarn@cosmocats.space"));
        products.put(3L, new Product(3L, "Comet Scratching Post", "Fast as a comet", 49.9, "furniture", "post@cosmocats.space"));
    }

    public List<Product> getAll() {
        return new ArrayList<>(products.values());
    }

    public Product getById(Long id) {
        return products.get(id);
    }

    public Product create(Product product) {
        Long id = (long) (products.size() + 1);
        product.setId(id);
        products.put(id, product);
        System.out.println("Created product " + product.getName());
        return product;
    }

    public Product update(Long id, Product product) {
        product.setId(id);
        products.put(id, product);
        return product;
    }

    public void delete(Long id) {
        try {
            products.remove(id);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
