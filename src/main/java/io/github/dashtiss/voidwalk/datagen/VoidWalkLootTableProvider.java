package io.github.dashtiss.voidwalk.datagen;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.SimpleFabricLootTableProvider;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.Items;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.loot.entry.ItemEntry;
import net.minecraft.loot.function.SetComponentsLootFunction;
import net.minecraft.loot.function.SetCountLootFunction;
import net.minecraft.loot.function.EnchantWithLevelsLootFunction;
import net.minecraft.loot.provider.number.ConstantLootNumberProvider;
import net.minecraft.loot.provider.number.UniformLootNumberProvider;
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

        RegistryKey<LootTable> lootTableKey = RegistryKey.of(
                RegistryKeys.LOOT_TABLE,
                Identifier.of("voidwalk", "chests/supply_drop")
        );

        // ==========================================
        // POOL 1: ENDGAME PVP & COMBAT GEAR
        // ==========================================
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

        // ==========================================
        // POOL 2: RAW CURRENCY & ULTRA-RARE VALUABLES
        // ==========================================
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
                // Netherite Upgrade Smithing Templates (Crucial for 1.21 balancing)
                .with(ItemEntry.builder(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE).weight(10)
                        .apply(SetCountLootFunction.builder(UniformLootNumberProvider.create(1, 2)))
                )
                // Netherite Block (The jackpot!)
                .with(ItemEntry.builder(Items.NETHERITE_BLOCK).weight(2))

                // Enchanted Golden Apples (God Apples)
                .with(ItemEntry.builder(Items.ENCHANTED_GOLDEN_APPLE).weight(6)
                        .apply(SetCountLootFunction.builder(UniformLootNumberProvider.create(1, 2)))
                );

        // Combine both pools together into a single master loot table architecture
        biConsumer.accept(lootTableKey, LootTable.builder().pool(combatPool).pool(treasurePool));
    }
}