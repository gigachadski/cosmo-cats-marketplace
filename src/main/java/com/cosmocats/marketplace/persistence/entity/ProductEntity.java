package com.cosmocats.marketplace.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "product")
public class ProductEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "product_seq")
    @SequenceGenerator(name = "product_seq", sequenceName = "product_seq", allocationSize = 50)
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "price", nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private CategoryEntity category;

    @Column(name = "seller_email", nullable = false, length = 254)
    private String sellerEmail;

    protected ProductEntity() {
        // for JPA
    }

    public ProductEntity(String name, String description, BigDecimal price, CategoryEntity category, String sellerEmail) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.category = category;
        this.sellerEmail = sellerEmail;
    }

    /** Copies every editable field; the id never changes. */
    public void replaceWith(String newName, String newDescription, BigDecimal newPrice,
                            CategoryEntity newCategory, String newSellerEmail) {
        this.name = newName;
        this.description = newDescription;
        this.price = newPrice;
        this.category = newCategory;
        this.sellerEmail = newSellerEmail;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public CategoryEntity getCategory() {
        return category;
    }

    public String getSellerEmail() {
        return sellerEmail;
    }
}
