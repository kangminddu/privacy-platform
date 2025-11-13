export const tokenManager = {
    saveToken: (accessToken, refreshToken) => {
        console.log('💾 토큰 저장:', { accessToken: '있음', refreshToken: '있음' });
        localStorage.setItem('token', accessToken);
        localStorage.setItem('refreshToken', refreshToken);
    },

    getToken: () => {
        const token = localStorage.getItem('token');
        console.log('🔍 토큰 조회:', token ? '있음' : '없음');
        return token;
    },

    getRefreshToken: () => {
        return localStorage.getItem('refreshToken');
    },

    clearToken: () => {
        console.log('🗑️ 토큰 삭제');
        localStorage.removeItem('token');
        localStorage.removeItem('refreshToken');
        localStorage.removeItem('user');
    },

    saveUser: (user) => {
        console.log('💾 유저 정보 저장:', user);
        localStorage.setItem('user', JSON.stringify(user));
    },

    getUser: () => {
        const userStr = localStorage.getItem('user');
        if (!userStr) {
            console.log('❌ 유저 정보 없음');
            return null;
        }
        const user = JSON.parse(userStr);
        console.log('👤 유저 정보 조회:', user);
        return user;
    },
};