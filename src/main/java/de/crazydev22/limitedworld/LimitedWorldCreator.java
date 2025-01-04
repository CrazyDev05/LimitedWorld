package de.crazydev22.limitedworld;

import com.volmit.iris.core.loader.IrisData;
import com.volmit.iris.engine.object.IrisDimension;
import com.volmit.iris.engine.object.IrisWorld;
import com.volmit.iris.engine.platform.BukkitChunkGenerator;
import lombok.Data;
import lombok.experimental.Accessors;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.generator.ChunkGenerator;

import java.io.File;

@Data
@Accessors(fluent = true, chain = true)
public class LimitedWorldCreator {
    private String name;
    private String dimension = null;
    private long seed = 1337;
    private ChunkLimiter chunkLimiter = (x, z) -> false;

    public WorldCreator create() {
        IrisDimension dim = IrisData.loadAnyDimension(dimension);
        if (dim == null)
            throw new IllegalArgumentException("Dimension '" + dimension + "' not found");

        World.Environment env = dim.getEnvironment() == null || dim.getEnvironment() == World.Environment.CUSTOM ?
                World.Environment.NORMAL :
                dim.getEnvironment();
        IrisWorld irisWorld = IrisWorld.builder()
                .name(name)
                .minHeight(dim.getMinHeight())
                .maxHeight(dim.getMaxHeight())
                .seed(seed)
                .worldFolder(new File(Bukkit.getWorldContainer(), name))
                .environment(env)
                .build();
        ChunkGenerator g = new LimitedChunkGenerator(new BukkitChunkGenerator(irisWorld, false, new File(irisWorld.worldFolder(), "iris/pack"), dimension, false), chunkLimiter);

        return new WorldCreator(name)
                .environment(env)
                .generateStructures(true)
                .generator(g).seed(seed);
    }
}
