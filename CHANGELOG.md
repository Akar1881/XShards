# Xshards Changelog

## [1.2.8] - 2026-04-08

### Fixed

- Fixed critical bug where players could take items from the store (`/store`) for free without purchasing them. The shop inventory click protection was not being triggered due to a title mismatch caused by color codes, allowing players to freely drag items out of the GUI.
