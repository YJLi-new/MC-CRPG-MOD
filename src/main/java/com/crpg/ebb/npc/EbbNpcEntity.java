package com.crpg.ebb.npc;

import com.crpg.ebb.EbbMod;
import com.crpg.ebb.routine.NpcRoutineController;
import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.constant.DefaultAnimations;
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
    private static final String ROUTINE_TAG = "EbbRoutine";
    private static final String NARRATIVE_KEY_TAG = "EbbNarrativeKey";
    private static final String POSE_TAG = "EbbPose";
    private static final String ANIMATION_TAG = "EbbAnimation";
    private final AnimatableInstanceCache geckoCache = GeckoLibUtil.createInstanceCache(this);
    private Optional<Identifier> routineId = Optional.of(EbbMod.id("demo/innkeeper_day"));
    private String narrativeStateKey = "ebb:demo/innkeeper";
    private String narrativePose = "standing";
    private String narrativeAnimation = "idle";
    private String routinePathKey = "";
    private int routinePathIndex;

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
    }

    public String narrativeStateKey() {
        return narrativeStateKey;
    }

    public void setNarrativeStateKey(String narrativeStateKey) {
        if (narrativeStateKey != null && !narrativeStateKey.isBlank()) {
            this.narrativeStateKey = narrativeStateKey.trim().toLowerCase(java.util.Locale.ROOT);
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
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        routineId = input.getString(ROUTINE_TAG).map(Identifier::parse).or(() -> Optional.of(EbbMod.id("demo/innkeeper_day")));
        narrativeStateKey = input.getString(NARRATIVE_KEY_TAG).orElse("ebb:demo/innkeeper");
        narrativePose = input.getString(POSE_TAG).orElse("standing");
        narrativeAnimation = input.getString(ANIMATION_TAG).orElse("idle");
        resetRoutinePath();
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(DefaultAnimations.genericWalkIdleController());
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geckoCache;
    }
}
