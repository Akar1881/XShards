package com.xshards;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TransferListener implements Listener {
    private final ShardManager shardManager;
    private final MessageManager messageManager;
    private final Map<UUID, TransferData> pendingTransfers = new HashMap<>();

    public TransferListener(ShardManager shardManager, MessageManager messageManager) {
        this.shardManager = shardManager;
        this.messageManager = messageManager;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals("Confirm Transfer")) {
            return;
        }

        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        TransferData transfer = pendingTransfers.get(player.getUniqueId());

        if (transfer == null || event.getCurrentItem() == null) {
            return;
        }

        if (event.getCurrentItem().getType() == Material.GREEN_WOOL) {
            // Confirm transfer
            Player recipient = Bukkit.getPlayer(transfer.getRecipientUUID());
            if (recipient != null) {
                shardManager.addShards(player, -transfer.getAmount());
                shardManager.addShards(recipient, transfer.getAmount());

                player.sendMessage(messageManager.get("transfer.success-sender").replace("{player}", recipient.getName()).replace("{amount}", String.valueOf(transfer.getAmount())));
                recipient.sendMessage(messageManager.get("transfer.success-recipient").replace("{player}", player.getName()).replace("{amount}", String.valueOf(transfer.getAmount())));
            }
        } else if (event.getCurrentItem().getType() == Material.RED_WOOL) {
            player.sendMessage(messageManager.get("transfer.cancelled"));
        }

        pendingTransfers.remove(player.getUniqueId());
        player.closeInventory();
    }

    public void addPendingTransfer(Player player, TransferData transfer) {
        pendingTransfers.put(player.getUniqueId(), transfer);
    }

    public static class TransferData {
        private final UUID recipientUUID;
        private final int amount;

        public TransferData(UUID recipientUUID, int amount) {
            this.recipientUUID = recipientUUID;
            this.amount = amount;
        }

        public UUID getRecipientUUID() {
            return recipientUUID;
        }

        public int getAmount() {
            return amount;
        }
    }
}
