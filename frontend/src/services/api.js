import axios from 'axios';
import { tokenManager } from '../utils/tokenManager';

const API_BASE_URL = '/api';

// JWT 토큰 헤더 가져오기
const getAuthHeaders = () => {
    const token = tokenManager.getToken();
    return token ? { Authorization: `Bearer ${token}` } : {};
};

export const videoAPI = {
    // 1. 업로드 URL 요청
    initUpload: async (filename, contentType) => {
        const response = await axios.post(
            `${API_BASE_URL}/videos/init-upload`,
            {
                filename,
                contentType,
            },
            {
                headers: getAuthHeaders(),
            }
        );
        return response.data;
    },

    // 2. S3에 파일 업로드
    uploadToS3: async (uploadUrl, file, onProgress) => {
        await axios.put(uploadUrl, file, {
            headers: {
                'Content-Type': file.type,
            },
            onUploadProgress: (progressEvent) => {
                const percentCompleted = Math.round(
                    (progressEvent.loaded * 100) / progressEvent.total
                );
                onProgress && onProgress(percentCompleted);
            },
        });
    },

    // 3. 처리 시작
    processVideo: async (videoId, s3Key, fileSize) => {
        const response = await axios.post(
            `${API_BASE_URL}/videos/${videoId}/process`,
            {
                s3Key,
                fileSize,
            },
            {
                headers: getAuthHeaders(),
            }
        );
        return response.data;
    },

    // 4. 결과 조회
    getResult: async (videoId) => {
        const response = await axios.get(`${API_BASE_URL}/videos/${videoId}`, {
            headers: getAuthHeaders(),
        });
        return response.data;
    },

    // 5. 내 비디오 목록
    getMyVideos: async () => {
        const response = await axios.get(`${API_BASE_URL}/videos/my-videos`, {
            headers: getAuthHeaders(),
        });
        return response.data;
    },

    // 6. Health Check
    healthCheck: async () => {
        const response = await axios.get(`${API_BASE_URL}/videos/health`);
        return response.data;
    },
};