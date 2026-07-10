package io.github.dashtiss.voidwalk.datagen;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.SimpleFabricLootTableProvider;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.item.Items;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.loot.entry.ItemEntry;
import net.minecraft.loot.function.SetComponentsLootFunction;
import net.minecraft.loot.function.SetCountLootFunction;
import net.minecraft.loot.function.EnchantWithLevelsLootFunction;
import net.minecraft.loot.function.SetPotionLootFunction;
import net.minecraft.loot.provider.number.ConstantLootNumberProvider;
import net.minecraft.loot.provider.number.UniformLootNumberProvider;
import net.minecraft.potion.Potions;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.function.BiConsumer;
import java.util.concurrent.CompletableFuture;

public class VoidWalkLootTableProvider extends SimpleFabricLootTableProvider {

    private final CompletableFuture<RegistryWrapper.WrapperLookup> localLookup;

    public VoidWalkLootTableProvider(FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registryLookup) {
        super(output, registryLookup, LootContextTypes.CHEST);
        this.localLookup = registryLookup;
    }

    @Override
    public void accept(BiConsumer<RegistryKey<LootTable>, LootTable.Builder> biConsumer) {
        RegistryWrapper.WrapperLookup registries = this.localLookup.join();

        // ================================================================
        // LOOT TABLE 1: SUPPLY DROP — High-tier PVP gear + resources
        // ================================================================
        RegistryKey<LootTable> supplyDropKey = RegistryKey.of(
                RegistryKeys.LOOT_TABLE,
                Identifier.of("voidwalk", "chests/supply_drop")
        );

        // POOL 1: ENDGAME PVP & COMBAT GEAR
        LootPool.Builder combatPool = LootPool.builder()
                .rolls(UniformLootNumberProvider.create(3, 5)) // 3 to 5 combat stacks

                // The Purge Blade (Netherite Sword)
                .with(ItemEntry.builder(Items.NETHERITE_SWORD).weight(5)
                        .apply(SetComponentsLootFunction.builder(DataComponentTypes.CUSTOM_NAME, Text.literal("§4§lThe Purge Blade")))
                        .apply(EnchantWithLevelsLootFunction.builder(registries, ConstantLootNumberProvider.create(30.0F)))
                )
                // The Annihilator (Mace)
                .with(ItemEntry.builder(Items.MACE).weight(3)
                        .apply(SetComponentsLootFunction.builder(DataComponentTypes.CUSTOM_NAME, Text.literal("§5§lAnnihilator")))
                        .apply(EnchantWithLevelsLootFunction.builder(registries, ConstantLootNumberProvider.create(30.0F)))
                )
                // God-Tier Netherite Chestplate
                .with(ItemEntry.builder(Items.NETHERITE_CHESTPLATE).weight(4)
                        .apply(SetComponentsLootFunction.builder(DataComponentTypes.CUSTOM_NAME, Text.literal("§b§lVoid Carapace")))
                        .apply(EnchantWithLevelsLootFunction.builder(registries, ConstantLootNumberProvider.create(30.0F)))
                )
                // --- NEW: Netherite Helmet ---
                .with(ItemEntry.builder(Items.NETHERITE_HELMET).weight(4)
                        .apply(SetComponentsLootFunction.builder(DataComponentTypes.CUSTOM_NAME, Text.literal("§b§lVoid Visor")))
                        .apply(EnchantWithLevelsLootFunction.builder(registries, ConstantLootNumberProvider.create(30.0F)))
                )
                // --- NEW: Netherite Leggings ---
                .with(ItemEntry.builder(Items.NETHERITE_LEGGINGS).weight(4)
                        .apply(SetComponentsLootFunction.builder(DataComponentTypes.CUSTOM_NAME, Text.literal("§b§lVoid Greaves")))
                        .apply(EnchantWithLevelsLootFunction.builder(registries, ConstantLootNumberProvider.create(30.0F)))
                )
                // --- NEW: Netherite Boots ---
                .with(ItemEntry.builder(Items.NETHERITE_BOOTS).weight(4)
                        .apply(SetComponentsLootFunction.builder(DataComponentTypes.CUSTOM_NAME, Text.literal("§b§lVoid Striders")))
                        .apply(EnchantWithLevelsLootFunction.builder(registries, ConstantLootNumberProvider.create(30.0F)))
                )
                // --- NEW: Shield ---
                .with(ItemEntry.builder(Items.SHIELD).weight(3)
                        .apply(SetComponentsLootFunction.builder(DataComponentTypes.CUSTOM_NAME, Text.literal("§6§lBulwark of the Void")))
                        .apply(EnchantWithLevelsLootFunction.builder(registries, ConstantLootNumberProvider.create(30.0F)))
                )
                // --- NEW: Bow ---
                .with(ItemEntry.builder(Items.BOW).weight(4)
                        .apply(SetComponentsLootFunction.builder(DataComponentTypes.CUSTOM_NAME, Text.literal("§a§lVoid Bow")))
                        .apply(EnchantWithLevelsLootFunction.builder(registries, ConstantLootNumberProvider.create(30.0F)))
                )
                // --- NEW: Crossbow ---
                .with(ItemEntry.builder(Items.CROSSBOW).weight(3)
                        .apply(SetComponentsLootFunction.builder(DataComponentTypes.CUSTOM_NAME, Text.literal("§c§lPurge Crossbow")))
                        .apply(EnchantWithLevelsLootFunction.builder(registries, ConstantLootNumberProvider.create(30.0F)))
                )
                // --- NEW: Trident ---
                .with(ItemEntry.builder(Items.TRIDENT).weight(2)
                        .apply(SetComponentsLootFunction.builder(DataComponentTypes.CUSTOM_NAME, Text.literal("§b§lPoseidon's Wrath")))
                        .apply(EnchantWithLevelsLootFunction.builder(registries, ConstantLootNumberProvider.create(30.0F)))
                )
                // Totems
                .with(ItemEntry.builder(Items.TOTEM_OF_UNDYING).weight(12))
                // End Crystals
                .with(ItemEntry.builder(Items.END_CRYSTAL).weight(15)
                        .apply(SetCountLootFunction.builder(UniformLootNumberProvider.create(4, 12)))
                )
                // Obsidian
                .with(ItemEntry.builder(Items.OBSIDIAN).weight(15)
                        .apply(SetCountLootFunction.builder(UniformLootNumberProvider.create(16, 32)))
                )
                // Respawn Anchors & Glowstone
                .with(ItemEntry.builder(Items.RESPAWN_ANCHOR).weight(8)
                        .apply(SetCountLootFunction.builder(UniformLootNumberProvider.create(2, 4)))
                )
                .with(ItemEntry.builder(Items.GLOWSTONE).weight(12)
                        .apply(SetCountLootFunction.builder(UniformLootNumberProvider.create(16, 48)))
                );

        // POOL 2: RAW CURRENCY & ULTRA-RARE VALUABLES
        LootPool.Builder treasurePool = LootPool.builder()
                .rolls(UniformLootNumberProvider.create(2, 4)) // 2 to 4 luxury reward stacks

                // Raw Diamonds (Bundles of 4 to 12)
                .with(ItemEntry.builder(Items.DIAMOND).weight(25)
                        .apply(SetCountLootFunction.builder(UniformLootNumberProvider.create(4, 12)))
                )
                // Diamond Blocks (Rare structural reward)
                .with(ItemEntry.builder(Items.DIAMOND_BLOCK).weight(8)
                        .apply(SetCountLootFunction.builder(UniformLootNumberProvider.create(1, 3)))
                )
                // Netherite Ingots
                .with(ItemEntry.builder(Items.NETHERITE_INGOT).weight(12)
                        .apply(SetCountLootFunction.builder(UniformLootNumberProvider.create(1, 4)))
                )
                // Netherite Upgrade Smithing Templates
                .with(ItemEntry.builder(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE).weight(10)
                        .apply(SetCountLootFunction.builder(UniformLootNumberProvider.create(1, 2)))
                )
                // Netherite Block (The jackpot!)
                .with(ItemEntry.builder(Items.NETHERITE_BLOCK).weight(2))

                // Enchanted Golden Apples (God Apples)
                .with(ItemEntry.builder(Items.ENCHANTED_GOLDEN_APPLE).weight(6)
                        .apply(SetCountLootFunction.builder(UniformLootNumberProvider.create(1, 2)))
                );

        // POOL 3: CONSUMABLES & UTILITY (NEW!)
        LootPool.Builder consumablePool = LootPool.builder()
                .rolls(UniformLootNumberProvider.create(2, 4)) // 2 to 4 consumable stacks

                // --- Potions: Healing II ---
                .with(ItemEntry.builder(Items.POTION).weight(8)
                        .apply(SetPotionLootFunction.builder(Potions.HEALING))
                        .apply(SetCountLootFunction.builder(UniformLootNumberProvider.create(1, 3)))
                )
                .with(ItemEntry.builder(Items.SPLASH_POTION).weight(6)
                        .apply(SetPotionLootFunction.builder(Potions.HEALING))
                        .apply(SetCountLootFunction.builder(UniformLootNumberProvider.create(1, 2)))
                )
                .with(ItemEntry.builder(Items.LINGERING_POTION).weight(3)
                        .apply(SetPotionLootFunction.builder(Potions.HEALING))
                        .apply(SetCountLootFunction.builder(UniformLootNumberProvider.create(1, 2)))
                )
                // --- Potions: Strength II ---
                .with(ItemEntry.builder(Items.POTION).weight(6)
                        .apply(SetPotionLootFunction.builder(Potions.STRENGTH))
                        .apply(SetCountLootFunction.builder(UniformLootNumberProvider.create(1, 2)))
                )
                .with(ItemEntry.builder(Items.SPLASH_POTION).weight(4)
                        .apply(SetPotionLootFunction.builder(Potions.STRENGTH))
                )
                // --- Potions: Speed II ---
                .with(ItemEntry.builder(Items.POTION).weight(6)
                        .apply(SetPotionLootFunction.builder(Potions.SWIFTNESS))
                        .apply(SetCountLootFunction.builder(UniformLootNumberProvider.create(1, 2)))
                )
                // --- Potions: Fire Resistance ---
                .with(ItemEntry.builder(Items.POTION).weight(8)
                        .apply(SetPotionLootFunction.builder(Potions.FIRE_RESISTANCE))
                        .apply(SetCountLootFunction.builder(UniformLootNumberProvider.create(1, 3)))
                )
                // --- Potions: Water Breathing ---
                .with(ItemEntry.builder(Items.POTION).weight(5)
                        .apply(SetPotionLootFunction.builder(Potions.WATER_BREATHING))
                        .apply(SetCountLootFunction.builder(UniformLootNumberProvider.create(1, 2)))
                )
                // --- Arrows: Spectral ---
                .with(ItemEntry.builder(Items.SPECTRAL_ARROW).weight(10)
                        .apply(SetCountLootFunction.builder(UniformLootNumberProvider.create(8, 24)))
                )
                // --- Arrows: Tipped (Harming II) ---
                .with(ItemEntry.builder(Items.TIPPED_ARROW).weight(8)
                        .apply(SetPotionLootFunction.builder(Potions.HARMING))
                        .apply(SetCountLootFunction.builder(UniformLootNumberProvider.create(8, 16)))
                )
                // --- Arrows: Tipped (Healing II) ---
                .with(ItemEntry.builder(Items.TIPPED_ARROW).weight(6)
                        .apply(SetPotionLootFunction.builder(Potions.HEALING))
                        .apply(SetCountLootFunction.builder(UniformLootNumberProvider.create(8, 16)))
                )
                // --- Tools: Netherite Pickaxe ---
                .with(ItemEntry.builder(Items.NETHERITE_PICKAXE).weight(3)
                        .apply(SetComponentsLootFunction.builder(DataComponentTypes.CUSTOM_NAME, Text.literal("§d§lVoid Excavator")))
                        .apply(EnchantWithLevelsLootFunction.builder(registries, ConstantLootNumberProvider.create(30.0F)))
                )
                // --- Tools: Netherite Axe ---
                .with(ItemEntry.builder(Items.NETHERITE_AXE).weight(3)
                        .apply(SetComponentsLootFunction.builder(DataComponentTypes.CUSTOM_NAME, Text.literal("§d§lVoid Cleaver")))
                        .apply(EnchantWithLevelsLootFunction.builder(registries, ConstantLootNumberProvider.create(30.0F)))
                )
                // --- Tools: Netherite Shovel ---
                .with(ItemEntry.builder(Items.NETHERITE_SHOVEL).weight(3)
                        .apply(SetComponentsLootFunction.builder(DataComponentTypes.CUSTOM_NAME, Text.literal("§d§lVoid Spade")))
                        .apply(EnchantWithLevelsLootFunction.builder(registries, ConstantLootNumberProvider.create(30.0F)))
                )
                // --- Utility: Ender Pearls ---
                .with(ItemEntry.builder(Items.ENDER_PEARL).weight(12)
                        .apply(SetCountLootFunction.builder(UniformLootNumberProvider.create(4, 12)))
                )
                // --- Utility: Golden Apples ---
                .with(ItemEntry.builder(Items.GOLDEN_APPLE).weight(10)
                        .apply(SetCountLootFunction.builder(UniformLootNumberProvider.create(2, 6)))
                )
                // --- Rare: Elytra ---
                .with(ItemEntry.builder(Items.ELYTRA).weight(1)
                        .apply(SetComponentsLootFunction.builder(DataComponentTypes.CUSTOM_NAME, Text.literal("§5§lWings of the Void")))
                )
                // --- Rare: Shulker Box ---
                .with(ItemEntry.builder(Items.SHULKER_BOX).weight(3)
                        .apply(SetCountLootFunction.builder(UniformLootNumberProvider.create(1, 2)))
                )
                // --- Rare: Shulker Shells ---
                .with(ItemEntry.builder(Items.SHULKER_SHELL).weight(4)
                        .apply(SetCountLootFunction.builder(UniformLootNumberProvider.create(2, 4)))
                )
                // --- Trinkets: Echo Shards ---
                .with(ItemEntry.builder(Items.ECHO_SHARD).weight(6)
                        .apply(SetCountLootFunction.builder(UniformLootNumberProvider.create(2, 6)))
                );

        // Build supply drop loot table with all 3 pools
        biConsumer.accept(supplyDropKey, LootTable.builder()
                .pool(combatPool)
                .pool(treasurePool)
                .pool(consumablePool)
        );

        // ================================================================
        // LOOT TABLE 2: RANDOM CRATE — More varied, slightly lower tier
        // ================================================================
        RegistryKey<LootTable> randomCrateKey = RegistryKey.of(
                RegistryKeys.LOOT_TABLE,
                Identifier.of("voidwalk", "chests/random_crate")
        );

        // POOL 1: MID-TIER COMBAT GEAR
        LootPool.Builder crateCombatPool = LootPool.builder()
                .rolls(UniformLootNumberProvider.create(2, 4))

                .with(ItemEntry.builder(Items.DIAMOND_SWORD).weight(8)
                        .apply(EnchantWithLevelsLootFunction.builder(registries, ConstantLootNumberProvider.create(20.0F)))
                )
                .with(ItemEntry.builder(Items.DIAMOND_CHESTPLATE).weight(6)
                        .apply(EnchantWithLevelsLootFunction.builder(registries, ConstantLootNumberProvider.create(20.0F)))
                )
                .with(ItemEntry.builder(Items.DIAMOND_HELMET).weight(6)
                        .apply(EnchantWithLevelsLootFunction.builder(registries, ConstantLootNumberProvider.create(20.0F)))
                )
                .with(ItemEntry.builder(Items.DIAMOND_LEGGINGS).weight(6)
                        .apply(EnchantWithLevelsLootFunction.builder(registries, ConstantLootNumberProvider.create(20.0F)))
                )
                .with(ItemEntry.builder(Items.DIAMOND_BOOTS).weight(6)
                        .apply(EnchantWithLevelsLootFunction.builder(registries, ConstantLootNumberProvider.create(20.0F)))
                )
                .with(ItemEntry.builder(Items.BOW).weight(7)
                        .apply(EnchantWithLevelsLootFunction.builder(registries, ConstantLootNumberProvider.create(20.0F)))
                )
                .with(ItemEntry.builder(Items.CROSSBOW).weight(5)
                        .apply(EnchantWithLevelsLootFunction.builder(registries, ConstantLootNumberProvider.create(20.0F)))
                )
                .with(ItemEntry.builder(Items.TRIDENT).weight(2)
                        .apply(EnchantWithLevelsLootFunction.builder(registries, ConstantLootNumberProvider.create(20.0F)))
                )
                .with(ItemEntry.builder(Items.SHIELD).weight(5)
                        .apply(EnchantWithLevelsLootFunction.builder(registries, ConstantLootNumberProvider.create(20.0F)))
                )
                .with(ItemEntry.builder(Items.TOTEM_OF_UNDYING).weight(6))
                .with(ItemEntry.builder(Items.END_CRYSTAL).weight(8)
                        .apply(SetCountLootFunction.builder(UniformLootNumberProvider.create(2, 6)))
                )
                .with(ItemEntry.builder(Items.OBSIDIAN).weight(10)
                        .apply(SetCountLootFunction.builder(UniformLootNumberProvider.create(8, 16)))
                );

        // POOL 2: SUPPLIES & TREASURE
        LootPool.Builder crateTreasurePool = LootPool.builder()
                .rolls(UniformLootNumberProvider.create(2, 3))

                .with(ItemEntry.builder(Items.DIAMOND).weight(30)
                        .apply(SetCountLootFunction.builder(UniformLootNumberProvider.create(2, 8)))
                )
                .with(ItemEntry.builder(Items.IRON_INGOT).weight(20)
                        .apply(SetCountLootFunction.builder(UniformLootNumberProvider.create(8, 16)))
                )
                .with(ItemEntry.builder(Items.GOLD_INGOT).weight(15)
                        .apply(SetCountLootFunction.builder(UniformLootNumberProvider.create(8, 16)))
                )
                .with(ItemEntry.builder(Items.NETHERITE_SCRAP).weight(8)
                        .apply(SetCountLootFunction.builder(UniformLootNumberProvider.create(1, 3)))
                )
                .with(ItemEntry.builder(Items.ENCHANTED_GOLDEN_APPLE).weight(3)
                        .apply(SetCountLootFunction.builder(UniformLootNumberProvider.create(1, 2)))
                )
                .with(ItemEntry.builder(Items.GOLDEN_APPLE).weight(12)
                        .apply(SetCountLootFunction.builder(UniformLootNumberProvider.create(2, 4)))
                )
                .with(ItemEntry.builder(Items.ENDER_PEARL).weight(10)
                        .apply(SetCountLootFunction.builder(UniformLootNumberProvider.create(2, 6)))
                )
                .with(ItemEntry.builder(Items.EXPERIENCE_BOTTLE).weight(15)
                        .apply(SetCountLootFunction.builder(UniformLootNumberProvider.create(8, 24)))
                );

        // POOL 3: CONSUMABLES & UTILITY
        LootPool.Builder crateConsumablePool = LootPool.builder()
                .rolls(UniformLootNumberProvider.create(1, 3))

                .with(ItemEntry.builder(Items.POTION).weight(8)
                        .apply(SetPotionLootFunction.builder(Potions.HEALING))
                        .apply(SetCountLootFunction.builder(UniformLootNumberProvider.create(1, 2)))
                )
                .with(ItemEntry.builder(Items.SPLASH_POTION).weight(6)
                        .apply(SetPotionLootFunction.builder(Potions.HEALING))
                )
                .with(ItemEntry.builder(Items.POTION).weight(6)
                        .apply(SetPotionLootFunction.builder(Potions.SWIFTNESS))
                )
                .with(ItemEntry.builder(Items.POTION).weight(6)
                        .apply(SetPotionLootFunction.builder(Potions.STRENGTH))
                )
                .with(ItemEntry.builder(Items.POTION).weight(7)
                        .apply(SetPotionLootFunction.builder(Potions.FIRE_RESISTANCE))
                        .apply(SetCountLootFunction.builder(UniformLootNumberProvider.create(1, 2)))
                )
                .with(ItemEntry.builder(Items.SPECTRAL_ARROW).weight(8)
                        .apply(SetCountLootFunction.builder(UniformLootNumberProvider.create(8, 16)))
                )
                .with(ItemEntry.builder(Items.TIPPED_ARROW).weight(6)
                        .apply(SetPotionLootFunction.builder(Potions.HARMING))
                        .apply(SetCountLootFunction.builder(UniformLootNumberProvider.create(4, 12)))
                )
                .with(ItemEntry.builder(Items.DIAMOND_PICKAXE).weight(5)
                        .apply(EnchantWithLevelsLootFunction.builder(registries, ConstantLootNumberProvider.create(20.0F)))
                )
                .with(ItemEntry.builder(Items.DIAMOND_AXE).weight(5)
                        .apply(EnchantWithLevelsLootFunction.builder(registries, ConstantLootNumberProvider.create(20.0F)))
                )
                .with(ItemEntry.builder(Items.SHULKER_SHELL).weight(3)
                        .apply(SetCountLootFunction.builder(UniformLootNumberProvider.create(1, 2)))
                )
                .with(ItemEntry.builder(Items.ECHO_SHARD).weight(5)
                        .apply(SetCountLootFunction.builder(UniformLootNumberProvider.create(1, 4)))
                );

        biConsumer.accept(randomCrateKey, LootTable.builder()
                .pool(crateCombatPool)
                .pool(crateTreasurePool)
                .pool(crateConsumablePool)
        );
    }
}
