import axios from 'axios';

const API_BASE_URL = '/api';

export const authAPI = {
    // 회원가입
    register: async (email, password, username) => {
        const response = await axios.post(`${API_BASE_URL}/auth/register`, {
            email,
            password,
            username,
        });
        return response.data;
    },
    // 로그인
    login: async (email, password) => {
        const response = await axios.post(`${API_BASE_URL}/auth/login`, {
            email,
            password,
        });
        return response.data;
    },
    // 내 정보 조회
    getMe: async (token) => {
        const response = await axios.get(`${API_BASE_URL}/auth/me`, {
            headers: {
                Authorization: `Bearer ${token}`,
            },
        });
        return response.data;
    },
    // 로그아웃
    logout: async (token) => {
        const response = await axios.post(`${API_BASE_URL}/auth/logout`, {},
            {
                headers: {
                    Authorization: `Bearer ${token}`,
                },
            });
        return response.data;
    },
};
