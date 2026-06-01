package com.crpg.ebb.api;

import net.minecraft.server.level.ServerPlayer;

public interface RollService {
    RollOutcome roll(ServerPlayer player, RollRule rule, RollContext context);
}
