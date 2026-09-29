# Nocturne — Portfolio Music Player 🎵

A Java music player built on my `MusicPlayer` component, with a desktop UI:
a spinning vinyl record with a tonearm, a clickable waveform seek bar, and a
queue that takes its color from whatever is playing.

## Features

- Play MP3, WAV, AIFF and AU files
- Play / pause, next, previous, seek (click or drag the waveform), volume
- Shuffle (keeps the current song playing) and repeat off / all / one
- Queue: add files or whole folders (button or drag-and-drop), reorder and
  remove from the right-click menu, double-click to play
- Save and open playlists as `.m3u`
- Title and artist read from file names like `Artist - Title.wav`
- Keyboard: `Space` play/pause · `Ctrl+←/→` previous/next ·
  `Shift+←/→` seek 5s · `↑/↓` volume · `Delete` remove selected
- Console versions: `BasicMusicPlayer` and `AdvancedMusicPlayer`

## Running

Requires Java 17+. On Windows, double-click **`run.bat`** (or run it from
PowerShell). The first time, it downloads two optional libraries into `lib/`:

- [FlatLaf](https://www.formdev.com/flatlaf/): modern, crisp dark look
- [mp3spi / JLayer](https://github.com/umjammer/mp3spi): MP3 playback

then builds and opens the player. Without them it still runs, with the
standard look and no MP3.

Manual build (any OS):

```bash
javac -d bin -cp "lib/*" -sourcepath src src/MusicPlayerUI.java
java -cp "bin:lib/*" MusicPlayerUI        # use ; instead of : on Windows
```

## Component

| File | Role |
| --- | --- |
| `MusicPlayerKernel` | Kernel: add/remove songs, play, pause, current track, length |
| `MusicPlayer` | Enhanced: skip, shuffle, adjustOrder, next, previous, songs |
| `MusicPlayerSecondary` | Secondary methods written using only kernel methods |
| `MusicPlayerOnQueue` | Kernel implementation on a queue (front = current track) |

The UI (`MusicPlayerUI`) and audio playback (`AudioTrack`) sit on top of it.
