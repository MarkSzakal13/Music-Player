# Nocturne — Portfolio Music Player 🎵

A Java music player built on my `MusicPlayer` component, with a desktop UI:
a spinning vinyl record with a tonearm, a clickable waveform seek bar, and a
queue that takes its color from whatever is playing.

## Features

- Play WAV, AIFF and AU files (Java's built-in audio; no extra libraries)
- Play / pause, next, previous, seek (click or drag the waveform), volume
- Shuffle (keeps the current song playing) and repeat off / all / one
- Queue: add files or whole folders (button or drag-and-drop), reorder and
  remove from the right-click menu, double-click to play
- Save and open playlists as `.m3u`
- Title and artist read from file names like `Artist - Title.wav`
- Keyboard: `Space` play/pause · `Ctrl+←/→` previous/next ·
  `Shift+←/→` seek 5s · `↑/↓` volume · `Delete` remove selected
- Console versions: `BasicMusicPlayer` and `AdvancedMusicPlayer`

> MP3 isn't supported by Java's built-in audio. Convert MP3s to WAV (e.g.
> with Audacity or VLC) to play them.

## Running

Requires Java 17+. From the repository root:

```bash
javac -d bin -sourcepath src src/MusicPlayerUI.java
java -cp bin MusicPlayerUI
```

You can also pass files or folders to queue on startup:
`java -cp bin MusicPlayerUI "C:\Users\me\Music"`.

## Component

| File | Role |
| --- | --- |
| `MusicPlayerKernel` | Kernel: add/remove songs, play, pause, current track, length |
| `MusicPlayer` | Enhanced: skip, shuffle, adjustOrder, next, previous, songs |
| `MusicPlayerSecondary` | Secondary methods written using only kernel methods |
| `MusicPlayerOnQueue` | Kernel implementation on a queue (front = current track) |

The UI (`MusicPlayerUI`) and audio playback (`AudioTrack`) sit on top of it.
