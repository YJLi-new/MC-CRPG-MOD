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
import net.minecraft.resources.Identifier;

import java.util.Map;

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

    @GameTest(maxTicks = 20)
    public void roleTaggedEbbNpcsResolveDistinctConfiguredBindings(GameTestHelper helper) {
        assertLegacyRoleBinding(helper, "innkeeper", EbbMod.id("demo/innkeeper_intro"), new BlockPos(1, 2, 1));
        assertLegacyRoleBinding(helper, "witness", EbbMod.id("demo/witness_intro"), new BlockPos(2, 2, 1));
        assertLegacyRoleBinding(helper, "tenant", EbbMod.id("demo/tenant_intro"), new BlockPos(3, 2, 1));
        assertLegacyRoleBinding(helper, "guard", EbbMod.id("demo/guard_intro"), new BlockPos(4, 2, 1));
        helper.succeed();
    }

    @GameTest(maxTicks = 20)
    public void playableSliceBlockGroupsLoadAllGuiRetestTargets(GameTestHelper helper) {
        for (String id : Map.of(
                "locked door", "demo/locked_door",
                "counter ledger", "demo/counter_ledger",
                "notice board", "demo/notice_board",
                "washroom mirror", "demo/washroom_mirror",
                "windowsill ash", "demo/windowsill_ash",
                "tenant luggage", "demo/tenant_luggage",
                "cellar hatch", "demo/cellar_hatch",
                "back door", "demo/back_door"
        ).values()) {
            helper.assertTrue(BlockGroupIndex.byId(EbbMod.id(id)).isPresent(),
                    "GUI retest block group should load: " + id);
        }
        helper.assertTrue(BlockGroupIndex.groupCount() >= 8,
                "GUI retest expects all eight vertical-slice block groups, not only locked_door");
        helper.succeed();
    }

    private static void assertLegacyRoleBinding(GameTestHelper helper, String role, Identifier expectedDialogue, BlockPos pos) {
        EbbNpcEntity npc = helper.spawn(ModEntityTypes.NPC, pos);
        npc.addTag("ebb.npc");
        npc.addTag("ebb.npc." + role + "_day");
        var binding = EntityBindingRegistry.resolve(npc);
        helper.assertTrue(binding.isPresent(), "legacy role tag should resolve an entity binding: " + role);
        helper.assertTrue(expectedDialogue.equals(binding.get().dialogueId()),
                "legacy role tag should resolve " + role + " to " + expectedDialogue
                        + ", got " + binding.get().dialogueId());
    }
}
