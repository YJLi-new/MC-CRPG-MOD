package com.crpg.ebb.api;

import com.crpg.ebb.dialogue.RollMode;

public record RollRule(String attribute, int dc, RollMode mode, boolean advantage, int modifier) {
}
