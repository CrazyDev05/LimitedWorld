package de.crazydev22.limitedworld;

import com.volmit.iris.engine.framework.Engine;
import com.volmit.iris.engine.platform.BukkitChunkGenerator;
import com.volmit.iris.engine.platform.PlatformChunkGenerator;
import lombok.AllArgsConstructor;
import org.bukkit.World;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;

import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

@AllArgsConstructor
public class LimitedChunkGenerator extends ChunkGenerator implements PlatformChunkGenerator {
    private final BukkitChunkGenerator delegate;
    private final ChunkLimiter chunkLimiter;

    public void generateNoise(WorldInfo world, Random random, int x, int z, ChunkGenerator.ChunkData d) {
        if (chunkLimiter.block(x, z)) return;
        delegate.generateNoise(world, random, x, z, d);
    }

        @Override
    public Engine getEngine() {
        return delegate.getEngine();
    }

    @Override
    public void injectChunkReplacement(World world, int x, int z, Consumer<Runnable> jobs) {
        if (chunkLimiter.block(x, z)) return;
        delegate.injectChunkReplacement(world, x, z, jobs);
    }

    @Override
    public void close() {
        delegate.close();
    }

    @Override
    public boolean isStudio() {
        return delegate.isStudio();
    }

    @Override
    public void touch(World world) {
        delegate.touch(world);
    }

    @Override
    public CompletableFuture<Integer> getSpawnChunks() {
        return delegate.getSpawnChunks();
    }

    @Override
    public void hotload() {
        delegate.hotload();
    }
}
