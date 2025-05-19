package com.example.storefinder.models;

import java.util.ArrayList;
import java.util.List;

/**
 * Класс представляет раздел магазина на схеме.
 */
public class StoreSection {
    private long id;
    private String name;
    private float x; // Координата X на схеме
    private float y; // Координата Y на схеме
    private List<Product> products;

    public StoreSection() {
        this.products = new ArrayList<>();
    }

    public StoreSection(long id, String name, float x, float y) {
        this.id = id;
        this.name = name;
        this.x = x;
        this.y = y;
        this.products = new ArrayList<>();
    }

    // Геттеры и сеттеры
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public float getX() {
        return x;
    }

    public void setX(float x) {
        this.x = x;
    }

    public float getY() {
        return y;
    }

    public void setY(float y) {
        this.y = y;
    }

    public List<Product> getProducts() {
        return products;
    }

    public void setProducts(List<Product> products) {
        this.products = products;
    }

    public void addProduct(Product product) {
        this.products.add(product);
    }

    @Override
    public String toString() {
        return name;
    }
}