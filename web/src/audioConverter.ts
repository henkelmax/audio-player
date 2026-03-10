import {
    Input,
    Output,
    Conversion,
    ALL_FORMATS,
    BlobSource,
    Mp3OutputFormat,
    canEncodeAudio, BufferTarget
} from 'mediabunny';
import {registerMp3Encoder} from "@mediabunny/mp3-encoder";

export async function convertAudio(inputFile: File): Promise<{ blob: Blob, fileName: string }> {
    if (!(await canEncodeAudio('mp3'))) {
        registerMp3Encoder();
    }

    const input = new Input({
        source: new BlobSource(inputFile),
        formats: ALL_FORMATS,
    });

    const output = new Output({
        format: new Mp3OutputFormat(),
        target: new BufferTarget(),
    });

    const conversion = await Conversion.init({
        input, output, audio: {
            numberOfChannels: 1,
        }
    });

    if (!conversion.isValid) {
        throw new Error("Audio format not supported!")
    }

    await output.start();
    await conversion.execute();
    await output.finalize();

    return {
        blob: new Blob([output.target.buffer!], {type: output.format.mimeType}),
        fileName: inputFile.name.replace(/\.[^/.]+$/, ".mp3")
    };
}
