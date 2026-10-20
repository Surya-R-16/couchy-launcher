# Reference: Plume Launcher

Screenshots from [aradhyas8/Plume_Launcher](https://github.com/aradhyas8/Plume_Launcher)
(GPL-3.0, © the Plume authors), used as the **visual direction** for our UI refresh.

| file | what it shows |
|---|---|
| `home-dark.jpg` | Dark theme — big clock + date top-left, icon cluster top-right, app tiles, "Continue Watching" row with progress bars |
| `home-light.jpg` | The same screen in the light theme |

Copied from that repo's `docs/` folder. GPL-3.0 permits redistribution with
attribution; the licence and source are noted here.

## What we are taking from it

- **A large clock and date, top-left** — we have a small clock top-right.
- **Monochrome app logos on dark rounded tiles** — reads as a set, not a pile.
- **"Continue Watching"** — the most useful thing a TV home screen can show,
  and it comes from the standard Android TV *Watch Next* API.
- **HDMI inputs shown as app tiles** — see the note in `NOTES.local.md`: the
  platform API for switching to a passthrough input is
  `TvContract.buildChannelUriForPassthroughInput(inputId)`, fired with
  `ACTION_VIEW`. We spent a session chasing the vendor's intent instead.
