- Removed file name restrictions for serverfile import
- Added suggestions for serverfile command
- Save import date of audio files
- Save owner of audio files
- Allow bulk applying to bundles and other container items
- Cleaned up commands
- Removed randomized sounds
- Removed static sounds
- Added audio player API
- Change versioning to <modversion>+<minecraftversion>
- Removed musicdisc and goathorn commands
- Improved threading
- Added info command
- Added separate range command
- Improve memory usage for cached audio files
- Improve playback performance for cached items
- Added`max_import_duration` config option
- Added search command
- Removed name command
- Renamed `cache_size` config option to `audio_cache_size` and increased default value to 128
- Added translations with fallback
- Added `audio_loader_threads` config option
- Allow disabling audio type duration limits


**WARNING**

Version 2.x.x is not compatible with version 1.x.x!

Your old data gets partially migrated and might work, but it is strongly discouraged to use version 2.x.x
with worlds created with version 1.x.x as it may cause issues down the line with items that contain old audio data.
