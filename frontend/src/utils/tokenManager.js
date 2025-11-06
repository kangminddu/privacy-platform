const TOKEN_KEY = 'access_token';
const REFRESH_TOKEN_KEY = 'refresh_token';
const USER_KEY = 'user_info';

export const tokenManager = {
    // 토큰 저장
    saveToken: (accessToken, refreshToken) => {
        localStorage.setItem(TOKEN_KEY, accessToken);
        if (refreshToken) {
            localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken);
        }
    },

    // 사용자 정보 저장
    saveUser: (user) => {
        localStorage.setItem(USER_KEY, JSON.stringify(user));
    },

    // 토큰 가져오기
    getToken: () => {
        return localStorage.getItem(TOKEN_KEY);
    },

    // 사용자 정보 가져오기
    getUser: () => {
        const user = localStorage.getItem(USER_KEY);
        return user ? JSON.parse(user) : null;
    },

    // 토큰 삭제
    clearToken: () => {
        localStorage.removeItem(TOKEN_KEY);
        localStorage.removeItem(REFRESH_TOKEN_KEY)
        localStorage.removeItem(USER_KEY);
    },

    // 토큰 있는지 확인
    hasToken: () => {
        return !!localStorage.getItem(TOKEN_KEY);
    },
};