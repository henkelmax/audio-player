import type {FC, ReactNode} from "react";

export const Button: FC<{ onClick: () => void, children: ReactNode }> = ({onClick, children}) => (
    <button
        onClick={onClick}
        type="button"
        className="bg-green-800 p-2 rounded-lg hover:bg-green-900 disabled:opacity-75 focus:outline-none cursor-pointer"
    >
        {children}
    </button>
);