package de.crazydev22.limitedworld;

@FunctionalInterface
public interface ChunkLimiter {
    boolean block(int chunkX, int chunkZ);
}
