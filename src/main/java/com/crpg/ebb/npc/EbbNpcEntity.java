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
    private final AnimatableInstanceCache geckoCache = GeckoLibUtil.createInstanceCache(this);
    private Optional<Identifier> routineId = Optional.of(EbbMod.id("demo/innkeeper_day"));
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
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        routineId = input.getString(ROUTINE_TAG).map(Identifier::parse).or(() -> Optional.of(EbbMod.id("demo/innkeeper_day")));
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
