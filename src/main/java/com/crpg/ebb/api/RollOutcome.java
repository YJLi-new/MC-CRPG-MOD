package com.crpg.ebb.api;

public record RollOutcome(int dieRoll, int modifier, int total, boolean success, boolean critical, String outcome) {
}
