import { useState, useEffect } from 'react';
import LoginPage from './pages/LoginPage';
import UploadPage from './pages/UploadPage';
import VideoListPage from './pages/VideoListPage';
import { tokenManager } from './utils/tokenManager';
import './App.css';

function App() {
    const [isAuthenticated, setIsAuthenticated] = useState(false);
    const [user, setUser] = useState(null);
    const [currentPage, setCurrentPage] = useState('upload'); // 'upload' or 'list'

    useEffect(() => {
        const token = tokenManager.getToken();
        const savedUser = tokenManager.getUser();

        if (token && savedUser) {
            setIsAuthenticated(true);
            setUser(savedUser);
        }
    }, []);

    const handleLoginSuccess = () => {
        const savedUser = tokenManager.getUser();
        setIsAuthenticated(true);
        setUser(savedUser);
    };

    const handleLogout = () => {
        tokenManager.clearToken();
        setIsAuthenticated(false);
        setUser(null);
        setCurrentPage('upload');
    };

    return (
        <div className="App">
            {isAuthenticated ? (
                <>
                    {/* 헤더 */}
                    <div className="header">
                        <div className="header-left">
                            <h2 className="logo">🔒 Privacy Platform</h2>
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

                    {/* 메인 컨텐츠 */}
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
    );
}

export default App;