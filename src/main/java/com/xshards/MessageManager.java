package com.xshards;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.io.IOException;

public class MessageManager {
    private final Xshards plugin;
    private FileConfiguration messagesConfig;
    private File messagesFile;

    public MessageManager(Xshards plugin) {
        this.plugin = plugin;
        loadMessages();
    }

    public void loadMessages() {
        messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
    }

    public String get(String key) {
        String message = messagesConfig.getString(key, "");
        // Convert & color codes
        return message.replace("&", "§");
    }

    public void reload() {
        loadMessages();
    }
}
