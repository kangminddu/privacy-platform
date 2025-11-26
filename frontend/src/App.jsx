import { useState, useEffect } from 'react';
import { BrowserRouter, Routes, Route } from 'react-router-dom';
import LoginPage from './pages/LoginPage';
import UploadPage from './pages/UploadPage';
import VideoListPage from './pages/VideoListPage'; // 새로 만든 리스트 페이지
import OAuthCallback from './pages/OAuthCallback';
import { tokenManager } from './utils/tokenManager';
import './App.css';

// ✨ 푸터 컴포넌트 (App.jsx 안에 정의)
const Footer = () => (
    <footer className="app-footer">
        <div className="footer-content">
            <div className="footer-logo">🔒 Safe Masking</div>
            <div className="footer-links">
                <span>이용약관</span>
                <span>개인정보처리방침</span>
                <span>고객센터</span>
            </div>
            <p className="footer-copy">© 2025 Safe Masking Inc. All rights reserved.</p>
        </div>
    </footer>
);

function App() {
    const [isAuthenticated, setIsAuthenticated] = useState(false);
    const [user, setUser] = useState(null);
    const [currentPage, setCurrentPage] = useState('upload');
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        console.log('🔍 App 초기화 - 토큰 확인 중...');
        const token = tokenManager.getToken();
        const savedUser = tokenManager.getUser();

        if (token && savedUser) {
            setIsAuthenticated(true);
            setUser(savedUser);
            console.log('✅ 인증 상태: 로그인됨');
        } else {
            console.log('❌ 인증 상태: 로그아웃');
        }

        setLoading(false);
    }, []);

    const handleLoginSuccess = () => {
        console.log('🎉 handleLoginSuccess 호출됨');
        const savedUser = tokenManager.getUser();
        setIsAuthenticated(true);
        setUser(savedUser);
    };

    const handleLogout = () => {
        console.log('🚪 로그아웃');
        tokenManager.clearToken();
        setIsAuthenticated(false);
        setUser(null);
        setCurrentPage('upload');
    };

    // 로딩 화면
    if (loading) {
        return (
            <div style={{
                display: 'flex',
                justifyContent: 'center',
                alignItems: 'center',
                height: '100vh',
                fontSize: '1.2rem',
                color: '#666'
            }}>
                ⏳ Safe Masking 로딩 중...
            </div>
        );
    }

    return (
        <BrowserRouter>
            <Routes>
                {/* OAuth 콜백 */}
                <Route path="/auth/callback" element={<OAuthCallback />} />

                {/* 메인 앱 */}
                <Route path="/*" element={
                    <div className="App">
                        {isAuthenticated ? (
                            <div className="app-layout">
                                {/* 헤더 */}
                                <header className="header">
                                    <div className="header-left">
                                        <h2 className="logo" onClick={() => setCurrentPage('upload')}>
                                            🔒 Safe Masking
                                        </h2>
                                        <div className="nav-buttons">
                                            <button
                                                className={currentPage === 'upload' ? 'active' : ''}
                                                onClick={() => setCurrentPage('upload')}
                                            >
                                                ➕ 업로드
                                            </button>
                                            <button
                                                className={currentPage === 'list' ? 'active' : ''}
                                                onClick={() => setCurrentPage('list')}
                                            >
                                                📋 내 보관함
                                            </button>
                                        </div>
                                    </div>
                                    <div className="header-right">
                                        <div className="user-info">
                                            👤 {user?.username}님
                                        </div>
                                        <button onClick={handleLogout} className="btn-logout">
                                            로그아웃
                                        </button>
                                    </div>
                                </header>

                                {/* 메인 컨텐츠 */}
                                <main className="main-content">
                                    {currentPage === 'upload' ? (
                                        <UploadPage onNavigateToList={() => setCurrentPage('list')} />
                                    ) : (
                                        <VideoListPage onNavigateToUpload={() => setCurrentPage('upload')} />
                                    )}
                                </main>

                                {/* 푸터 */}
                                <Footer />
                            </div>
                        ) : (
                            <LoginPage onLoginSuccess={handleLoginSuccess} />
                        )}
                    </div>
                } />
            </Routes>
        </BrowserRouter>
    );
}

export default App;