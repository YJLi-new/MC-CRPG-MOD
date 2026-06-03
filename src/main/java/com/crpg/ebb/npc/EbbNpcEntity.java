package com.crpg.ebb.npc;

import com.crpg.ebb.EbbMod;
import com.crpg.ebb.routine.NpcRoutineController;
import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.Optional;

public class EbbNpcEntity extends PathfinderMob implements GeoEntity {
    private static final RawAnimation IDLE_ANIMATION = RawAnimation.begin().thenLoop("misc.idle");
    private static final RawAnimation WALK_ANIMATION = RawAnimation.begin().thenLoop("move.walk");
    private static final RawAnimation FIDGET_ANIMATION = RawAnimation.begin().thenLoop("misc.fidget");
    private static final RawAnimation TALK_ANIMATION = RawAnimation.begin().thenLoop("dialogue.talk");
    private static final RawAnimation THINK_ANIMATION = RawAnimation.begin().thenLoop("dialogue.think");
    private static final RawAnimation DISMISS_ANIMATION = RawAnimation.begin().thenPlayAndHold("dialogue.dismiss");
    private static final RawAnimation NERVOUS_IDLE_ANIMATION = RawAnimation.begin().thenLoop("dialogue.nervous_idle");
    private static final String ROUTINE_TAG = "EbbRoutine";
    private static final String NARRATIVE_KEY_TAG = "EbbNarrativeKey";
    private static final String POSE_TAG = "EbbPose";
    private static final String ANIMATION_TAG = "EbbAnimation";
    private static final String VISUAL_ROLE_TAG = "EbbVisualRole";
    private final AnimatableInstanceCache geckoCache = GeckoLibUtil.createInstanceCache(this);
    private Optional<Identifier> routineId = Optional.of(EbbMod.id("demo/innkeeper_day"));
    private String narrativeStateKey = "ebb:demo/innkeeper";
    private String narrativePose = "standing";
    private String narrativeAnimation = "idle";
    private String visualRole = "innkeeper";
    private String routinePathKey = "";
    private int routinePathIndex;
    private String routineDebugAction = "idle";
    private String routineDebugTarget = "-";
    private String routineDebugStep = "-";
    private boolean conversationFocused;
    private String preConversationPose = "standing";
    private String preConversationAnimation = "idle";

