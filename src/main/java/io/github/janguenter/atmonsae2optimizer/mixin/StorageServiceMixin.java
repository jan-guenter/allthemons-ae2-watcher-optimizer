package io.github.janguenter.atmonsae2optimizer.mixin;

import appeng.api.networking.IGridNode;
import appeng.api.networking.storage.IStorageWatcherNode;
import appeng.api.stacks.AEKey;
import appeng.me.helpers.InterestManager;
import appeng.me.helpers.StackWatcher;
import appeng.me.service.StorageService;
import com.google.common.collect.SetMultimap;
import io.github.janguenter.atmonsae2optimizer.config.OptimizerConfig;
import io.github.janguenter.atmonsae2optimizer.diagnostics.DiagnosticsRegistry;
import io.github.janguenter.atmonsae2optimizer.diagnostics.DiagnosticsReporter;
import io.github.janguenter.atmonsae2optimizer.diagnostics.GridMetrics;
import io.github.janguenter.atmonsae2optimizer.scheduling.CensusSchedule;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(StorageService.class)
public abstract class StorageServiceMixin {
    @Shadow
    @Final
    private SetMultimap<AEKey, StackWatcher<IStorageWatcherNode>> interests;

    @Shadow
    @Final
    private InterestManager<StackWatcher<IStorageWatcherNode>> interestManager;

    @Shadow
    @Final
    private Map<IGridNode, StackWatcher<IStorageWatcherNode>> watchers;

    @Shadow
    private boolean cachedStacksNeedUpdate;

    @Unique
    private GridMetrics atmons$metrics;

    @Unique
    private long atmons$tick;

    @Unique
    private long atmons$refreshStartedNanos;

    @Unique
    private boolean atmons$refreshWasScheduled;

    @Unique
    private boolean atmons$scheduledCensus;

    @Unique
    private boolean atmons$finishAtReturn;

    @Inject(method = "<init>()V", at = @At("RETURN"), require = 1)
    private void atmons$initialize(CallbackInfo callback) {
        atmons$metrics = new GridMetrics();
        DiagnosticsRegistry.register(this, atmons$metrics);
    }

    @Inject(
            method = "addNode(Lappeng/api/networking/IGridNode;Lnet/minecraft/nbt/CompoundTag;)V",
            at = @At("RETURN"),
            require = 1)
    private void atmons$resolveGridIdentity(IGridNode node, CompoundTag savedData, CallbackInfo callback) {
        try {
            atmons$metrics.identify(
                    node.getGrid().toString(),
                    node.getLevel().dimension().location().toString());
        } catch (IllegalStateException exception) {
            // The diagnostics keep a deterministic unresolved identity until another node can resolve the grid.
        }
    }

    @Inject(method = "onServerEndTick()V", at = @At("HEAD"), cancellable = true, require = 1)
    private void atmons$scheduleCensus(CallbackInfo callback) {
        var tick = atmons$tick++;
        var watcherInterestsPresent = !interestManager.isEmpty();
        var enabled = OptimizerConfig.VALUES.intervalThrottlingEnabled.get();
        var interval = OptimizerConfig.VALUES.fullCensusIntervalTicks.get();
        var phase = CensusSchedule.phaseFor(
                atmons$metrics.scheduleIdentity(),
                interval,
                OptimizerConfig.VALUES.staggerGrids.get());
        var throttlingActive = enabled && interval > 1 && watcherInterestsPresent;

        var gainedWatchAll = atmons$metrics.observe(
                watchers.size(),
                interests.keySet().size(),
                interestManager.getAllStacksWatchers().size(),
                throttlingActive,
                interval,
                phase);
        DiagnosticsReporter.watchAllChanged(atmons$metrics, gainedWatchAll);

        atmons$scheduledCensus = watcherInterestsPresent;
        atmons$finishAtReturn = true;

        if (throttlingActive && !CensusSchedule.shouldRun(tick, interval, phase)) {
            // Preserve AE2's lazy cache contract. A terminal or another stock cache consumer may still
            // request a full rebuild before the next scheduled watcher census.
            cachedStacksNeedUpdate = true;
            atmons$scheduledCensus = false;
            atmons$metrics.recordSkippedCensus();
            atmons$finishTick(tick);
            callback.cancel();
        }
    }

    @Inject(method = "onServerEndTick()V", at = @At("RETURN"), require = 1)
    private void atmons$finishUncancelledTick(CallbackInfo callback) {
        if (atmons$finishAtReturn) {
            atmons$finishTick(atmons$tick - 1);
        }
    }

    @Inject(method = "updateCachedStacks()V", at = @At("HEAD"), require = 1)
    private void atmons$startRefreshTiming(CallbackInfo callback) {
        atmons$refreshStartedNanos = System.nanoTime();
        atmons$refreshWasScheduled = atmons$scheduledCensus;
    }

    @Inject(method = "updateCachedStacks()V", at = @At("RETURN"), require = 1)
    private void atmons$finishRefreshTiming(CallbackInfo callback) {
        atmons$metrics.recordRefresh(
                atmons$refreshWasScheduled,
                System.nanoTime() - atmons$refreshStartedNanos);
    }

    @Inject(
            method = "postWatcherUpdate(Lappeng/api/stacks/AEKey;J)V",
            at = @At("HEAD"),
            require = 1)
    private void atmons$countNotification(AEKey what, long newAmount, CallbackInfo callback) {
        var callbackCount = interestManager.get(what).size()
                + interestManager.getAllStacksWatchers().size();
        atmons$metrics.recordNotification(callbackCount);
    }

    @Unique
    private void atmons$finishTick(long tick) {
        atmons$scheduledCensus = false;
        atmons$finishAtReturn = false;
        DiagnosticsReporter.maybeLog(atmons$metrics, tick);
    }
}
