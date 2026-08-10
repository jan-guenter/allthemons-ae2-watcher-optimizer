# Agent guide: All the Mons AE2 Watcher Optimizer

This repository is the independent project
`allthemons-ae2-watcher-optimizer`.

| Property | Value |
| --- | --- |
| Mod ID | `atmons_ae2_optimizer` |
| Maven artifact ID | `allthemons-ae2-watcher-optimizer` |
| Maven group | `io.github.jan-guenter` |
| Initial version | `0.1.0` |
| Current reviewed version | `0.1.1` |
| License for this add-on | MIT |
| Java | 21 |
| Gradle wrapper | 8.10.2 |

## Non-negotiable invariants

- This is a dedicated-server-only deployment add-on. An unmodified pack
  client must not need the JAR.
- Register no blocks, items, entities, block entities, menus, recipes,
  payloads, packet channels, screens, renderers, or client hooks.
- Never copy, shade, patch in place, or redistribute an AE2, ExtendedAE,
  ME Requester, Minecraft, NeoForge, or mod-pack JAR.
- Never write required custom state into a world, grid, block entity, entity,
  item, or saved-data file. Config and memory-only diagnostics are allowed.
- Removing the JAR must restore stock behavior after one restart.
- Disabling interval throttling or setting its interval to 1 must restore
  AE2's stock end-tick scheduling.
- A due census must execute AE2's own `updateCachedStacks()` method. Do not
  recreate availability, diffing, or watcher notification logic.
- A skipped census must leave `cachedStacksNeedUpdate = true`, allowing an
  ordinary AE2 cache consumer to demand the stock rebuild.
- Do not claim dedicated-server, unmodified-client, production-world, or
  Spark verification unless it was actually run and recorded.

## Authoritative compatibility evidence

The current compatibility investigation used these exact versions:

| Component | Exact evidence |
| --- | --- |
| All the Mons | 1.2.0, repository commit `c7bb230f21d14d26859d0b92548f089b3a493ad9` |
| Minecraft | 1.21.1 |
| NeoForge | 21.1.248 |
| AE2 source | tag `neoforge/v19.2.17`, commit `79ee2c704ad62941a426c26b1cb1f76ef5b2ee5a` |
| AE2 JAR | `appliedenergistics2-19.2.17.jar`, SHA-256 `460d779a0609b81409907d9956de8f6f70a1b0912257e3e5c3c7e75ac9630e95` |
| ExtendedAE source | tag `1.21-2.2.35-neoforge`, commit `3776bc854458301bbcc9a44a8238d70a0e3dc00d` |
| ExtendedAE JAR | SHA-256 `14a2860fa2c747e9dda2279b8933fac6311fecfee166c765171022b902591c65` |
| ME Requester source | tag `v1.21.1-neoforge-1.4.3`, commit `fce75946bbb00c7a4705cb114d1423a284fb112a` |
| ME Requester JAR | SHA-256 `68f3c861a802d48afeb6e3a48e8ee4f8633904340ac3f89f17493dc84490e385` |

The complete 1.2.0 audit, including changed adjacent mods and exact CurseForge
project/file identities, is in [`docs/compatibility-1.2.0.md`](docs/compatibility-1.2.0.md).
Source/JAR paths in the investigation workspace must never be added to this
repository.

## Exact AE2 19.2.17 patch surface

The required target is `appeng.me.service.StorageService`.

Verified members and erased descriptors:

```text
public StorageService()V
public onServerEndTick()V
private updateCachedStacks()V
private postWatcherUpdate(Lappeng/api/stacks/AEKey;J)V
public addNode(
  Lappeng/api/networking/IGridNode;
  Lnet/minecraft/nbt/CompoundTag;
)V
private interestManager:Lappeng/me/helpers/InterestManager;
private interests:Lcom/google/common/collect/SetMultimap;
private watchers:Ljava/util/Map;
private cachedStacksNeedUpdate:Z
```

Also verified:

```text
InterestManager.isEmpty()Z
InterestManager.get(AEKey)Ljava/util/Collection;
InterestManager.getAllStacksWatchers()Ljava/util/Collection;
StackWatcher.getHost()Ljava/lang/Object;
NetworkStorage.getAvailableStacks(KeyCounter)V
NetworkStorage.extract(AEKey,long,Actionable,IActionSource)J
IStorageWatcherNode.onStackChange(AEKey,long)V
```

`CompatibilityGuard` checks the versions and these members at startup.
`atmons_ae2_optimizer.mixins.json` is required, uses Java 21 compatibility,
places `StorageServiceMixin` only in the physical-side `server` list, and has
`defaultRequire = 1`. Every injection also explicitly uses `require = 1`.

