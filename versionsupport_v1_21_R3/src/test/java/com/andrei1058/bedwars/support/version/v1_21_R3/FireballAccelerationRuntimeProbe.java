package com.andrei1058.bedwars.support.version.v1_21_R3;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class FireballAccelerationRuntimeProbe {

    private FireballAccelerationRuntimeProbe() {
    }

    public static void main(String[] args) throws ReflectiveOperationException {
        Class<?> craftFireball = Class.forName("org.bukkit.craftbukkit.entity.CraftFireball");
        Method getHandle = craftFireball.getMethod("getHandle");
        Field accelerationPower = getHandle.getReturnType().getField("accelerationPower");
        if (accelerationPower.getType() != double.class) {
            throw new IllegalStateException("Unexpected fireball acceleration field type: "
                    + accelerationPower.getType().getName());
        }
    }
}
