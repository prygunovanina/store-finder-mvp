package com.example.storefinder;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.storefinder.database.StoreDbHelper;
import com.example.storefinder.models.Product;
import com.example.storefinder.models.StoreSection;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import static org.junit.Assert.*;

/**
 * Тесты для проверки работы с базой данных.
 */
@RunWith(AndroidJUnit4.class)
public class StoreDbHelperTest {

    private StoreDbHelper dbHelper;

    @Before
    public void createDb() {
        Context context = ApplicationProvider.getApplicationContext();
        dbHelper = new StoreDbHelper(context);
    }

    @After
    public void closeDb() {
        dbHelper.close();
    }

    /**
     * Тест на сохранение и получение схемы магазина.
     */
    @Test
    public void testSaveAndGetStoreMap() {
        // Сохраняем тестовую схему
        long mapId = dbHelper.saveStoreMap("Test Map", "/test/path/image.jpg");

        // Проверяем, что идентификатор не равен -1 (ошибка вставки)
        assertTrue(mapId != -1);

        // Получаем путь к схеме и проверяем, что он соответствует ожидаемому
        String path = dbHelper.getStoreMapPath(mapId);
        assertEquals("/test/path/image.jpg", path);
    }

    /**
     * Тест на добавление и получение разделов магазина.
     */
    @Test
    public void testAddAndGetSections() {
        // Создаем тестовую схему
        long mapId = dbHelper.saveStoreMap("Test Map", "/test/path/image.jpg");

        // Создаем и добавляем два тестовых раздела
        StoreSection section1 = new StoreSection();
        section1.setName("Section 1");
        section1.setX(100.0f);
        section1.setY(200.0f);

        StoreSection section2 = new StoreSection();
        section2.setName("Section 2");
        section2.setX(300.0f);
        section2.setY(400.0f);

        long sectionId1 = dbHelper.addSection(section1, mapId);
        long sectionId2 = dbHelper.addSection(section2, mapId);

        // Проверяем, что идентификаторы не равны -1 (ошибка вставки)
        assertTrue(sectionId1 != -1);
        assertTrue(sectionId2 != -1);

        // Получаем список всех разделов для данной схемы
        List<StoreSection> sections = dbHelper.getAllSections(mapId);

        // Проверяем количество разделов
        assertEquals(2, sections.size());

        // Проверяем имя первого раздела
        assertEquals("Section 1", sections.get(0).getName());
    }

    /**
     * Тест на добавление и получение товаров.
     */
    @Test
    public void testAddAndGetProducts() {
        // Создаем тестовую схему и раздел
        long mapId = dbHelper.saveStoreMap("Test Map", "/test/path/image.jpg");

        StoreSection section = new StoreSection();
        section.setName("Test Section");
        section.setX(100.0f);
        section.setY(100.0f);

        long sectionId = dbHelper.addSection(section, mapId);

        // Создаем и добавляем два тестовых товара
        Product product1 = new Product();
        product1.setName("Product 1");
        product1.setSectionId(sectionId);

        Product product2 = new Product();
        product2.setName("Product 2");
        product2.setSectionId(sectionId);

        long productId1 = dbHelper.addProduct(product1);
        long productId2 = dbHelper.addProduct(product2);

        // Проверяем, что идентификаторы не равны -1 (ошибка вставки)
        assertTrue(productId1 != -1);
        assertTrue(productId2 != -1);

        // Получаем список всех товаров для данного раздела
        List<Product> products = dbHelper.getProductsBySection(sectionId);

        // Проверяем количество товаров
        assertEquals(2, products.size());

        // Проверяем имя первого товара
        assertEquals("Product 1", products.get(0).getName());
    }

    /**
     * Тест на поиск товаров.
     */
    @Test
    public void testSearchProducts() {
        // Создаем тестовую схему и раздел
        long mapId = dbHelper.saveStoreMap("Test Map", "/test/path/image.jpg");

        StoreSection section = new StoreSection();
        section.setName("Test Section");
        section.setX(100.0f);
        section.setY(100.0f);

        long sectionId = dbHelper.addSection(section, mapId);

        // Создаем и добавляем тестовые товары с разными именами
        Product product1 = new Product();
        product1.setName("Apple");
        product1.setSectionId(sectionId);

        Product product2 = new Product();
        product2.setName("Banana");
        product2.setSectionId(sectionId);

        Product product3 = new Product();
        product3.setName("Apple Juice");
        product3.setSectionId(sectionId);

        dbHelper.addProduct(product1);
        dbHelper.addProduct(product2);
        dbHelper.addProduct(product3);

        // Ищем товары, содержащие "Apple"
        List<Product> searchResults = dbHelper.searchProducts("Apple");

        // Проверяем, что найдено 2 товара
        assertEquals(2, searchResults.size());

        // Проверяем имена найденных товаров
        boolean foundApple = false;
        boolean foundAppleJuice = false;

        for (Product product : searchResults) {
            if (product.getName().equals("Apple")) foundApple = true;
            if (product.getName().equals("Apple Juice")) foundAppleJuice = true;
        }

        assertTrue(foundApple);
        assertTrue(foundAppleJuice);
    }

    /**
     * Тест на импорт товаров из CSV-строки.
     */
    @Test
    public void testImportProductsFromCSV() {
        // Создаем тестовую схему и раздел
        long mapId = dbHelper.saveStoreMap("Test Map", "/test/path/image.jpg");

        StoreSection section = new StoreSection();
        section.setName("Fruits");
        section.setX(100.0f);
        section.setY(100.0f);

        long sectionId = dbHelper.addSection(section, mapId);

        // CSV данные в формате "название товара,раздел магазина"
        String csvData = "Apple,Fruits\nBanana,Fruits\nOrange,Fruits";

        // Импортируем товары
        int count = dbHelper.importProductsFromCSV(csvData, mapId);

        // Проверяем, что импортировано 3 товара
        assertEquals(3, count);

        // Получаем список всех товаров для данного раздела
        List<Product> products = dbHelper.getProductsBySection(sectionId);

        // Проверяем количество товаров
        assertEquals(3, products.size());

        // Проверяем имена товаров
        boolean foundApple = false;
        boolean foundBanana = false;
        boolean foundOrange = false;

        for (Product product : products) {
            if (product.getName().equals("Apple")) foundApple = true;
            if (product.getName().equals("Banana")) foundBanana = true;
            if (product.getName().equals("Orange")) foundOrange = true;
        }

        assertTrue(foundApple);
        assertTrue(foundBanana);
        assertTrue(foundOrange);
    }
}