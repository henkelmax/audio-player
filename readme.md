<!-- modrinth_exclude.start -->

# AudioPlayer

## Links

- [CurseForge](https://www.curseforge.com/minecraft/mc-mods/audioplayer)
- [Modrinth](https://modrinth.com/mod/audioplayer)
- [Simple Voice Chat Discord](https://discord.gg/4dH2zwTmyX)

---

<!-- modrinth_exclude.end -->

This server side Fabric mod enables uploading custom audio for music discs, goat horns and note blocks with heads.

This mod requires [Simple Voice Chat](https://www.curseforge.com/minecraft/mc-mods/simple-voice-chat) on the client and server.

> [!IMPORTANT]  
> This documentation is for version 2.x.x of the mod only!

## Features

- On the fly audio uploading without needing to restart the server
- Support for `mp3` and `wav`
- Upload audio via a URL
- Upload audio directly to your server
- Upload audio via [Filebin](https://github.com/espebra/filebin2/)
- Upload using a website
- Server side only
- No server restart needed
- No resource pack needed
- No changes needed on the client
- Configurable upload limit
- Configurable command permissions
- Configurable audio range
- Per-item custom audio range
- Bulk applying audio to all items in shulker boxes, bundles and other container items
- Configurable goat horn cooldown

## Commands

Run `/audioplayer` to get general information on how to upload files.

### Uploading audio files via URL

Run `/audioplayer url "https://example.com/myaudio.mp3"` where `https://example.com/myaudio.mp3` is the link to your `.mp3` or `.wav` file.

### Uploading audio files directly to the server

Copy your `.mp3` or `.wav` file to the `audioplayer_uploads` folder in your server.
Run `/audioplayer serverfile "yourfile.mp3"` where `yourfile.mp3` is the name of the file you put on the server.

### Uploading audio files via Filebin

Run `/audioplayer filebin` and follow the instructions.

### Putting custom audio on an item

Run `/audioplayer apply <ID>` and hold a **music disc**, **goat horn** or **head** in your main hand.

It's also possible to bulk apply audio to more than one item at a time by holding a shulker box, bundle or other container item in your hand.

You can also apply custom audio by its original file name:
`/audioplayer apply "<FILE_NAME>"`.
This command works with and without the file extension (like `.mp3` or `.wav`).
Note that the file name must be unique for this to work.

### Adjusting the range of an item

Run `/audioplayer range <RANGE>` while holding an item or container with items that have custom audio in your main hand.

### Getting the audio info of an existing item

Run `/audioplayer info` while holding a music disc, goat horn or head with custom audio in your main hand.

### Searching audio files by name

Run `/audioplayer search <SEARCH_TEXT>` to get all audio files that contain the search text.


---
[![](https://user-images.githubusercontent.com/13237524/179395180-05f2ec3b-2ed3-412d-8639-72c7f13a8068.png)](https://youtu.be/j8GRcYnjUp8)

[![](https://user-images.githubusercontent.com/13237524/179395233-582b70bc-f308-47c7-96ff-541257e86545.png)](https://youtu.be/tixidvB4Zko)

![](https://user-images.githubusercontent.com/13237524/179395296-be3643eb-1c23-4300-ac17-25d11d53d6f3.png)

![](https://user-images.githubusercontent.com/13237524/143213769-99a6b03a-887a-4b30-8b18-baf394be6b6c.png)

## Credits

- [MP3SPI](https://github.com/umjammer/mp3spi)
- [Simple Voice Chat](https://github.com/henkelmax/simple-voice-chat)
- [Admiral](https://github.com/henkelmax/admiral)

*Note that the files you upload to Filebin are publicly available if the upload link is disclosed!*
