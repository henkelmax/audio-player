export const BASE_URL = import.meta.env.DEV ? 'http://localhost:8080' : ''

export const UUID_REGEX = /^[0-9a-fA-F]{8}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{12}$/

export interface UploadFileResponse {
    success: boolean
    headline: string
    subText: string
    buttonText: string
}

export const uploadAudio = async (file: File, token: string): Promise<UploadFileResponse> => {
    try {
        const response = await fetch(`${BASE_URL}/upload`, {
            method: 'POST',
            headers: new Headers({
                'token': token
            }),
            body: file
        })
        if (response.status === 401) {
            return {
                success: false,
                headline: 'Unauthorized request!',
                subText: 'The file could not be uploaded. Please request a new token in Minecraft and try again.',
                buttonText: 'Try again!'
            }
        } else if (response.status === 400) {
            return {
                success: false,
                headline: 'Upload could not be processed!',
                subText: 'The file could not be uploaded. Please request a new token in Minecraft and try again.',
                buttonText: 'Try again!'
            }
        } else if (response.status === 404) {
            return {
                success: false,
                headline: 'URL not found!',
                subText: 'Please request a new token in Minecraft and try again. If the issue persists, please reach out to your server admin.',
                buttonText: 'Try again!'
            }
        } else if (response.status === 413) {
            return {
                success: false,
                headline: 'File too big!',
                subText: 'Your file was too big, please request a new token in Minecraft and try again with a smaller file.',
                buttonText: 'Try again!'
            }
        } else if (response.status === 200) {
            return {
                success: true,
                headline: 'Audio uploaded successfully!',
                subText: 'Your file was uploaded successfully. You can now go back into Minecraft and use it!',
                buttonText: 'Upload another file!'
            }
        } else {
            return {
                success: false,
                headline: 'Unexpected critical error!',
                subText: 'Please reach out to your server admin.',
                buttonText: 'Try again!'
            }
        }
    } catch (error) {
        return {
            success: false,
            headline: 'File too big!',
            subText: 'Your file was too big, please request a new token in Minecraft and try again with a smaller file.',
            buttonText: 'Try again!'
        }
    }

}
