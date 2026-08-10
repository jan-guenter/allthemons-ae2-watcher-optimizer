# All the Mons AE2 Watcher Optimizer

A dedicated-server-only NeoForge add-on for All the Mons 1.2.0. It reduces
the average cost of Applied Energistics 2 storage watchers by running AE2's
unchanged full storage census at a configurable, deterministic interval for
each grid instead of every tick.

No client installation is intended. The add-on registers no content, menus,
payloads, packet channels, screens, recipes, or client hooks. It stores no
required state in worlds, grids, block entities, or items.

## Compatibility

| Component | Supported version |
| --- | --- |
| All the Mons | exactly 1.2.0 |
| Minecraft | 1.21.1 |
| NeoForge | 21.1.248 through the 21.1 line |
| Applied Energistics 2 | exactly 19.2.17 |
| ExtendedAE, when installed | exactly 1.21-2.2.35-neoforge |
| ME Requester, when installed | exactly 1.21.1-1.4.3 |
| Java | 21 |

The dependency metadata and a startup reflection guard intentionally reject
unreviewed versions. Required Mixins also require every inspected target and
injection point to exist.

## What it changes

AE2 19.2.17 normally calls its private `StorageService.updateCachedStacks()`
every server tick whenever any storage watcher has an exact interest or a
watch-all interest. That method enumerates every available key and dispatches
changes through AE2's watcher callbacks.

This add-on changes only that scheduling decision:

1. Every live storage service has its own tick counter and deterministic
   phase.
2. On a due tick, AE2 executes its original
   `updateCachedStacks()` implementation.
3. On a skipped tick, the add-on marks AE2's complete inventory cache dirty.
4. If a terminal, emitter reconfiguration, or another stock cache consumer
   asks for the cache before the next due tick, AE2 immediately performs its
   original rebuild. That rebuild is recorded as `cacheDriven`.
5. AE2 remains responsible for computing amounts, comparing its private
   amount cache, and invoking `IStorageWatcherNode.onStackChange`.

Setting `fullCensusIntervalTicks = 1` or
`intervalThrottlingEnabled = false` restores stock every-tick scheduling.

The grid phase uses AE2's runtime `Grid #<serial>` plus the first resolvable
node dimension. This identity is stable only for that live grid in that server
process. It is not stable across restarts and must not be treated as a durable
grid ID.

## Why exact-interest polling is not implemented

Exact-interest polling is deliberately unsupported in the 0.1.x line. AE2
19.2.17 offers:

- complete availability enumeration through
  `MEStorage.getAvailableStacks(KeyCounter)`; and
- exact-key simulated extraction through `MEStorage.extract(...)`.

It does not offer a side-effect-free exact-key query with the same
*availability* semantics as the full census. Simulated extractability can
differ from availability for security-restricted, insert-only,
non-extractable, or custom storage. ME Requester also initializes its known
amount from complete availability. Using simulated extraction would therefore
be a semantic guess, so this release keeps the exact AE2 census as its only
amount source.

## Installation

1. Stop the dedicated server and make a normal backup.
2. Put the production JAR in the server's `mods` directory.
3. Do not add it to client instances.
4. Start the server once to generate
   `world/serverconfig/atmons_ae2_optimizer-server.toml`.
5. Confirm startup reports that the AE2 19.2.17 guard passed.
6. Run `/atmonsae2 status` from the console or as an operator.

The default interval is intentionally a staging value. Validate behavior and
tick profiles on a copy before using it on a production world.

## Configuration

The server config is
`world/serverconfig/atmons_ae2_optimizer-server.toml`.

| Key | Default | Meaning |
| --- | ---: | --- |
| `watcherCensus.intervalThrottlingEnabled` | `true` | Enables interval scheduling. `false` restores stock scheduling. |
| `watcherCensus.fullCensusIntervalTicks` | `10` | Full census period. `1` restores stock scheduling. |
| `watcherCensus.staggerGrids` | `true` | Gives grids different deterministic phases within a process. |
| `diagnostics.diagnosticsEnabled` | `true` | Periodically logs per-grid metrics; command counters remain available when false. |
| `diagnostics.diagnosticsLogIntervalTicks` | `1200` | Per-grid log period; 1200 ticks is about 60 seconds at 20 TPS. |
| `diagnostics.warnOnWatchAll` | `true` | Warns when a fuzzy, blank, or other watch-all watcher first appears. |

The predictable worst-case delay is up to the configured census interval. At
20 TPS, 10 ticks is about 500 ms. A rough stock-keeping overshoot bound is:

```text
production rate per tick × fullCensusIntervalTicks
```

