import { uploadFileToApi } from '@/services/api/api';

export const uploadFile = (file: File, token: string) => {
  return uploadFileToApi(file, token);
};