`StorageServiceMixin` injects only:

- constructor return, to allocate memory-only metrics;
- `addNode` return, to resolve a runtime diagnostic identity;
- `onServerEndTick` head/return, to choose stock execution or a dirty-cache
  skip;
- `updateCachedStacks` head/return, for timing and refresh classification; and
- `postWatcherUpdate` head, to count stock callback dispatch attempts.

The scheduling identity combines AE2's runtime `Grid #<serial>` string and
the first resolved node dimension. It is only deterministic during that live
server process. It is not stable across restarts, must never be persisted, and
must never become a durable per-grid configuration key.

## Watcher implementation audit

The exact sources and constant-pool references in the supplied shipped JARs
were scanned for `IStorageWatcherNode`.

### AE2 19.2.17

- `StorageLevelEmitterPart` has an `IStorageWatcherNode`.
- A configured non-fuzzy emitter calls `storageWatcher.add(myStack)`.
- A blank filter or fuzzy-card emitter calls
  `storageWatcher.setWatchAll(true)`.
- Crafting-card mode resets the storage watcher and uses the separate crafting
  watcher; it does not create a storage interest in that mode.
- `AbstractMonitorPart` adds its configured exact item to its storage watcher.
  Conversion Monitor subclasses share this behavior.

### ExtendedAE 1.21-2.2.35-neoforge

- `PartThresholdLevelEmitter` has an `IStorageWatcherNode`.
- A configured non-fuzzy threshold emitter registers one exact key.
- A blank filter or fuzzy-card threshold emitter registers watch-all.
- Crafting-card mode uses its separate crafting watcher.

### ME Requester 1.21.1-1.4.3

- `RequesterBlockEntity` installs `StorageManager` as its
  `IStorageWatcherNode`.
- `StorageManager.populateWatcher` adds each configured request key as an
  exact interest.
- `computeKnownAmount` initializes state from
  `getInventory().getAvailableStacks().get(key)`, i.e. census availability.

In the supplied shipped JAR set, only AE2, ExtendedAE, and ME Requester had
implementations/registrations referencing `IStorageWatcherNode`.

## Why exact-interest polling remains unsupported

Do not implement exact polling by calling simulated extraction.

AE2 19.2.17's `MEStorage` API has full availability enumeration and exact
insert/extract methods. It has no exact-key availability method with the same
semantics as `getAvailableStacks`. A simulated extraction query reports what
the caller can extract, which can differ from the amount reported available
by secured, insert-only, non-extractable, or custom `MEStorage`
implementations. ME Requester explicitly relies on census availability.

No supplied test environment proves equivalence across ordinary cells, fluid
cells, MEGA Cells, storage buses, Sophisticated Storage, Functional Storage,
security restrictions, chemical storage, crafting storage, and craftable-only
keys. Exact polling would therefore guess at semantics and is intentionally
absent. Diagnostics report exact interests but do not poll them.

Only revisit this if a future exact AE2 API reports census-equivalent
availability, or if an exhaustive provider matrix proves a replacement. Any
such work is a new feature and must retain automatic full-census fallback.

## Interval semantics

`CensusSchedule` computes `floorMod(scheduleIdentity.hashCode(), interval)`
when staggering is enabled. Each interval contains exactly one due tick for
each grid.

On a due tick or in stock mode, the Mixin returns control to the original
`StorageService.onServerEndTick`. On a skipped tick it:

1. sets `cachedStacksNeedUpdate = true`;
2. records a skipped census;
3. cancels only `onServerEndTick`.

It does not call watcher hosts, inspect storage providers, insert/extract
anything, or mutate AE2's private amount cache.

A cache consumer can call `getCachedInventory` after a skip. AE2 then invokes
its own `updateCachedStacks`; the instrumentation records this as
`cacheDriven`. Updates that happen and reverse entirely within an interval may
be coalesced. This limitation must stay prominent in operator documentation.

## Build and tests

Run the full local/CI gate with Java 21:

```bash
./gradlew --no-daemon clean check build
```

The gate includes:

- JUnit 5 schedule tests;
- JUnit 5 diagnostics-counter tests;
- an exact AE2 reflection-signature test;
- ModDevGradle's server-side unit-test launcher with
  `testedMod = mods.atmons_ae2_optimizer`;
- Checkstyle for main and test sources; and
- `verifyProductionJar`.

`verifyProductionJar` requires project classes,
`META-INF/neoforge.mods.toml`, the Mixin configuration, and the manifest. It
rejects any other package or resource, preventing accidental third-party
bundling.

The current suite contains 11 tests. Its ModDevGradle launch loads NeoForge,
AE2, GuideME, the mod entrypoint, the physical-server Mixin configuration, and
the startup guard. This is useful application/signature coverage, but it is
not a full dedicated-server startup, world test, or client-connection test.

