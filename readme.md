<!-- modrinth_exclude.start -->

# AudioPlayer

## Links
- [CurseForge](https://www.curseforge.com/minecraft/mc-mods/audioplayer)
- [Modrinth](https://modrinth.com/mod/audioplayer)
- [Simple Voice Chat Discord](https://discord.gg/4dH2zwTmyX)

---

<!-- modrinth_exclude.end -->

This server side Fabric mod enables uploading custom audio for music discs and goat horns.

This mod requires [Simple Voice Chat](https://www.curseforge.com/minecraft/mc-mods/simple-voice-chat) on the client and server.

## Features

- On the fly audio uploading without needing to restart the server
- Support for `mp3` and `wav`
- Upload audio via a URL
- Upload audio directly to your server
- Upload audio via [Filebin](https://github.com/espebra/filebin2/)
- Server side only
- No server restart needed
- No resource pack needed
- No changes needed on the client
- Configurable upload limit
- Configurable command permissions
- Configurable audio range
- Per-item custom audio range
- Bulk applying audio to all items in a shulker box
- Configurable goat horn cooldown

## Commands

Run `/audioplayer` to get general information on how to upload files.

**Uploading audio files via URL**

Run `/audioplayer url "https://example.com/myaudio.mp3"` where `https://example.com/myaudio.mp3` is the link to your `.mp3` or `.wav` file.

**Uploading audio files directly to the server**

Copy your `.mp3` or `.wav` file to the `audioplayer_uploads` folder in your server.
Run `/audioplayer serverfile "yourfile.mp3"` where `yourfile.mp3` is the name of the file you put on the server.

**Uploading audio files via Filebin**

Run `/audioplayer filebin` and follow the instructions.

**Putting custom audio on a music disc or goat horn**

Run `/audioplayer apply <ID>` and hold a **music disc** or **goat horn** in your main hand.
Additionally, you can add a custom name and range to the item `/audioplayer apply <ID> "<CUSTOM-TEXT>" <RANGE>`.

**Getting the audio from an existing item**

Run `/audioplayer id` and hold a music disc or a goat horn with custom audio in your main hand.

---
[![](https://user-images.githubusercontent.com/13237524/179395180-05f2ec3b-2ed3-412d-8639-72c7f13a8068.png)](https://youtu.be/j8GRcYnjUp8)

[![](https://user-images.githubusercontent.com/13237524/179395233-582b70bc-f308-47c7-96ff-541257e86545.png)](https://youtu.be/tixidvB4Zko)

![](https://user-images.githubusercontent.com/13237524/179395296-be3643eb-1c23-4300-ac17-25d11d53d6f3.png)

![](https://user-images.githubusercontent.com/13237524/142997959-9120d038-4ee6-45bb-8815-2179884ef958.png)

![](https://user-images.githubusercontent.com/13237524/143213769-99a6b03a-887a-4b30-8b18-baf394be6b6c.png)

## Credits

- [MP3SPI](https://github.com/umjammer/mp3spi)
- [Simple Voice Chat](https://github.com/henkelmax/simple-voice-chat)
- [Admiral](https://github.com/henkelmax/admiral)

*Note that the files you upload to Filebin are publicly available if the upload link is disclosed!*