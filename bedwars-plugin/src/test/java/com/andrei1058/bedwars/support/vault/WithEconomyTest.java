package com.andrei1058.bedwars.support.vault;

import net.milkbowl.vault.economy.Economy;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WithEconomyTest {

    @Test
    void purchaseWithdrawsFromPlayerAccountInsteadOfBankAccount() {
        AtomicReference<String> invokedMethod = new AtomicReference<>();
        Economy provider = (Economy) Proxy.newProxyInstance(
                Economy.class.getClassLoader(),
                new Class<?>[]{Economy.class},
                (proxy, method, arguments) -> {
                    invokedMethod.set(method.getName());
                    return defaultValue(method.getReturnType());
                });

        new WithEconomy(provider).buyAction(null, 5D);

        assertEquals("withdrawPlayer", invokedMethod.get());
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) return null;
        if (type == boolean.class) return false;
        if (type == char.class) return '\0';
        if (type == byte.class) return (byte) 0;
        if (type == short.class) return (short) 0;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == float.class) return 0F;
        if (type == double.class) return 0D;
        return null;
    }
}
