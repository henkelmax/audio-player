<template>
  <main class="h-full w-full">
    <div
      v-if="!isFileUploadProcessDone"
      class="flex flex-col gap-32 justify-center items-center">
      <div class="flex flex-col gap-16 w-full">
        <div class="flex flex-row justify-center items-center text-4xl">
          {{ headline }}
        </div>
        <div
          v-if="!tokenIsTakenFromUrl"
          class="flex flex-row justify-center items-center">
          <input
            v-model:="token"
            class="!rounded-tr-none !rounded-br-none w-80"
            :disabled="isTokenSaved"
            type="text"/>
          <button
            v-if="!isTokenSaved"
            class="!rounded-bl-none !rounded-tl-none"
            type="button"
            :disabled="!isTokenValid"
            @click="saveToken">
            {{ 'Confirm' }}
          </button>
          <button
            v-else
            class="!rounded-bl-none !rounded-tl-none"
            type="button"
            @click="resetToken">
            {{ 'Enter new Token' }}
          </button>
        </div>
      </div>
      <!-- File Input -->
      <div
        v-if="isTokenSaved && !selectedFile"
        class="flex flex-col gap-4 justify-center items-center">
        <div>
          <input
            ref="HiddenFileUpload_Ref"
            @change="onFileInputChange($event)"
            class="hidden"
            accept=".mp3,.wav"
            type="file">
          <button
            type="button"
            @click="triggerFileSelection">
            {{ 'Select your File' }}
          </button>
        </div>
      </div>
      <!-- File sending -->
      <div
        v-if="selectedFile"
        class="flex flex-col justify-center items-center">
        <div class="flex flex-col mb-6 gap-6 justify-center items-center">
          <div class="font-bold">
            {{ 'The following file will be uploaded:' }}
          </div>
          <div class="italic">
            {{ selectedFile.name }}
          </div>
        </div>
        <div>
          <button
            type="button"
            @click="triggerFileUpload">
            {{ 'Start upload' }}
          </button>
        </div>
      </div>
    </div>
    <div v-else>
      <div class=" h-full w-full flex flex-col gap-4 justify-center items-center">
        <span class="text-3xl">{{ finishHeadline }}</span>
        <span class="text-2xl text-center">{{ finishSubText }}</span>
        <button
          v-if="!tokenIsTakenFromUrl"
          type="button"
          @click="resetForm">
          {{ finishButtonText }}
        </button>
      </div>
    </div>
  </main>
</template>


<script setup lang="ts">
  import { computed, onMounted, type Ref, ref } from 'vue';

  import { uploadFile } from '@/services/FileUploadService';

  // Regex for UUID
  const uuidRegex = /^[0-9a-fA-F]{8}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{12}$/;

  const HiddenFileUpload_Ref: Ref<HTMLInputElement | null>= ref(null);

  const token = ref();
  const isTokenSaved = ref(false);
  const tokenIsTakenFromUrl = ref(false);
  const selectedFile: Ref<File | null> = ref(null);

  const finishHeadline = ref('');
  const finishSubText = ref('');
  const finishButtonText = ref('');
  const isFileUploadProcessDone = ref(false);


  const headline = computed((): string => {
    if (!isTokenSaved.value) return 'Enter your token!';
    else if (isTokenSaved.value && !selectedFile.value) return 'Choose a file to upload!';
    else if (selectedFile.value) return 'Now upload your file!';
    else return '';
  });

  const saveToken = (): void => {
    isTokenSaved.value = true;
  };

  const resetToken = (): void => {
    isTokenSaved.value = false;
    token.value = '';
    selectedFile.value = null;
  };


  const isTokenValid = computed((): boolean => {
    return uuidRegex.test(token.value);
  });

  const triggerFileSelection = (): void => {
    if (HiddenFileUpload_Ref.value) HiddenFileUpload_Ref.value?.click();
  };

  const onFileInputChange = (event: Event): void => {
    const target = event.target as HTMLInputElement;

    if (target && target.files) {
      selectedFile.value = target.files[0];
    }
  };

  const triggerFileUpload = async (): Promise<void> => {
    if (selectedFile.value) {
      try {
        const response = await uploadFile(selectedFile.value, token.value);

        if (response.status === 401) {
          finishHeadline.value = 'Unauthorized request!';
          finishSubText.value = 'The file could not be uploaded. Please request a new token in Minecraft and try again.';
          finishButtonText.value = 'Try again!';
        } else if (response.status === 400) {
          finishHeadline.value = 'Upload could not be processed';
          finishSubText.value = 'The file could not be uploaded. Please request a new token in Minecraft and try again.';
          finishButtonText.value = 'Try again!';
        } else if (response.status === 404) {
          finishHeadline.value = 'URL not found';
          finishSubText.value = 'Please request a new token in Minecraft and try again. If the issue persists, please reach out to your server admin.';
          finishButtonText.value = 'Try again!';
        } else if (response.status === 413) {
          finishHeadline.value = 'File too big';
          finishSubText.value = 'Your file was too big, please request a new token in Minecraft and try again with a smaller file.';
          finishButtonText.value = 'Try again!';
        } else if (response.status === 200) {
          finishHeadline.value = 'Sound uploaded successfully!';
          finishSubText.value = 'Your file was uploaded successfully. You can now go back into Minecraft and use it!';
          finishButtonText.value = 'Upload another file!';
        } else {
          finishHeadline.value = 'Unexpected critical error';
          finishSubText.value = 'Please reach out to your server admin.';
          finishButtonText.value = 'Try again';
        }
      } catch (error) {
        finishHeadline.value = 'File too big';
        finishSubText.value = 'Your file was too big, please request a new token in Minecraft and try again with a smaller file.';
        finishButtonText.value = 'Try again!';
      }

      isFileUploadProcessDone.value = true;
    }
  };

  const resetForm = (): void => {
    token.value = '';
    isTokenSaved.value = false;
    selectedFile.value = null;

    finishHeadline.value = '';
    finishSubText.value = '';
    isFileUploadProcessDone.value = false;
  };

  onMounted(() => {
    const urlSearchParams = new URLSearchParams(window.location.search);

    const tokenFromQuery= urlSearchParams.get('token');

    if (tokenFromQuery) {
      const isTokenFromQueryValid = uuidRegex.test(tokenFromQuery);

      if (isTokenFromQueryValid) {
        token.value = tokenFromQuery;
        tokenIsTakenFromUrl.value = true;
        isTokenSaved.value = true;
      }
    }
  });
</script>

<style lang="scss">
input {
  @apply p-2 rounded-lg text-black
}

textarea:focus, input:focus{
  outline: none;
}

button {
  @apply bg-green-800 p-2 rounded-lg
}

button:hover:active {
  @apply bg-green-900
}

button:disabled {
  @apply opacity-75 cursor-not-allowed
}
</style>

