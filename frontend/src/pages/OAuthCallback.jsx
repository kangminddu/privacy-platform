import { useEffect } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import { tokenManager } from '../utils/tokenManager';

function OAuthCallback() {
    const [searchParams] = useSearchParams();
    const navigate = useNavigate();

    useEffect(() => {
        console.log('🔍 OAuth Callback 페이지 로드');
        console.log('📋 전체 URL:', window.location.href);

        const accessToken = searchParams.get('accessToken');
        const refreshToken = searchParams.get('refreshToken');
        const userId = searchParams.get('userId');
        const email = searchParams.get('email');
        const username = decodeURIComponent(searchParams.get('username') || '');

        console.log('🔑 accessToken:', accessToken ? '있음' : '없음');
        console.log('🔑 refreshToken:', refreshToken ? '있음' : '없음');
        console.log('👤 userId:', userId);
        console.log('📧 email:', email);
        console.log('🏷️ username:', username);

        if (accessToken && refreshToken) {
            // 토큰 저장
            tokenManager.saveToken(accessToken, refreshToken);
            tokenManager.saveUser({
                userId,
                email,
                username,
            });

            console.log('✅ 토큰 저장 완료!');
            console.log('📦 localStorage 확인:', {
                token: localStorage.getItem('token'),
                user: localStorage.getItem('user')
            });

            // 페이지 새로고침으로 App 재초기화
            console.log('🔄 메인 페이지로 이동');
            window.location.href = '/';

        } else {
            console.error('❌ 토큰이 없습니다');
            console.error('받은 파라미터:', {
                accessToken: accessToken ? 'O' : 'X',
                refreshToken: refreshToken ? 'O' : 'X',
                userId,
                email,
                username
            });
            alert('로그인에 실패했습니다.');
            navigate('/');
        }
    }, [searchParams, navigate]);

    return (
        <div style={{
            display: 'flex',
            justifyContent: 'center',
            alignItems: 'center',
            height: '100vh',
            flexDirection: 'column',
            gap: '20px',
            backgroundColor: '#f5f5f5'
        }}>
            <div style={{ fontSize: '4rem' }}>🔄</div>
            <p style={{
                fontSize: '1.5rem',
                fontWeight: 'bold',
                color: '#333'
            }}>
                로그인 처리 중...
            </p>
            <p style={{
                fontSize: '1rem',
                color: '#666'
            }}>
                잠시만 기다려주세요
            </p>
        </div>
    );
}

export default OAuthCallback;