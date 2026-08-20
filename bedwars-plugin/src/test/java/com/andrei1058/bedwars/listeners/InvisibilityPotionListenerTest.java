package com.andrei1058.bedwars.listeners;

import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InvisibilityPotionListenerTest {

    @Test
    void activatesOnlyForAnAcceptedDrunkInvisibilityEffect() {
        assertTrue(InvisibilityPotionListener.isDrunkInvisibility(
                EntityPotionEffectEvent.Cause.POTION_DRINK,
                EntityPotionEffectEvent.Action.ADDED,
                false,
                true));
        assertTrue(InvisibilityPotionListener.isDrunkInvisibility(
                EntityPotionEffectEvent.Cause.POTION_DRINK,
                EntityPotionEffectEvent.Action.CHANGED,
                true,
                true));
        assertFalse(InvisibilityPotionListener.isDrunkInvisibility(
                EntityPotionEffectEvent.Cause.POTION_DRINK,
                EntityPotionEffectEvent.Action.CHANGED,
                false,
                true));
        assertFalse(InvisibilityPotionListener.isDrunkInvisibility(
                EntityPotionEffectEvent.Cause.PLUGIN,
                EntityPotionEffectEvent.Action.ADDED,
                false,
                true));
        assertFalse(InvisibilityPotionListener.isDrunkInvisibility(
                EntityPotionEffectEvent.Cause.POTION_DRINK,
                EntityPotionEffectEvent.Action.REMOVED,
                false,
                true));
        assertFalse(InvisibilityPotionListener.isDrunkInvisibility(
                EntityPotionEffectEvent.Cause.POTION_DRINK,
                EntityPotionEffectEvent.Action.ADDED,
                false,
                false));
    }

    @Test
    void roundsPartialSecondsUpSoShortEffectsAreStillTracked() {
        assertEquals(1, InvisibilityPotionListener.durationSeconds(1));
        assertEquals(2, InvisibilityPotionListener.durationSeconds(21));
        assertEquals(30, InvisibilityPotionListener.durationSeconds(600));
    }
}
