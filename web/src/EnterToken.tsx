import {type FC, useMemo, useRef, useState} from "react";
import {UUID_REGEX} from "./api.ts";

const EnterToken: FC<{ setToken: (t: string | null) => void }> = ({setToken}) => {
    const inputRef = useRef<HTMLInputElement>(null);
    const [tokenText, setTokenText] = useState<string | null>(null);
    const valid = useMemo(() => tokenText !== null && UUID_REGEX.test(tokenText), [tokenText]);
    return (
        <main className="h-full w-full">
            <div className="flex flex-col gap-32 justify-center items-center">
                <div className="flex flex-col gap-16 w-full">
                    <div className="flex flex-row justify-center items-center text-4xl">
                        Enter your token!
                    </div>
                    <div className="flex flex-row justify-center items-center">
                        <input
                            ref={inputRef}
                            onChange={event => setTokenText(event.target.value)}
                            onKeyDown={(e) => {
                                if (e.key === "Enter") setToken(inputRef.current?.value ?? null)
                            }}
                            className="p-2 rounded-lg rounded-tr-none! rounded-br-none! w-80 text-black bg-white focus:outline-none"
                            type="text"
                            placeholder="Enter token"
                        />
                        <button
                            onClick={() => setToken(inputRef.current?.value ?? null)}
                            disabled={!valid}
                            className="bg-green-800 p-2 rounded-lg rounded-bl-none! rounded-tl-none! hover:enabled:bg-green-900 disabled:opacity-75 focus:outline-none disabled:cursor-not-allowed cursor-pointer"
                            type="button"
                        >
                            Confirm
                        </button>
                    </div>
                </div>
            </div>
        </main>
    );
}

export default EnterToken;