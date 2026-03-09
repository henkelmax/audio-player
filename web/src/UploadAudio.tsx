import {type ChangeEvent, type FC, useRef, useState} from "react";
import {uploadAudio, type UploadFileResponse} from "./api.ts";
import {Button} from "./Button.tsx";

const SelectFile: FC<{ setFile: (f: File | null) => void }> = ({setFile}) => {
    const inputRef = useRef<HTMLInputElement>(null);
    const handleFileUpload = (e: ChangeEvent<HTMLInputElement>) => {
        const selectedFile = e.target.files?.[0];
        if (selectedFile) {
            setFile(selectedFile);
        }
    };

    return (
        <div className="flex flex-col gap-4 justify-center items-center">
            <div>
                <input
                    ref={inputRef}
                    onChange={handleFileUpload}
                    className="hidden"
                    accept=".mp3,.wav"
                    type="file"
                />
                <Button onClick={() => inputRef.current?.click()}>
                    Select your File
                </Button>
            </div>
        </div>
    )
}

const UploadFile:
    FC<{ token: string, file: File, setResponse: (r: UploadFileResponse) => void }>
    = ({token, file, setResponse}) => {
    return (
        <div className="flex flex-col justify-center items-center">
            <div className="flex flex-col mb-6 gap-6 justify-center items-center">
                <div className="font-bold">
                    The following file will be uploaded:
                </div>
                <div className="italic">
                    {file?.name}
                </div>
            </div>
            <div>
                <button
                    onClick={async () => setResponse(await uploadAudio(file, token))}
                    type="button"
                    className="bg-green-800 p-2 rounded-lg hover:active:bg-green-900 disabled:opacity-75 cursor-pointer"
                >
                    Start upload
                </button>
            </div>
        </div>
    )
}

export const UploadAudio:
    FC<{ token: string, setResponse: (response: UploadFileResponse) => void }>
    = ({token, setResponse}) => {
    const [file, setFile] = useState<File | null>(null);

    return (
        <main className="h-full w-full">
            <div className="flex flex-col gap-32 justify-center items-center">
                <div className="flex flex-col gap-16 w-full">
                    <div className="flex flex-row justify-center items-center text-4xl">
                        Choose a file to upload!
                    </div>
                </div>
                {file ? <UploadFile token={token} file={file} setResponse={setResponse}/> :
                    <SelectFile setFile={setFile}/>}
            </div>
        </main>
    );
}
