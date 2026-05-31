package com.crpg.ebb.interaction;

public final class InteractionSyncLimits {
    public static final int MAX_BLOCK_GROUPS = 2048;
    public static final int MAX_BLOCKS_PER_GROUP = 512;
    public static final int MAX_ENTITY_BINDINGS = 2048;
    public static final int MAX_ENTITY_BINDING_TAGS = 64;
    public static final int MAX_ENTITY_BINDING_TYPES = 64;
    public static final int MAX_ENTITY_BINDING_STRING_LENGTH = 128;
    public static final int MAX_SYNCED_ENTITY_TARGETS = 512;

    private InteractionSyncLimits() {
    }
}
