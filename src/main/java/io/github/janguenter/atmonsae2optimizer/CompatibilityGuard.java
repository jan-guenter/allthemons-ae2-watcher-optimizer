package io.github.janguenter.atmonsae2optimizer;

import appeng.api.config.Actionable;
import appeng.api.networking.IGridNode;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.storage.IStorageWatcherNode;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import appeng.me.helpers.InterestManager;
import appeng.me.helpers.StackWatcher;
import appeng.me.service.StorageService;
import appeng.me.storage.NetworkStorage;
import com.google.common.collect.SetMultimap;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.fml.ModList;

public final class CompatibilityGuard {
    private static final String EXPECTED_AE2 = "19.2.17";
    private static final String EXPECTED_EXTENDED_AE = "1.21-2.2.35-neoforge";
    private static final String EXPECTED_ME_REQUESTER = "1.21.1-1.4.3";

    private CompatibilityGuard() {
    }

    public static void verify() {
        requireVersion("ae2", EXPECTED_AE2, true);
        requireVersion("extendedae", EXPECTED_EXTENDED_AE, false);
        requireVersion("merequester", EXPECTED_ME_REQUESTER, false);
        verifyAe2Signatures();
    }

    public static void verifyAe2Signatures() {
        try {
            requireMethod(StorageService.class, "onServerEndTick");
            requireMethod(StorageService.class, "updateCachedStacks");
            requireMethod(StorageService.class, "postWatcherUpdate", AEKey.class, long.class);
            requireMethod(StorageService.class, "addNode", IGridNode.class, CompoundTag.class);
            requireField(StorageService.class, "interestManager", InterestManager.class);
            requireField(StorageService.class, "interests", SetMultimap.class);
            requireField(StorageService.class, "watchers", Map.class);
            requireField(StorageService.class, "cachedStacksNeedUpdate", boolean.class);

            requireMethod(InterestManager.class, "isEmpty");
            requireMethod(InterestManager.class, "get", AEKey.class);
            requireMethod(InterestManager.class, "getAllStacksWatchers");
            requireMethod(StackWatcher.class, "getHost");

            requireMethod(NetworkStorage.class, "getAvailableStacks", KeyCounter.class);
            requireMethod(
                    NetworkStorage.class,
                    "extract",
                    AEKey.class,
                    long.class,
                    Actionable.class,
                    IActionSource.class);
            requireMethod(IStorageWatcherNode.class, "onStackChange", AEKey.class, long.class);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(
                    "AE2 19.2.17 storage watcher signatures changed; refusing to enable the optimizer",
                    exception);
        }
    }

    private static void requireVersion(String modId, String expectedVersion, boolean required) {
        var container = ModList.get().getModContainerById(modId);
        if (container.isEmpty()) {
            if (required) {
                throw new IllegalStateException("Required mod " + modId + " is not loaded");
            }
            return;
        }

        var actualVersion = container.get().getModInfo().getVersion().toString();
        if (!expectedVersion.equals(actualVersion)) {
            throw new IllegalStateException(
                    "Unsupported " + modId + " version " + actualVersion + "; expected exactly " + expectedVersion);
        }
    }

    private static Method requireMethod(Class<?> owner, String name, Class<?>... parameters)
            throws NoSuchMethodException {
        return owner.getDeclaredMethod(name, parameters);
    }

    private static Field requireField(Class<?> owner, String name, Class<?> type)
            throws NoSuchFieldException {
        var field = owner.getDeclaredField(name);
        if (field.getType() != type) {
            throw new NoSuchFieldException(
                    owner.getName() + '#' + name + " has type " + field.getType().getName()
                            + ", expected " + type.getName());
        }
        return field;
    }
}
