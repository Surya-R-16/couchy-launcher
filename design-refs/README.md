# Design references

Screenshots of this launcher, used as the "before" for the visual refresh.

Naming: `NN-before-<surface>.png` and `NN-after-<surface>.png`, numbered in the
order the surfaces are being reworked. Same surface, same wallpaper, same state
— so the two can be put side by side and judged.

| file | what it shows |
|---|---|
| `00-before-home.png` | Single row layout, top-left RAM/storage readout, 6 pinned apps, focus on the first icon |
| `01-before-settings.png` | The settings sheet (Main screen) |
| `02-after-home.png` | Same surface after the dock and status-bar pass: the dock floats clear of the bezel (32 dp bottom margin), both status chips share one glass material and one inset, focus ring on the first icon. The wallpaper differs from `00-before-home.png` — the slideshow rotates every minute and the two shots were taken days apart. |

## Ground rules for a reference

- The **before** must be a real screenshot of the real device, not a mock-up.
- The **after** must be the same screen, same state, same wallpaper.
- One surface per pair. A montage of everything is not a reference.

## Direction references (not yet here)

The brief for the refresh is "make it look modern without breaking what is now
fast". A picture of a look we want beats a paragraph describing it — the Google
TV home screen and the tvOS home screen are the obvious candidates.

Those images are not in the repo yet: they are third-party screenshots, and
they should be fetched (or supplied by the owner) rather than approximated.
