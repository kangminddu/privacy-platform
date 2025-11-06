import { useState, useEffect } from 'react';
import LoginPage from './pages/LoginPage';
import UploadPage from './pages/UploadPage';
import { tokenManager } from './utils/tokenManager';
import './App.css';

function App() {
    const [isAuthenticated, setIsAuthenticated] = useState(false);
    const [user, setUser] = useState(null);

    useEffect(() => {
        // 페이지 로드 시 토큰 확인
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
    };

    return (
        <div className="App">
            {isAuthenticated ? (
                <>
                    {/* 헤더 */}
                    <div className="header">
                        <div className="user-info">
                            👤 {user?.username} ({user?.email})
                        </div>
                        <button onClick={handleLogout} className="btn-logout">
                            🚪 로그아웃
                        </button>
                    </div>

                    {/* 메인 페이지 */}
                    <UploadPage />
                </>
            ) : (
                <LoginPage onLoginSuccess={handleLoginSuccess} />
            )}
        </div>
    );
}

export default App;