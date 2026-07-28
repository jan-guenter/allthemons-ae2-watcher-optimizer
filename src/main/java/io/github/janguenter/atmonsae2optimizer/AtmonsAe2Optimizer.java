package io.github.janguenter.atmonsae2optimizer;

import com.mojang.logging.LogUtils;
import io.github.janguenter.atmonsae2optimizer.command.DiagnosticsCommand;
import io.github.janguenter.atmonsae2optimizer.config.OptimizerConfig;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

@Mod(value = AtmonsAe2Optimizer.MOD_ID, dist = Dist.DEDICATED_SERVER)
public final class AtmonsAe2Optimizer {
    public static final String MOD_ID = "atmons_ae2_optimizer";
    public static final Logger LOGGER = LogUtils.getLogger();

    public AtmonsAe2Optimizer(IEventBus modEventBus, ModContainer modContainer) {
        OptimizerConfig.register(modContainer);
        CompatibilityGuard.verify();
        NeoForge.EVENT_BUS.addListener(DiagnosticsCommand::register);

        LOGGER.info(
                "All the Mons AE2 Watcher Optimizer loaded: exact AE2 19.2.17 guard passed; "
                        + "exact-interest polling remains disabled");
    }
}
