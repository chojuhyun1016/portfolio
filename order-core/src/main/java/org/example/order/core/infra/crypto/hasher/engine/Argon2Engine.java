package org.example.order.core.infra.crypto.hasher.engine;

import de.mkammerer.argon2.Argon2;
import de.mkammerer.argon2.Argon2Factory;

public class Argon2Engine {

    private static final int ITERATIONS = 2;
    private static final int MEMORY_KB = 65536;
    private static final int PARALLELISM = 1;

    private static final Argon2 argon2 = Argon2Factory.create();

    public static String hash(char[] plainPassword) {
        return argon2.hash(ITERATIONS, MEMORY_KB, PARALLELISM, plainPassword);
    }

    public static boolean verify(String hash, char[] plainPassword) {
        return argon2.verify(hash, plainPassword);
    }

    public static void wipe(char[] password) {
        argon2.wipeArray(password);
    }
}
