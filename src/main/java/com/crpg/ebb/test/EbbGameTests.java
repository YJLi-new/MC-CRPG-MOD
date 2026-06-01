package com.crpg.ebb.test;

import com.crpg.ebb.EbbMod;
import com.crpg.ebb.dialogue.DialogueRegistry;
import com.crpg.ebb.interaction.BlockGroupIndex;
import com.crpg.ebb.interaction.entity.EntityBindingRegistry;
import com.crpg.ebb.npc.EbbNpcEntity;
import com.crpg.ebb.npc.ModEntityTypes;
import com.crpg.ebb.routine.NpcRoutineRegistry;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Minimal Fabric GameTest coverage for the report's in-world regression-test requirement.
 * These tests use Fabric's bundled empty structure and are run by a Fabric gametest server
 * when launched with the `fabric-api.gametest` system property.
 */
public final class EbbGameTests {
    public EbbGameTests() {
    }

    @GameTest(maxTicks = 20)
    public void bundledDataRegistriesAreValid(GameTestHelper helper) {
        helper.assertTrue(DialogueRegistry.byId(EbbMod.id("demo/innkeeper_intro")).isPresent(),
                "bundled innkeeper dialogue should load");
        helper.assertTrue(DialogueRegistry.validationMessages().isEmpty(),
                "bundled dialogues should have no validation messages");
        helper.assertTrue(BlockGroupIndex.byId(EbbMod.id("demo/locked_door")).isPresent(),
                "bundled locked-door block group should load");
        helper.assertTrue(BlockGroupIndex.messages().isEmpty(),
                "bundled block groups should have no index messages");
        helper.assertTrue(EntityBindingRegistry.size() >= 2,
                "bundled entity bindings should load");
        helper.assertTrue(EntityBindingRegistry.validationMessages().isEmpty(),
                "bundled entity bindings should have no validation messages");
        helper.assertTrue(NpcRoutineRegistry.byId(EbbMod.id("demo/innkeeper_day")).isPresent(),
                "bundled innkeeper routine should load");
        helper.assertTrue(NpcRoutineRegistry.validationMessages().isEmpty(),
                "bundled NPC routines should have no validation messages");
        helper.succeed();
    }

    @GameTest(maxTicks = 20)
    public void ebbNpcSpawnsWithRoutineState(GameTestHelper helper) {
        EbbNpcEntity npc = helper.spawn(ModEntityTypes.NPC, new BlockPos(1, 2, 1));
        npc.setRoutineId(EbbMod.id("demo/innkeeper_day"));
        helper.assertTrue(npc.routineId().isPresent(), "spawned Ebb NPC should store a routine id");
        helper.assertTrue(npc.entityTags().contains("ebb.npc") || npc.getType() == ModEntityTypes.NPC,
                "spawned test NPC should be the registered Ebb NPC type");
        helper.succeed();
    }

    @GameTest(maxTicks = 20)
    public void taggedEbbNpcResolvesConfiguredBinding(GameTestHelper helper) {
        EbbNpcEntity npc = helper.spawn(ModEntityTypes.NPC, new BlockPos(1, 2, 1));
        npc.addTag("ebb.npc");
        var binding = EntityBindingRegistry.resolve(npc);
        helper.assertTrue(binding.isPresent(), "tagged Ebb NPC should resolve an entity binding");
        helper.assertTrue(EbbMod.id("demo/innkeeper_intro").equals(binding.get().dialogueId()),
                "tagged Ebb NPC should resolve to the innkeeper dialogue");
        helper.succeed();
    }
}
