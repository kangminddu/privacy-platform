import axios from 'axios';
import { tokenManager } from '../utils/tokenManager';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api';

// ✅ axios 인스턴스 생성
const apiClient = axios.create({
    baseURL: API_BASE_URL,
});

// ✅ 요청 인터셉터 - 토큰 자동 추가
apiClient.interceptors.request.use(
    (config) => {
        const token = tokenManager.getToken();
        if (token) {
            config.headers.Authorization = `Bearer ${token}`;
        }
        return config;
    },
    (error) => Promise.reject(error)
);

// ✅ 응답 인터셉터 - 401 에러 시 자동 갱신
apiClient.interceptors.response.use(
    (response) => response,
    async (error) => {
        const originalRequest = error.config;

        if (error.response?.status === 401 && !originalRequest._retry) {
            originalRequest._retry = true;

            const refreshToken = tokenManager.getRefreshToken();
            if (refreshToken) {
                try {
                    console.log('🔄 토큰 갱신 시도...');
                    const response = await axios.post(`${API_BASE_URL}/auth/refresh`, {
                        refreshToken,
                    });

                    const { accessToken, refreshToken: newRefreshToken } = response.data;
                    tokenManager.saveToken(accessToken, newRefreshToken);

                    console.log('✅ 토큰 갱신 성공!');

                    originalRequest.headers.Authorization = `Bearer ${accessToken}`;
                    return apiClient(originalRequest);
                } catch (refreshError) {
                    console.log('❌ 토큰 갱신 실패 - 로그아웃');
                    tokenManager.clearToken();
                    window.location.href = '/login';
                    return Promise.reject(refreshError);
                }
            } else {
                tokenManager.clearToken();
                window.location.href = '/login';
            }
        }

        return Promise.reject(error);
    }
);

// ========== 인증 API ==========
export const authAPI = {
    sendVerificationCode: async (email) => {
        const response = await axios.post(
            `${API_BASE_URL}/auth/send-code`,
            { email }
        );
        return response.data;
    },

    verifyCode: async (email, code) => {
        const response = await axios.post(
            `${API_BASE_URL}/auth/verify-code`,
            { email, code }
        );
        return response.data;
    },

    register: async (email, password, username) => {
        const response = await axios.post(
            `${API_BASE_URL}/auth/register`,
            { email, password, username }
        );
        return response.data;
    },

    login: async (email, password) => {
        const response = await axios.post(
            `${API_BASE_URL}/auth/login`,
            { email, password }
        );
        return response.data;
    },

    logout: async (refreshToken) => {
        const response = await axios.post(
            `${API_BASE_URL}/auth/logout`,
            { refreshToken }
        );
        return response.data;
    },

    refreshToken: async (refreshToken) => {
        const response = await axios.post(
            `${API_BASE_URL}/auth/refresh`,
            { refreshToken }
        );
        return response.data;
    },

    getMe: async () => {
        const response = await apiClient.get('/auth/me');
        return response.data;
    },
};

// ========== 비디오 API ==========
export const videoAPI = {
    // 1. 업로드 URL 요청
    initUpload: async (filename, contentType) => {
        const response = await apiClient.post(
            '/videos/init-upload',
            {
                filename,
                contentType,
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
    processVideo: async (videoId, s3Key, fileSize, maskingOptions) => {
        const response = await apiClient.post(
            `/videos/${videoId}/process`,
            {
                s3Key,
                fileSize,
                maskingOptions: {
                    face: maskingOptions.face,
                    licensePlate: maskingOptions.licensePlate,
                    object: maskingOptions.object,
                    objectName: maskingOptions.objectName,
                    useAvatar: maskingOptions.useAvatar
                }
            }
        );
        return response.data;
    },

    // 4. 상태 조회 (폴링용)
    getStatus: async (videoId) => {
        const response = await apiClient.get(`/videos/${videoId}/status`);
        return response.data;
    },

    // 5. 결과 조회
    getResult: async (videoId) => {
        const response = await apiClient.get(`/videos/${videoId}`);
        return response.data;
    },

    // 6. 내 비디오 목록
    getMyVideos: async () => {
        const response = await apiClient.get('/videos/my-videos');
        return response.data;
    },

    // 7. Health Check
    healthCheck: async () => {
        const response = await axios.get(`${API_BASE_URL}/videos/health`);
        return response.data;
    },

    // 8. 비디오 삭제
    deleteVideo: async (videoId) => {
        const response = await apiClient.delete(`/videos/${videoId}`);
        return response.data;
    },
};