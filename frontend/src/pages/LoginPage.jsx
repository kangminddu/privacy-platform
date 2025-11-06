import { useState } from 'react';
import { authAPI } from '../services/auth';
import { tokenManager } from '../utils/tokenManager';

function LoginPage({ onLoginSuccess }) {
    const [isLogin, setIsLogin] = useState(true); // true: 로그인, false: 회원가입
    const [formData, setFormData] = useState({
        email: '',
        password: '',
        username: '',
    });
    const [error, setError] = useState('');
    const [loading, setLoading] = useState(false);

    const handleChange = (e) => {
        setFormData({
            ...formData,
            [e.target.name]: e.target.value,
        });
        setError('');
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setLoading(true);
        setError('');

        try {
            if (isLogin) {
                // 로그인
                const response = await authAPI.login(formData.email, formData.password);

                // 토큰 저장
                tokenManager.saveToken(response.accessToken, response.refreshToken);
                tokenManager.saveUser({
                    userId: response.userId,
                    email: response.email,
                    username: response.username,
                });

                console.log('✅ 로그인 성공!', response);
                onLoginSuccess();
            } else {
                // 회원가입
                const response = await authAPI.register(
                    formData.email,
                    formData.password,
                    formData.username
                );

                // 토큰 저장
                tokenManager.saveToken(response.accessToken, response.refreshToken);
                tokenManager.saveUser({
                    userId: response.userId,
                    email: response.email,
                    username: response.username,
                });

                console.log('✅ 회원가입 성공!', response);
                onLoginSuccess();
            }
        } catch (err) {
            console.error('❌ 에러:', err);
            setError(
                err.response?.data?.message ||
                (isLogin ? '로그인에 실패했습니다.' : '회원가입에 실패했습니다.')
            );
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="login-container">
            <div className="login-box">
                <h1>🔒 Privacy Platform</h1>
                <p className="subtitle">비디오 내 개인정보 자동 마스킹</p>

                {/* 탭 전환 */}
                <div className="tab-buttons">
                    <button
                        className={isLogin ? 'active' : ''}
                        onClick={() => {
                            setIsLogin(true);
                            setError('');
                        }}
                    >
                        로그인
                    </button>
                    <button
                        className={!isLogin ? 'active' : ''}
                        onClick={() => {
                            setIsLogin(false);
                            setError('');
                        }}
                    >
                        회원가입
                    </button>
                </div>

                {/* 폼 */}
                <form onSubmit={handleSubmit}>
                    {!isLogin && (
                        <div className="form-group">
                            <label>이름</label>
                            <input
                                type="text"
                                name="username"
                                value={formData.username}
                                onChange={handleChange}
                                placeholder="홍길동"
                                required
                            />
                        </div>
                    )}

                    <div className="form-group">
                        <label>이메일</label>
                        <input
                            type="email"
                            name="email"
                            value={formData.email}
                            onChange={handleChange}
                            placeholder="example@email.com"
                            required
                        />
                    </div>

                    <div className="form-group">
                        <label>비밀번호</label>
                        <input
                            type="password"
                            name="password"
                            value={formData.password}
                            onChange={handleChange}
                            placeholder="8자 이상"
                            required
                            minLength={8}
                        />
                    </div>

                    {error && <div className="error-message">{error}</div>}

                    <button type="submit" className="btn-primary" disabled={loading}>
                        {loading ? '처리 중...' : isLogin ? '🔓 로그인' : '✨ 회원가입'}
                    </button>
                </form>

                <div className="login-footer">
                    <p>
                        {isLogin ? '계정이 없으신가요?' : '이미 계정이 있으신가요?'}{' '}
                        <button
                            className="link-button"
                            onClick={() => {
                                setIsLogin(!isLogin);
                                setError('');
                            }}
                        >
                            {isLogin ? '회원가입' : '로그인'}
                        </button>
                    </p>
                </div>
            </div>
        </div>
    );
}

export default LoginPage;