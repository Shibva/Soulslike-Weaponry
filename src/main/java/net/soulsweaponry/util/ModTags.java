package net.soulsweaponry.util;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.tag.TagKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.gen.feature.ConfiguredStructureFeature;
import net.soulsweaponry.SoulsWeaponry;

@SuppressWarnings("unused")
public class ModTags {

    public static class Blocks {

        private static TagKey<Block> createTag(String name) {
            return TagKey.of(Registry.BLOCK_KEY, new Identifier(SoulsWeaponry.ModId, name));
        }

        private static TagKey<Block> createCommonTag(String name) {
            return TagKey.of(Registry.BLOCK_KEY, new Identifier("c", name));
        }
    }

    public static class Items {
        public static final TagKey<Item> LORD_SOUL = createCommonTag("lord_soul");
        public static final TagKey<Item> DEMON_HEARTS = createCommonTag("demon_hearts");
        public static final TagKey<Item> MOONLIGHT_SWORD = createTag("moonlight_sword");

        private static TagKey<Item> createTag(String name) {
            return TagKey.of(Registry.ITEM_KEY, new Identifier(SoulsWeaponry.ModId, name));
        }

        private static TagKey<Item> createCommonTag(String name) {
            return TagKey.of(Registry.ITEM_KEY, new Identifier("c", name));
        }
    }

    public static class Structures {
        public static final TagKey<ConfiguredStructureFeature<?, ?>> DECAYING_KINGDOM = createTag("decaying_kingdom");
        public static final TagKey<ConfiguredStructureFeature<?, ?>> CHAMPIONS_GRAVES = createTag("champions_graves");

        private static TagKey<ConfiguredStructureFeature<?, ?>> createTag(String id) {
            return TagKey.of(Registry.CONFIGURED_STRUCTURE_FEATURE_KEY, new Identifier(SoulsWeaponry.ModId, id));
        }
    }
}
