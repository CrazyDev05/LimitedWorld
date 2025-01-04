package de.crazydev22.limitedworld;

import com.google.common.util.concurrent.AtomicDouble;
import com.volmit.iris.Iris;
import com.volmit.iris.core.IrisSettings;
import com.volmit.iris.core.ServerConfigurator;
import com.volmit.iris.core.pregenerator.PregenTask;
import com.volmit.iris.core.service.StudioSVC;
import com.volmit.iris.core.tools.IrisToolbelt;
import com.volmit.iris.engine.object.IrisDimension;
import com.volmit.iris.util.exceptions.IrisException;
import com.volmit.iris.util.format.C;
import com.volmit.iris.util.format.Form;
import com.volmit.iris.util.paper.PaperLib;
import com.volmit.iris.util.plugin.VolmitSender;
import com.volmit.iris.util.scheduling.J;
import lombok.Data;
import lombok.experimental.Accessors;
import org.bukkit.*;

import java.io.File;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * Makes it a lot easier to setup an engine, world, studio or whatever
 */
@Data
@Accessors(fluent = true, chain = true)
public class LimitedCreator {
    /**
     * Specify an area to pregenerate during creation
     */
    private PregenTask pregen;
    /**
     * Specify a sender to get updates & progress info + tp when world is created.
     */
    private VolmitSender sender;
    /**
     * The seed to use for this generator
     */
    private long seed = 1337;
    /**
     * The dimension to use. This can be any online dimension, or a dimension in the
     * packs folder
     */
    private String dimension = IrisSettings.get().getGenerator().getDefaultWorldType();
    /**
     * The name of this world.
     */
    private String name = "irisworld";
    private ChunkLimiter chunkLimiter = (x, z) -> false;

    /**
     * Create the IrisAccess (contains the world)
     *
     * @return the IrisAccess
     * @throws IrisException shit happens
     */

    public World create() throws IrisException {
        if (Bukkit.isPrimaryThread()) {
            throw new IrisException("You cannot invoke create() on the main thread.");
        }

        IrisDimension d = IrisToolbelt.getDimension(dimension());

        if (d == null) {
            throw new IrisException("Dimension cannot be found null for id " + dimension());
        }

        if (sender == null)
            sender = Iris.getSender();

        Iris.service(StudioSVC.class).installIntoWorld(sender, d.getLoadKey(), new File(Bukkit.getWorldContainer(), name()));

        AtomicReference<World> world = new AtomicReference<>();
        AtomicDouble pp = new AtomicDouble(0);
        WorldCreator wc = new LimitedWorldCreator()
                .dimension(dimension)
                .name(name)
                .seed(seed)
                .create();

        var access = (LimitedChunkGenerator) wc.generator();
        if (access == null)
            throw new IrisException("Access is null. Something bad happened.");

        ServerConfigurator.installDataPacks(false);
        J.a(() -> {
            Supplier<Integer> g = () -> {
                if (access.getEngine() == null)
                    return 0;
                return access.getEngine().getGenerated();
            };
            int req = access.getSpawnChunks().join();

            while (g.get() < req) {
                double v = (double) g.get() / (double) req;
                if (sender.isPlayer()) {
                    sender.sendProgress(v, "Generating");
                    J.sleep(16);
                } else {
                    sender.sendMessage(C.WHITE + "Generating " + Form.pc(v) + ((C.GRAY + " (" + (req - g.get()) + " Left)")));
                    J.sleep(1000);
                }
            }
        });


        try {
            J.s(() -> world.set(wc.createWorld()));
            world.get();
        } catch (Throwable e) {
            e.printStackTrace();
        }

        if (sender.isPlayer()) {
            PaperLib.teleportAsync(sender.player(), world.get().getHighestBlockAt(0, 0).getLocation());
        }

        if (pregen != null) {
            CompletableFuture<Boolean> ff = new CompletableFuture<>();

            IrisToolbelt.pregenerate(pregen, access)
                    .onProgress(pp::set)
                    .whenDone(() -> ff.complete(true));

            try {
                AtomicBoolean dx = new AtomicBoolean(false);

                J.a(() -> {
                    while (!dx.get()) {
                        if (sender.isPlayer()) {
                            sender.sendProgress(pp.get(), "Pregenerating");
                            J.sleep(16);
                        } else {
                            sender.sendMessage(C.WHITE + "Pregenerating " + Form.pc(pp.get()));
                            J.sleep(1000);
                        }
                    }
                });

                ff.get();
                dx.set(true);
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
        return world.get();
    }
}