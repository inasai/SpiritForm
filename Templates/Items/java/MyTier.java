package com.spiritform;

import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;

/** Матеріал зброї та інструментів (Tier). Усі числа змінюються тут. */
public enum MyTier implements Tier {
    //  міцність, швидкість копання, бонус шкоди, що НЕ можна добути, зачаровуваність
    MY_TIER(1800, 8.0F, 3.0F, BlockTags.INCORRECT_FOR_DIAMOND_TOOL, 15);

    private final int uses;
    private final float speed;
    private final float damage;
    private final TagKey<Block> incorrectBlocks;
    private final int enchantability;

    MyTier(int uses, float speed, float damage, TagKey<Block> incorrectBlocks, int enchantability) {
        this.uses = uses;
        this.speed = speed;
        this.damage = damage;
        this.incorrectBlocks = incorrectBlocks;
        this.enchantability = enchantability;
    }

    @Override
    public int getUses() {
        return uses;
    }

    @Override
    public float getSpeed() {
        return speed;
    }

    @Override
    public float getAttackDamageBonus() {
        return damage;
    }

    @Override
    public TagKey<Block> getIncorrectBlocksForDrops() {
        return incorrectBlocks;
    }

    @Override
    public int getEnchantmentValue() {
        return enchantability;
    }

    /** Чим лагодиться в ковадлі. Приклад: Ingredient.of(Items.IRON_INGOT). */
    @Override
    public Ingredient getRepairIngredient() {
        return Ingredient.EMPTY;
    }
}
