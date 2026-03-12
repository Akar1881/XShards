package com.xshards;

import org.bukkit.inventory.ItemStack;
import java.util.List;

public class ShopItem {
    private final ItemStack item;
    private final double price;
    private String customDisplayName;
    private List<String> customLore;

    public ShopItem(ItemStack item, double price) {
        this.item = item;
        this.price = price;
        this.customDisplayName = null;
        this.customLore = null;
    }

    public ShopItem(ItemStack item, double price, String customDisplayName, List<String> customLore) {
        this.item = item;
        this.price = price;
        this.customDisplayName = customDisplayName;
        this.customLore = customLore;
    }

    public ItemStack getItem() {
        return item;
    }

    public double getPrice() {
        return price;
    }

    public String getCustomDisplayName() {
        return customDisplayName;
    }

    public void setCustomDisplayName(String customDisplayName) {
        this.customDisplayName = customDisplayName;
    }

    public List<String> getCustomLore() {
        return customLore;
    }

    public void setCustomLore(List<String> customLore) {
        this.customLore = customLore;
    }
}