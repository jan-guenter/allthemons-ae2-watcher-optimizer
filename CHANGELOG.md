# Changelog

All notable changes to this project are documented here.

## 0.1.0

- Add a configurable, deterministic, per-grid interval for AE2's stock storage
  watcher census.
- Keep AE2's complete-census amount source and watcher callback path intact.
- Keep skipped ticks cache-dirty so ordinary cache consumers can demand a
  stock rebuild.
- Add per-grid census, cache rebuild, watcher, interest, timing, and
  notification diagnostics.
- Add strict AE2 19.2.17 compatibility guards.