    public EbbNpcEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.24D)
                .add(Attributes.FOLLOW_RANGE, 16.0D);
    }

    public Optional<Identifier> routineId() {
        return routineId;
    }

    public void setRoutineId(Identifier routineId) {
        if (!this.routineId.equals(Optional.of(routineId))) {
            resetRoutinePath();
        }
        this.routineId = Optional.of(routineId);
        setVisualRole(roleFromRoutine(routineId));
    }

    public String narrativeStateKey() {
        return narrativeStateKey;
    }

    public void setNarrativeStateKey(String narrativeStateKey) {
        if (narrativeStateKey != null && !narrativeStateKey.isBlank()) {
            this.narrativeStateKey = narrativeStateKey.trim().toLowerCase(java.util.Locale.ROOT);
            setVisualRole(roleFromNarrativeKey(this.narrativeStateKey));
        }
    }

    public String narrativePose() {
        return narrativePose;
    }

    public void setNarrativePose(String narrativePose) {
        if (narrativePose != null && !narrativePose.isBlank()) {
            this.narrativePose = narrativePose.trim().toLowerCase(java.util.Locale.ROOT);
        }
    }

    public String narrativeAnimation() {
        return narrativeAnimation;
    }

    public void setNarrativeAnimation(String narrativeAnimation) {
        if (narrativeAnimation != null && !narrativeAnimation.isBlank()) {
            this.narrativeAnimation = narrativeAnimation.trim().toLowerCase(java.util.Locale.ROOT);
        }
    }

    public String visualRole() {
        return visualRole;
    }

    public void setVisualRole(String visualRole) {
        if (visualRole != null && !visualRole.isBlank()) {
            this.visualRole = sanitizeRole(visualRole);
        }
    }

    public void beginConversationFocus(String animation) {
        if (!conversationFocused) {
            preConversationPose = narrativePose;
            preConversationAnimation = narrativeAnimation;
        }
        conversationFocused = true;
        setNarrativePose("conversation");
        setNarrativeAnimation(animation == null || animation.isBlank() ? "talk" : animation);
    }

    public void endConversationFocus() {
        if (!conversationFocused) {
            return;
        }
        conversationFocused = false;
        setNarrativePose(preConversationPose);
        setNarrativeAnimation(preConversationAnimation);
    }

    public boolean conversationFocused() {
        return conversationFocused;
    }

    public void setRoutineDebug(String action, String target, String step) {
        routineDebugAction = normalizeDebug(action, "idle");
        routineDebugTarget = target == null || target.isBlank() ? "-" : target;
        routineDebugStep = step == null || step.isBlank() ? "-" : step;
    }

    public String routineDebugSummary() {
        return "routine_debug(action=" + routineDebugAction
                + ", target=" + routineDebugTarget
                + ", step=" + routineDebugStep
                + ", focused=" + conversationFocused + ")";
    }

    public int routinePathIndex(String key, int pathSize) {
        if (!routinePathKey.equals(key)) {
            routinePathKey = key;
            routinePathIndex = 0;
        }
        if (pathSize <= 0) {
            return 0;
        }
        routinePathIndex = Math.floorMod(routinePathIndex, pathSize);
        return routinePathIndex;
    }

    public void advanceRoutinePath(String key, int pathSize) {
        if (pathSize <= 0) {
            return;
        }
        if (!routinePathKey.equals(key)) {
            routinePathKey = key;
            routinePathIndex = 0;
            return;
        }
        routinePathIndex = Math.floorMod(routinePathIndex + 1, pathSize);
    }

    public String routinePathKey() {
        return routinePathKey;
    }

    public int routinePathIndex() {
        return routinePathIndex;
    }

    private void resetRoutinePath() {
        routinePathKey = "";
        routinePathIndex = 0;
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        NpcRoutineController.tick(this, level);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        routineId.ifPresent(id -> output.putString(ROUTINE_TAG, id.toString()));
        output.putString(NARRATIVE_KEY_TAG, narrativeStateKey);
        output.putString(POSE_TAG, narrativePose);
        output.putString(ANIMATION_TAG, narrativeAnimation);
        output.putString(VISUAL_ROLE_TAG, visualRole);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        routineId = input.getString(ROUTINE_TAG).map(Identifier::parse).or(() -> Optional.of(EbbMod.id("demo/innkeeper_day")));
        narrativeStateKey = input.getString(NARRATIVE_KEY_TAG).orElse("ebb:demo/innkeeper");
        narrativePose = input.getString(POSE_TAG).orElse("standing");
        narrativeAnimation = input.getString(ANIMATION_TAG).orElse("idle");
        visualRole = input.getString(VISUAL_ROLE_TAG).map(EbbNpcEntity::sanitizeRole).orElse(roleFromNarrativeKey(narrativeStateKey));
        resetRoutinePath();
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<EbbNpcEntity>("ebb_npc_main", 5, test ->
                test.setAndContinue(animationFor(test.animatable().narrativeAnimation(), test.isMoving()))));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geckoCache;
    }

    private static String roleFromRoutine(Identifier routineId) {
        String path = routineId.getPath();
        String leaf = path.substring(path.lastIndexOf('/') + 1);
        return roleFromNarrativeKey(leaf.replaceAll("(_day|_night|_routine|_backroom)$", ""));
    }

    private static String roleFromNarrativeKey(String key) {
        String lower = key == null ? "" : key.toLowerCase(java.util.Locale.ROOT);
        if (lower.contains("witness")) return "witness";
        if (lower.contains("tenant")) return "tenant";
        if (lower.contains("guard")) return "guard";
        if (lower.contains("innkeeper")) return "innkeeper";
        return "npc";
    }

    private static String sanitizeRole(String role) {
        String normalized = role.trim().toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9_./-]", "_");
        if (normalized.contains("witness")) return "witness";
        if (normalized.contains("tenant")) return "tenant";
        if (normalized.contains("guard")) return "guard";
        if (normalized.contains("innkeeper")) return "innkeeper";
        return "npc";
    }

    private static String normalizeDebug(String value, String fallback) {
        return value == null || value.isBlank()
                ? fallback
                : value.trim().toLowerCase(java.util.Locale.ROOT);
    }

    private static RawAnimation animationFor(String animation, boolean moving) {
        String normalized = normalizeDebug(animation, "idle");
        if (moving || "walk".equals(normalized)) {
            return WALK_ANIMATION;
        }
        return switch (normalized) {
            case "fidget" -> FIDGET_ANIMATION;
            case "talk" -> TALK_ANIMATION;
            case "think" -> THINK_ANIMATION;
            case "dismiss" -> DISMISS_ANIMATION;
            case "nervous_idle", "nervous" -> NERVOUS_IDLE_ANIMATION;
            default -> IDLE_ANIMATION;
        };
    }
}
