package com.andrei1058.bedwars.support.version.v1_21_R3;

public final class FireballAccelerationRuntimeProbe {

    private FireballAccelerationRuntimeProbe() {
    }

    public static void main(String[] args) {
        // The probe is intentionally compile/runtime-linkage based. A real
        // Fireball requires a loaded Bukkit world, which CI does not start;
        // invoking the method reference forces JVM linkage to Paper's method
        // instead of merely constructing an unused lambda.
        try {
            java.lang.reflect.Method setter = org.bukkit.entity.Fireball.class
                    .getMethod("setAcceleration", org.bukkit.util.Vector.class);
            if (setter.getReturnType() != void.class) {
                throw new IllegalStateException("Unexpected Paper Fireball#setAcceleration signature");
            }
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Paper Fireball acceleration API unavailable", exception);
        }
    }
}
