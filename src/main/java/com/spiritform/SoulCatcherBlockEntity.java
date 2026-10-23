package com.spiritform;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Пам'ятає спійманого моба, поки Ловець Душ стоїть блоком. */
public class SoulCatcherBlockEntity extends BlockEntity {

    private CompoundTag mob;

    public SoulCatcherBlockEntity(BlockPos pos, BlockState state) {
        super(SpiritBlocks.SOUL_CATCHER_BE.get(), pos, state);
    }

    public CompoundTag getMob() {
        return mob;
    }

    public void setMob(CompoundTag mob) {
        this.mob = mob == null ? null : mob.copy();
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (mob != null) {
            tag.put("Mob", mob);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        mob = tag.contains("Mob", 10) ? tag.getCompound("Mob") : null;
    }
}
