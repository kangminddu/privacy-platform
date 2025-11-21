import axios from 'axios';
import { tokenManager } from '../utils/tokenManager';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api';

// JWT 토큰 헤더 가져오기
const getAuthHeaders = () => {
    const token = tokenManager.getToken();
    return token ? { Authorization: `Bearer ${token}` } : {};
};

// ========== 인증 API ==========
export const authAPI = {
    // 이메일 인증 코드 발송
    sendVerificationCode: async (email) => {
        const response = await axios.post(
            `${API_BASE_URL}/auth/send-code`,
            { email }
        );
        return response.data;
    },

    // 이메일 인증 코드 확인
    verifyCode: async (email, code) => {
        const response = await axios.post(
            `${API_BASE_URL}/auth/verify-code`,
            { email, code }
        );
        return response.data;
    },

    // 회원가입
    register: async (email, password, username) => {
        const response = await axios.post(
            `${API_BASE_URL}/auth/register`,
            { email, password, username }
        );
        return response.data;
    },

    // 로그인
    login: async (email, password) => {
        const response = await axios.post(
            `${API_BASE_URL}/auth/login`,
            { email, password }
        );
        return response.data;
    },

    // 로그아웃
    logout: async (refreshToken) => {
        const response = await axios.post(
            `${API_BASE_URL}/auth/logout`,
            { refreshToken }
        );
        return response.data;
    },

    // 토큰 갱신
    refreshToken: async (refreshToken) => {
        const response = await axios.post(
            `${API_BASE_URL}/auth/refresh`,
            { refreshToken }
        );
        return response.data;
    },

    // 내 정보 조회
    getMe: async () => {
        const response = await axios.get(
            `${API_BASE_URL}/auth/me`,
            {
                headers: getAuthHeaders(),
            }
        );
        return response.data;
    },
};

// ========== 비디오 API ==========
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
                maskingOptions: {
                    face: maskingOptions.face,
                    licensePlate: maskingOptions.licensePlate,
                    object: maskingOptions.object
                }
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

    // 7. 비디오 삭제
    deleteVideo: async (videoId) => {
        const response = await axios.delete(
            `${API_BASE_URL}/videos/${videoId}`,
            {
                headers: getAuthHeaders(),
            }
        );
        return response.data;
    },
};