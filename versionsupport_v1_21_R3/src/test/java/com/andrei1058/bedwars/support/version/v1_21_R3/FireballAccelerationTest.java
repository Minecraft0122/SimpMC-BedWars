package com.andrei1058.bedwars.support.version.v1_21_R3;

import org.bukkit.entity.Fireball;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FireballAccelerationTest {

    @Test
    void writesThePaperAccelerationVector() {
        AtomicReference<Vector> acceleration = new AtomicReference<>();
        Fireball fireball = (Fireball) Proxy.newProxyInstance(
                Fireball.class.getClassLoader(),
                new Class<?>[]{Fireball.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("setAcceleration")) {
                        acceleration.set((Vector) args[0]);
                        return null;
                    }
                    if (method.getReturnType() == boolean.class) return false;
                    if (method.getReturnType() == int.class) return 0;
                    if (method.getReturnType() == double.class) return 0D;
                    return null;
                });

        Vector expected = new Vector(0.0D, 0.2D, 0.0D);
        fireball.setAcceleration(expected);

        assertEquals(expected, acceleration.get());
    }

    @Test
    void adapterRestoresVelocityAfterPaperSetter() {
        AtomicReference<Vector> velocity = new AtomicReference<>(new Vector(1.25D, 0.4D, -0.75D));
        AtomicReference<Vector> acceleration = new AtomicReference<>();
        Fireball fireball = (Fireball) Proxy.newProxyInstance(
                Fireball.class.getClassLoader(),
                new Class<?>[]{Fireball.class},
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "getVelocity" -> { return velocity.get().clone(); }
                        case "setVelocity" -> { velocity.set(((Vector) args[0]).clone()); return true; }
                        case "setAcceleration" -> {
                            acceleration.set(((Vector) args[0]).clone());
                            // Model Paper's setter, which also changes the
                            // current velocity, so the adapter's restoration
                            // is tested rather than only its invocation.
                            velocity.set(((Vector) args[0]).clone());
                            return null;
                        }
                        default -> {
                            if (method.getReturnType() == boolean.class) return false;
                            if (method.getReturnType() == int.class) return 0;
                            if (method.getReturnType() == double.class) return 0D;
                            return null;
                        }
                    }
                });

        Vector initialVelocity = velocity.get().clone();
        Vector expectedAcceleration = new Vector(0.0D, 0.2D, 0.0D);
        new v1_21_R3(null, "1.21.11").setFireballAcceleration(fireball, expectedAcceleration);

        assertEquals(expectedAcceleration, acceleration.get());
        assertEquals(initialVelocity, velocity.get());
    }
}
