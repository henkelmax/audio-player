import {type ChangeEvent, type DragEvent, type FC, useRef, useState} from "react"
import {uploadAudio, type UploadFileResponse} from "./api.ts"
import {Button} from "./Button.tsx"

const SelectFile: FC<{ setFile: (f: File | null) => void }> = ({setFile}) => {
    const inputRef = useRef<HTMLInputElement>(null)
    const handleFileUpload = (e: ChangeEvent<HTMLInputElement>) => {
        const selectedFile = e.target.files?.[0]
        if (selectedFile) {
            setFile(selectedFile)
        }
    }

    return (
        <div className="flex flex-col gap-4 justify-center items-center">
            <div>
                <input
                    ref={inputRef}
                    onChange={handleFileUpload}
                    className="hidden"
                    accept="audio/*"
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

const HoverOverlay = () => {
    return (
        <div
            className="absolute inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm border-4 border-dashed border-blue-500 rounded-xl m-4 pointer-events-none">
        <span className="text-white text-3xl font-bold pointer-events-none">
            Drop your audio file here
        </span>
        </div>
    )
}

const HoverOverlayInvalid = () => {
    return (
        <div
            className="absolute inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm border-4 border-dashed border-red-500 rounded-xl m-4 pointer-events-none">
        <span className="text-white text-3xl font-bold pointer-events-none">
            Invalid file type!
        </span>
        </div>
    )
}

function isValidAudioMimeType(mimeType: string | null | undefined) {
    return mimeType && mimeType.startsWith("audio/")
}

export const UploadAudio:
    FC<{ token: string, setResponse: (response: UploadFileResponse) => void }>
    = ({token, setResponse}) => {
    const [file, setFile] = useState<File | null>(null)
    // true = valid, false = invalid, null = no overlay
    const [hoverType, setHoverType] = useState<boolean | null>(null);

    const handleDragOver = (e: DragEvent<HTMLElement>) => {
        e.preventDefault();
        const items = e.dataTransfer.items
        if (items && items.length > 0) {
            const item = items[0]
            if (isValidAudioMimeType(item.type)) {
                setHoverType(true)
                return
            }
        }
        setHoverType(false)
    };

    const handleDragLeave = (e: DragEvent<HTMLElement>) => {
        e.preventDefault();
        setHoverType(null);
    };

    const handleDrop = (e: DragEvent<HTMLElement>) => {
        e.preventDefault();
        setHoverType(null);

        const droppedFiles = e.dataTransfer.files;
        if (droppedFiles && droppedFiles.length > 0) {
            const type = droppedFiles[0].type;
            if (isValidAudioMimeType(type)) {
                setFile(droppedFiles[0]);
            }
        }
    };

    return (
        <main
            className="h-full w-full p-8"
            onDragOver={handleDragOver}
            onDragLeave={handleDragLeave}
            onDrop={handleDrop}
        >
            <div className="flex flex-col gap-32 justify-center items-center">
                <div className="flex flex-col gap-16 w-full">
                    <div className="flex flex-row justify-center items-center text-4xl">
                        Choose a file to upload!
                    </div>
                </div>
                {file ? <UploadFile token={token} file={file} setResponse={setResponse}/> :
                    <SelectFile setFile={setFile}/>}
            </div>
            {hoverType === true && <HoverOverlay/>}
            {hoverType === false && <HoverOverlayInvalid/>}
        </main>
    )
}
