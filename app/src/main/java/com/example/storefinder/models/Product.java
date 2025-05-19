package com.example.storefinder.models;

/**
 * Класс представляет товар в магазине.
 */
public class Product {
    private long id;
    private String name;
    private long sectionId; // Идентификатор раздела магазина

    public Product() {
    }

    public Product(long id, String name, long sectionId) {
        this.id = id;
        this.name = name;
        this.sectionId = sectionId;
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

    public long getSectionId() {
        return sectionId;
    }

    public void setSectionId(long sectionId) {
        this.sectionId = sectionId;
    }

    @Override
    public String toString() {
        return name;
    }
}