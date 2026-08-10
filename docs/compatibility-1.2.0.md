# All the Mons 1.2.0 compatibility audit

This document records the compatibility evidence for release 0.1.1. The
review used the exact All the Mons 1.2.0 client export and official server
archive; neither archive nor any third-party JAR is redistributed here.

## Pack identity

| Evidence | Identity |
| --- | --- |
| Pack release | All the Mons 1.2.0 |
| Pack source | tag `1.2.0`, commit `c7bb230f21d14d26859d0b92548f089b3a493ad9` |
| Client export | `All the Mons-1.2.0.zip`, SHA-256 `1d37df201daddecf5454115f5205cca15ca6ab84ed102bcc2e312f4c14876e5d` |
| Server archive | `ServerFiles-1.2.0.zip`, SHA-256 `de112ed8d79b3ff027e399a5108b706f6a2db3be74b15d0db6f6b9d6ac268e6c` |
| Minecraft | 1.21.1 |
| NeoForge | 21.1.248 |

The official pack changelog records NeoForge 21.1.234 to 21.1.248 and 75 mod
updates. The cumulative official NeoForge changelog for those versions does
not change AE2's storage API or `StorageService` target. The 21.1.248
changelog is available from the
[NeoForge Maven repository](https://maven.neoforged.net/releases/net/neoforged/neoforge/21.1.248/neoforge-21.1.248-changelog.txt).

## Exact watcher dependencies

| Mod | CurseForge project/file | Version and source | Shipped JAR SHA-256 | Result |
| --- | --- | --- | --- | --- |
| Applied Energistics 2 | `223794` / `7027323` | `19.2.17`; tag `neoforge/v19.2.17`, commit `79ee2c704ad62941a426c26b1cb1f76ef5b2ee5a` | `460d779a0609b81409907d9956de8f6f70a1b0912257e3e5c3c7e75ac9630e95` | Unchanged from pack 1.1.1. |
| ExtendedAE | `892005` / `8511901` | `1.21-2.2.35-neoforge`; tag of the same name, commit `3776bc854458301bbcc9a44a8238d70a0e3dc00d` | `14a2860fa2c747e9dda2279b8933fac6311fecfee166c765171022b902591c65` | Updated from 2.2.33; watcher classes are byte-identical. |
| ME Requester | `688367` / `8285803` | `1.21.1-1.4.3`; tag `v1.21.1-neoforge-1.4.3`, commit `fce75946bbb00c7a4705cb114d1423a284fb112a` | `68f3c861a802d48afeb6e3a48e8ee4f8633904340ac3f89f17493dc84490e385` | Unchanged from pack 1.1.1. |

The exact AE2 JAR and `appeng.me.service.StorageService` class bytes are
unchanged. Its constructor, fields, methods, descriptors, and every required
Mixin injection point therefore remain the exact target reviewed for 0.1.0.
The startup reflection guard and required Mixin injections continue to fail
closed if those assumptions cease to hold.

ExtendedAE's official
[2.2.35 file notes](https://www.curseforge.com/minecraft/mc-mods/ex-pattern-provider/files/8511901)
describe a latest-NeoForge crash fix and localization update. A source diff
from 2.2.33 to 2.2.35 also changes interface throughput and storage-bus
capability invalidation, but not `PartThresholdLevelEmitter`. The main
threshold-emitter class and both anonymous inner classes are byte-for-byte
identical between the shipped 2.2.33 and 2.2.35 JARs. They still use one exact
interest normally, watch-all for a blank/fuzzy filter, and the separate
crafting watcher for crafting-card mode.

A constant-pool scan of every relevant shipped JAR found
`IStorageWatcherNode` registrations only in AE2, ExtendedAE, and ME Requester.
No other reviewed JAR contains or Mixes into
`appeng.me.service.StorageService`.

## Updated adjacent mods

| Mod | Pack change | Inspection result |
| --- | --- | --- |
| AdvancedAE | 1.6.11 to 1.6.12; CurseForge file `8564177` | Top-level class bytecode is unchanged. The embedded library changes recipe helpers only. No storage-watcher API use or `StorageService` Mixin was found. |
| AE2WTLib | 19.5.0 to 19.5.1; CurseForge file `8450398` | Its only changed top-level class corrects payload registration direction. No storage-watcher API use or `StorageService` Mixin was found. |
| ModernFix | 5.27.15 to 5.27.20; CurseForge file `8459650` | No AE2 watcher reference or `StorageService` Mixin was found. The pack newly enables ModernFix's beta stability tier; enabled beta features do not target AE2 storage services. |
| AllTheMons coremod | 0.3.0 to 0.5.0; CurseForge file `8561730` | Its Mixin targets cover other mods and vanilla systems, not AE2 or `StorageService`. |

The pack's `ae2-common.toml` and `advanced_ae-common.toml` are byte-identical
to the 1.1.1 versions. Relevant AE2, ExtendedAE, and AdvancedAE KubeJS recipe
and startup-script directories are also unchanged. The only relevant config
delta is ModernFix's new `stability_level=BETA` setting.

## Scope of verification

The project gate compiles against NeoForge 21.1.248 and AE2 19.2.17, starts
ModDevGradle's server-side test environment, loads the required Mixin and
compatibility guard, runs the JUnit and Checkstyle suites, and verifies the
production JAR contents.

A fresh temporary server from the exact server archive above was then started
with the production 0.1.1 JAR. The full pack discovered the add-on, logged a
successful exact-AE2 compatibility guard, reached `Done`, executed
`atmonsae2 status` successfully, and shut down cleanly. The smoke did not
include a client connection, a production world, the storage-provider and
watcher transition matrix, or a Spark performance comparison.
