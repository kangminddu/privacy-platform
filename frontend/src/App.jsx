import { useState, useEffect } from 'react';
import { BrowserRouter, Routes, Route } from 'react-router-dom';
import LoginPage from './pages/LoginPage';
import UploadPage from './pages/UploadPage';
import VideoListPage from './pages/VideoListPage';
import OAuthCallback from './pages/OAuthCallback';
import { tokenManager } from './utils/tokenManager';
import './App.css';

function App() {
    const [isAuthenticated, setIsAuthenticated] = useState(false);
    const [user, setUser] = useState(null);
    const [currentPage, setCurrentPage] = useState('upload');
    const [loading, setLoading] = useState(true);  // ⭐ 추가

    useEffect(() => {
        console.log('🔍 App 초기화 - 토큰 확인 중...');
        const token = tokenManager.getToken();
        const savedUser = tokenManager.getUser();

        console.log('🔑 Token:', token ? '있음' : '없음');
        console.log('👤 User:', savedUser);

        if (token && savedUser) {
            setIsAuthenticated(true);
            setUser(savedUser);
            console.log('✅ 인증 상태: 로그인됨');
        } else {
            console.log('❌ 인증 상태: 로그아웃');
        }

        setLoading(false);  // ⭐ 로딩 완료
    }, []);

    const handleLoginSuccess = () => {
        console.log('🎉 handleLoginSuccess 호출됨');
        const savedUser = tokenManager.getUser();
        console.log('👤 저장된 유저:', savedUser);
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

    // ⭐ 로딩 중 화면
    if (loading) {
        return (
            <div style={{
                display: 'flex',
                justifyContent: 'center',
                alignItems: 'center',
                height: '100vh',
                fontSize: '2rem'
            }}>
                ⏳ 로딩 중...
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
                            <>
                                <div className="header">
                                    <div className="header-left">
                                        <h2 className="logo">🔒 Safe Masking</h2>
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
                                                📋 내 비디오
                                            </button>
                                        </div>
                                    </div>
                                    <div className="header-right">
                                        <div className="user-info">
                                            👤 {user?.username} ({user?.email})
                                        </div>
                                        <button onClick={handleLogout} className="btn-logout">
                                            🚪 로그아웃
                                        </button>
                                    </div>
                                </div>

                                {currentPage === 'upload' ? (
                                    <UploadPage onNavigateToList={() => setCurrentPage('list')} />
                                ) : (
                                    <VideoListPage onNavigateToUpload={() => setCurrentPage('upload')} />
                                )}
                            </>
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