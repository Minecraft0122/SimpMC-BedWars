package com.andrei1058.bedwars.support.vault;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VaultIntegrationTest {

    @Test
    void identifiesLegacyVaultServiceTypesWithoutDependingOnPluginName() {
        assertTrue(VaultIntegration.isVaultServiceName("net.milkbowl.vault.economy.Economy"));
        assertTrue(VaultIntegration.isVaultServiceName("net.milkbowl.vault.chat.Chat"));
        assertFalse(VaultIntegration.isVaultServiceName("org.example.Economy"));
        assertFalse(VaultIntegration.isVaultServiceName(null));
    }
}