After building, independently inspect the production JAR:

```bash
jar tf build/libs/allthemons-ae2-watcher-optimizer-<version>.jar
unzip -p build/libs/allthemons-ae2-watcher-optimizer-<version>.jar \
  META-INF/neoforge.mods.toml
unzip -p build/libs/allthemons-ae2-watcher-optimizer-<version>.jar \
  atmons_ae2_optimizer.mixins.json
```

Do not treat the ModDevGradle unit-test launch as a dedicated-server runtime
test.

### All the Mons 1.2.0 runtime smoke

Release 0.1.1 was additionally tested with a fresh temporary server extracted
from the exact `ServerFiles-1.2.0.zip` archive recorded in
`docs/compatibility-1.2.0.md`. With the production 0.1.1 JAR installed, the
server:

- discovered `atmons_ae2_optimizer` 0.1.1 alongside AE2 19.2.17,
  ExtendedAE 2.2.35, and ME Requester 1.4.3;
- logged that the exact AE2 compatibility guard passed;
- reached `Done` on a new world;
- executed `atmonsae2 status` and reported zero live grids; and
- shut down cleanly through the `stop` command.

This proves fresh-world dedicated-server startup and command registration for
the inspected pack. It does not prove unmodified-client connection,
production-world behavior, the storage-provider transition matrix, or Spark
performance.

## Release procedure

1. Complete the update audit below if any pack/dependency version changes.
2. Update implementation, tests, README, AGENTS, and changelog together.
3. Increase stable SemVer `mod_version` in `gradle.properties`.
4. Run `./gradlew --no-daemon clean check build`.
5. Inspect the production JAR.
6. Commit and merge through a pull request. The version-policy workflow
   requires the version to increase.
7. Create and push tag `v<mod_version>` exactly.
8. The release workflow repeats checks, publishes the `mod` publication to
   `https://maven.pkg.github.com/jan-guenter/allthemons-ae2-watcher-optimizer`,
   creates SHA-256 checksums, and creates a GitHub Release.
9. Confirm that the release contains the production JAR and checksum file,
   not the sources JAR.

The POM coordinates are:

```text
io.github.jan-guenter:allthemons-ae2-watcher-optimizer:<mod_version>
```

## Required pack-update procedure

A new All the Mons release is a fresh compatibility investigation, never a
metadata-only version bump.

1. Check out the exact new pack tag/commit. Record Minecraft, NeoForge, AE2,
   ExtendedAE, ME Requester, and every other relevant JAR filename, mod ID,
   version, SHA-256, source tag, and source commit.
2. Diff the complete mod list, configs, KubeJS, datapacks, and server scripts.
3. Obtain the exact new binary JARs. Inspect every `neoforge.mods.toml`, Mixin
   config, target class bytecode, and all implementations/callers of the
   patched APIs.
4. Re-scan every installed JAR for `IStorageWatcherNode`, `IStackWatcher`,
   watcher registration, `setWatchAll`, and direct cached-inventory use.
   Re-audit exact, fuzzy, blank, watch-all, crafting-card, and monitor
   behavior. Search for other Mixins targeting `StorageService`.
5. Re-read `StorageService.onServerEndTick`, `updateCachedStacks`,
   `postWatcherUpdate`, `InterestManager`, `StackWatcher`, `NetworkStorage`,
   `MEStorage`, `StorageLevelEmitterPart`, `AbstractMonitorPart`, every
   ExtendedAE emitter, and ME Requester storage bookkeeping.
6. Update dependencies and strict metadata ranges only after the review. Keep
   signature changes fail-fast in reflection tests and required Mixins; never
   broaden a range to make a build pass.
7. Run unit tests, a meaningful game/integration test where possible, a full
   pack dedicated-server startup, and an unmodified-client connection test on
   a staging copy.
8. Exercise the complete matrix: item/fluid/chemical and custom storage,
   security and I/O restrictions, exact/fuzzy/blank emitters, monitors,
   ExtendedAE, ME Requester, threshold crossings, disappearance, component
   hash collisions, grid split/merge, power/channel loss, chunk reload,
   restart, multiple grids, and terminal use.
9. Repeat controlled before/after Spark profiles with equivalent world,
   players, loaded chunks, duration, and activity. Record median, p95/p99,
   maximum, named hot paths, census burst timing, and cache-driven rebuilds.
10. Update README compatibility, limitations, verification status, AGENTS,
    tests, and release notes. Increase `mod_version`, merge, and create the
    exact matching tag.

If runtime or staging evidence is unavailable, preserve the conservative full
census design and document the missing evidence rather than implementing a
speculative optimization.
