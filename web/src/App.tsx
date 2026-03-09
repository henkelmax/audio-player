import EnterToken from "./EnterToken.tsx"
import {useState} from "react"
import {UploadAudio} from "./UploadAudio.tsx"
import {type UploadFileResponse, UUID_REGEX} from "./api.ts"
import {UploadResult} from "./UploadResult.tsx"

function App() {
    const params = new URLSearchParams(window.location.search)
    const [token, setToken] = useState(params.get("token"))
    const [response, setResponse] = useState<UploadFileResponse | null>(null)
    if(response){
        return <UploadResult response={response}/>
    }
    if (!token || !UUID_REGEX.test(token)) {
        return <EnterToken setToken={setToken}/>
    }
    return <UploadAudio token={token} setResponse={setResponse}/>
}

export default App
