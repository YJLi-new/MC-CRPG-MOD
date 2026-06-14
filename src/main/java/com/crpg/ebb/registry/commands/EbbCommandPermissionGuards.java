package com.crpg.ebb.registry.commands;

import com.crpg.ebb.EbbMod;
import net.fabricmc.fabric.api.permission.v1.PermissionPredicates;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.permissions.PermissionLevel;

import java.util.function.Predicate;

/**
 * Central permission surface for `/ebb` command groups.
 *
 * <p>Keeping the permission ids in one class makes accidental debug/state leaks easier
 * to audit while preserving player-facing self-inspection commands in {@code ModCommands}.</p>
 */
public final class EbbCommandPermissionGuards {
    private EbbCommandPermissionGuards() {
    }

    public static Predicate<CommandSourceStack> dev() {
        return group("command.dev");
    }

    public static Predicate<CommandSourceStack> dialogue() {
        return group("command.dialogue");
    }

    public static Predicate<CommandSourceStack> routine() {
        return group("command.routine");
    }

    public static Predicate<CommandSourceStack> export() {
        return group("command.export");
    }

    public static Predicate<CommandSourceStack> summonNpc() {
        return group("command.summon_npc");
    }

    public static Predicate<CommandSourceStack> attributeGrant() {
        return group("command.attributes.grant");
    }

    public static Predicate<CommandSourceStack> attributeSet() {
        return group("command.attributes.set");
    }

    public static Predicate<CommandSourceStack> attributeReset() {
        return group("command.attributes.reset");
    }

    private static Predicate<CommandSourceStack> group(String permissionId) {
        return PermissionPredicates.require(EbbMod.id(permissionId), PermissionLevel.GAMEMASTERS);
    }
}
