const BASEURL = import.meta.env.DEV ? 'http://localhost:8080' : '';

export const uploadFileToApi = (file: File, token: string): Promise<Response> => {
  const response = fetch(`${BASEURL}/upload`, {
    method: 'POST',
    headers: new Headers({
      'token': token
    }),
    body: file
  });

  return response;
};
