import {type FC} from "react";
import type {UploadFileResponse} from "./api.ts";
import {Button} from "./Button.tsx";

export const UploadResult: FC<{ response: UploadFileResponse }> = ({response}) => {
    return (
        <main className="h-full w-full">
            <div className="flex flex-col gap-32 justify-center items-center">
                <div className="flex flex-col gap-16 w-full">
                    <div className="flex flex-row justify-center items-center text-4xl">
                        {response.headline}
                    </div>
                    <div className="flex flex-row justify-center items-center text-xl text-center">
                        {response.subText}
                    </div>
                    <div className="flex flex-row justify-center items-center">
                        <Button onClick={() => window.location.href = "/"}>
                            {response.buttonText}
                        </Button>
                    </div>
                </div>
            </div>
        </main>
    );
}