# Changelog

All notable changes to this project are documented here.

## 0.1.1

- Validate compatibility with All the Mons 1.2.0 and NeoForge 21.1.248.
- Update the optional ExtendedAE dependency and startup guard to the reviewed
  `1.21-2.2.35-neoforge` release.
- Confirm that the shipped AE2 19.2.17 and ME Requester 1.4.3 artifacts are
  unchanged, including AE2's patched `StorageService` bytecode.
- Audit the updated ExtendedAE, AdvancedAE, AE2WTLib, ModernFix, and All the
  Mons coremod artifacts for watcher API use and competing `StorageService`
  Mixins.
- Pass a fresh-world startup smoke with the exact All the Mons 1.2.0 server
  archive, including the compatibility guard and diagnostics command.

## 0.1.0

- Add a configurable, deterministic, per-grid interval for AE2's stock storage
  watcher census.
- Keep AE2's complete-census amount source and watcher callback path intact.
- Keep skipped ticks cache-dirty so ordinary cache consumers can demand a
  stock rebuild.
- Add per-grid census, cache rebuild, watcher, interest, timing, and
  notification diagnostics.
- Add strict AE2 19.2.17 compatibility guards.