Changes that occur and reverse entirely between two censuses can be coalesced
and may never be reported as an intermediate state. This is the inherent
trade-off of interval census mode. Test fast farms, autocrafting, and requester
setpoints before increasing the interval.

## Diagnostics

`/atmonsae2 status` requires permission level 2. It reports each live grid's:

- runtime grid label and first resolved dimension;
- registered storage-watcher host count;
- number of unique exact keys;
- number of watch-all watchers, including blank/fuzzy emitters;
- active interval and phase;
- scheduled censuses and skipped census opportunities;
- cache-driven full rebuilds requested by ordinary AE2 cache consumers;
- changed-key comparisons and callback dispatch attempts; and
- average, maximum, and last full-refresh duration.

`/atmonsae2 reset` requires permission level 4 and clears counters without
changing scheduling or grid state. Metrics are memory-only and disappear on
restart.

The diagnostics classify interests, but they never replace AE2's amount
source or callbacks. There is no exact-key polling time because exact polling
is not implemented.

## Known watcher behavior in the inspected pack

The exact source and shipped JARs were inspected:

- AE2 `StorageLevelEmitterPart` registers one exact key normally and switches
  to watch-all for a blank filter or fuzzy card.
- AE2 `AbstractMonitorPart` registers its configured exact key.
- ExtendedAE `PartThresholdLevelEmitter` registers one exact key normally and
  switches to watch-all for a blank filter or fuzzy card.
- ME Requester `StorageManager` registers every configured request key as an
  exact interest.

All of those watcher types continue to use the stock full census and stock
callback path; they share the configured delay.

The All the Mons 1.2.0 update audit, exact artifact identities, hashes, source
commits, and inspected change surface are recorded in
[`docs/compatibility-1.2.0.md`](docs/compatibility-1.2.0.md).

## Rollout and rollback

For rollout:

1. Capture a comparable baseline Spark profile on a staging copy.
2. Start with interval 1 to establish that the add-on's stock mode behaves
   normally.
3. Test intervals 5, 10, and 20 while exercising exact and fuzzy item/fluid
   emitters, monitors, ExtendedAE threshold emitters, and ME Requester.
4. Exercise upward and downward threshold crossings, grid split/merge,
   power/channel loss, chunk unload/reload, terminals, and restart.
5. Compare median, p95/p99, maximum tick time, census bursts, and
   cache-driven rebuild counts under equivalent activity.
6. Select the smallest interval that provides sufficient savings.

For rollback, stop the server and remove the add-on JAR. No world conversion
or data cleanup is required. Alternatively, set the interval to 1 or disable
interval throttling to restore stock scheduling while retaining diagnostics.

## Verification status

Automated verification includes deterministic scheduling tests, diagnostics
counter tests, an AE2 signature test, Checkstyle, a clean build, and a
production-JAR content check. The JAR check rejects third-party classes and
unexpected resources. Eleven JUnit tests pass through ModDevGradle's
server-side unit-test launcher; that launcher loads NeoForge, AE2, GuideME,
this mod, its required Mixin configuration, and the startup compatibility
guard.

The ModDevGradle unit-test launch is not a full dedicated-server startup and
is not presented as one.

A fresh-world dedicated-server smoke test was also run with the exact All the
Mons 1.2.0 server archive. The server loaded this 0.1.1 JAR, passed the startup
compatibility guard, reached `Done`, returned a valid empty-grid response to
`/atmonsae2 status`, and shut down cleanly.

The following environment-dependent checks are still pending and are not
claimed by this project:

- unmodified-client connection;
- production-world compatibility;
- the complete storage-provider/watcher transition matrix; and
- before/after Spark performance results.

## Building

Use Java 21:

```bash
./gradlew --no-daemon clean check build
```

The production JAR is written to `build/libs`. A sources JAR is also produced
for Maven consumers but is not a server mod artifact.

## Releases and Maven

The semantic version is `mod_version` in `gradle.properties`. Release tags
must be exactly `v<mod_version>`. The release workflow repeats all checks,
publishes the `mod` Maven publication to GitHub Packages, creates SHA-256
checksums, and attaches only the production JAR and checksum file to the
GitHub Release.

Maven coordinates:

```text
io.github.jan-guenter:allthemons-ae2-watcher-optimizer:0.1.1
```

GitHub Packages repository:

```text
https://maven.pkg.github.com/jan-guenter/allthemons-ae2-watcher-optimizer
```

GitHub Packages consumers may need GitHub credentials even for public
packages.

## License

The add-on's original code is licensed under the MIT License. Applied
Energistics 2, ExtendedAE, and ME Requester are separate projects under their
own licenses and are not redistributed here.
