package com.spiritform;

import org.joml.Vector3f;

import net.minecraft.core.particles.DustParticleOptions;

/** Спільні частинки мода. М'ятний колір (R, G, B від 0 до 1) змінюється в одному місці. */
public final class SpiritParticles {

    public static final DustParticleOptions MINT = new DustParticleOptions(new Vector3f(0.55F, 1.0F, 0.78F), 1.1F);

    private SpiritParticles() {
    }
}
