package com.crpg.ebb.api;

import net.minecraft.world.phys.Vec3;

public record HitContext(Vec3 eye, Vec3 hit, double distance) {
}
