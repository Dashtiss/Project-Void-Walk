package io.github.dashtiss.voidwalk.managers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerConfigEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.WorldSavePath;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class BountyManager {

    private static Map<UUID, Integer> ACTIVE_BOUNTIES = new HashMap<>();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /**
     * Gets the file location inside the current world folder
     */
    private static File getSaveFile(MinecraftServer server) {
        File worldDir = server.getSavePath(WorldSavePath.ROOT).toFile();
        return new File(worldDir, "voidwalk_bounties.json");
    }

    /**
     * SAVE FUNCTION: Call this during server shutdown
     */
    public static void saveBounties(MinecraftServer server) {
        File file = getSaveFile(server);

        // Convert our UUID map into a String map so JSON handles it perfectly without layout bugs
        Map<String, Integer> saveMap = new HashMap<>();
        ACTIVE_BOUNTIES.forEach((uuid, amount) -> saveMap.put(uuid.toString(), amount));

        try (FileWriter writer = new FileWriter(file)) {
            GSON.toJson(saveMap, writer);
            System.out.println("[VoidWalk] Successfully saved " + saveMap.size() + " active bounties to disk.");
        } catch (IOException e) {
            System.err.println("[VoidWalk] Failed to save active bounties!");
            e.printStackTrace();
        }
    }

    /**
     * LOAD FUNCTION: Call this during server initialization
     */
    public static void loadBounties(MinecraftServer server) {
        File file = getSaveFile(server);
        if (!file.exists()) return;

        try (FileReader reader = new FileReader(file)) {
            Type type = new TypeToken<Map<String, Integer>>(){}.getType();
            Map<String, Integer> loadMap = GSON.fromJson(reader, type);

            ACTIVE_BOUNTIES.clear();
            if (loadMap != null) {
                loadMap.forEach((uuidStr, amount) -> ACTIVE_BOUNTIES.put(UUID.fromString(uuidStr), amount));
            }
            System.out.println("[VoidWalk] Loaded " + ACTIVE_BOUNTIES.size() + " active bounties from save file.");
        } catch (IOException e) {
            System.err.println("[VoidWalk] Failed to load active bounties!");
            e.printStackTrace();
        }
    }

    public static void addBounty(ServerPlayerEntity sender, ServerPlayerEntity target, int amount) {
        if (amount <= 0) {
            sender.sendMessage(Text.literal("§cAmount must be greater than 0!"), false);
            return;
        }

        // FIXED: Use size() and getStack() on the inventory directly
        int count = 0;
        int invSize = sender.getInventory().size();
        for (int i = 0; i < invSize; i++) {
            ItemStack stack = sender.getInventory().getStack(i);
            if (stack.isOf(Items.DIAMOND)) {
                count += stack.getCount();
            }
        }

        if (count < amount) {
            sender.sendMessage(Text.literal("§cYou do not have " + amount + " diamonds to clear this bounty!"), false);
            return;
        }

        // FIXED: Deduct items using getStack and setStack public methods
        int remainingToDeduct = amount;
        for (int i = 0; i < invSize; i++) {
            ItemStack stack = sender.getInventory().getStack(i);
            if (stack.isOf(Items.DIAMOND)) {
                if (stack.getCount() <= remainingToDeduct) {
                    remainingToDeduct -= stack.getCount();
                    sender.getInventory().setStack(i, ItemStack.EMPTY);
                } else {
                    stack.decrement(remainingToDeduct);
                    break;
                }
            }
        }

        UUID targetUUID = target.getUuid();
        int currentBounty = ACTIVE_BOUNTIES.getOrDefault(targetUUID, 0);
        ACTIVE_BOUNTIES.put(targetUUID, currentBounty + amount);

        saveBounties(sender.getEntityWorld().getServer());

        sender.getEntityWorld().getServer().getPlayerManager().broadcast(
                Text.literal("§6§l[BOUNTY] §e" + sender.getName().getString() + " placed a bounty of §b"
                        + amount + " Diamonds §eon §c" + target.getName().getString() + "!"), false
        );
    }

    public static void handlePlayerDeath(ServerPlayerEntity victim, ServerPlayerEntity killer) {
        UUID victimUUID = victim.getUuid();
        ServerWorld world = victim.getEntityWorld();
        if (ACTIVE_BOUNTIES.containsKey(victimUUID)) {

            int prizeAmount = ACTIVE_BOUNTIES.remove(victimUUID);
            // System.out.println("Prize amount = " + prizeAmount);
            // Save immediately when a bounty is wiped out
            saveBounties(killer.getEntityWorld().getServer());

            victim.getEntityWorld().getServer().getPlayerManager().broadcast(
                    Text.literal("§4§l[BOUNTY CLAIMED] §c" + killer.getName().getString()
                            + " §ehas eliminated §4" + victim.getName().getString() + " §eand claimed §b"
                            + prizeAmount + " Diamonds!"), false
            );

            int remaining = prizeAmount;
            while (remaining > 0) {
                int stackSize = Math.min(remaining, 64);
                // System.out.println("Spawning stack of " + stackSize);
                ItemStack rewardStack = new ItemStack(Items.DIAMOND, stackSize);

                boolean inserted =
                        killer.getInventory().insertStack(rewardStack);

                if (!inserted) {
                    killer.dropItem(rewardStack, false);
                }

                remaining -= stackSize;
            }
        }
    }
    public static Map<UUID, Integer> getBountiesMap() {
        return ACTIVE_BOUNTIES;
    }
    public static Map<UUID, Integer> getActiveBounties() {
        return ACTIVE_BOUNTIES;
    }
    public static String getPlayerName(
            MinecraftServer server,
            UUID uuid
    ) {

        Object cacheObj =
                server.getApiServices()
                        .nameToIdCache();

        if (cacheObj instanceof net.minecraft.util.UserCache cache) {

            Optional<PlayerConfigEntry> profile =
                    cache.getByUuid(uuid);

            if (profile.isPresent()) {
                return profile.get().name();
            }
        }

        return uuid.toString();
    }
}