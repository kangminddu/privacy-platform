import { useState } from 'react';
import { authAPI } from '../services/api';
import { tokenManager } from '../utils/tokenManager';
import '../App.css';

function LoginPage({ onLoginSuccess }) {
    const [isLogin, setIsLogin] = useState(true);
    const [step, setStep] = useState('email'); // 'email', 'verify', 'complete'

    const [formData, setFormData] = useState({
        email: '',
        password: '',
        username: '',
        verificationCode: '',
    });

    const [error, setError] = useState('');
    const [loading, setLoading] = useState(false);
    const [message, setMessage] = useState('');

    const handleChange = (e) => {
        setFormData({
            ...formData,
            [e.target.name]: e.target.value,
        });
        setError('');
    };

    // 이메일 인증 코드 발송
    const handleSendCode = async () => {
        if (!formData.email) {
            setError('이메일을 입력해주세요.');
            return;
        }

        setLoading(true);
        setError('');

        try {
            await authAPI.sendVerificationCode(formData.email);
            setStep('verify');
            setMessage('인증 코드가 발송되었습니다. 이메일을 확인해주세요!');
        } catch (err) {
            console.error('❌ 에러:', err);
            setError(err.response?.data || '인증 코드 발송에 실패했습니다.');
        } finally {
            setLoading(false);
        }
    };

    // 인증 코드 확인
    const handleVerifyCode = async () => {
        if (!formData.verificationCode) {
            setError('인증 코드를 입력해주세요.');
            return;
        }

        setLoading(true);
        setError('');

        try {
            await authAPI.verifyCode(formData.email, formData.verificationCode);
            setStep('complete');
            setMessage('이메일 인증이 완료되었습니다!');
        } catch (err) {
            console.error('❌ 에러:', err);
            setError(err.response?.data || '인증 코드가 일치하지 않습니다.');
        } finally {
            setLoading(false);
        }
    };

    // 회원가입
    const handleRegister = async () => {
        if (!formData.username || !formData.password) {
            setError('모든 필드를 입력해주세요.');
            return;
        }

        if (formData.password.length < 8) {
            setError('비밀번호는 8자 이상이어야 합니다.');
            return;
        }

        setLoading(true);
        setError('');

        try {
            const response = await authAPI.register(
                formData.email,
                formData.password,
                formData.username
            );

            tokenManager.saveToken(response.accessToken, response.refreshToken);
            tokenManager.saveUser({
                userId: response.userId,
                email: response.email,
                username: response.username,
            });

            console.log('✅ 회원가입 성공!');
            onLoginSuccess();
        } catch (err) {
            console.error('❌ 에러:', err);
            setError(err.response?.data?.message || '회원가입에 실패했습니다.');
        } finally {
            setLoading(false);
        }
    };

    // 로그인
    const handleLogin = async (e) => {
        e.preventDefault();

        if (!formData.email || !formData.password) {
            setError('이메일과 비밀번호를 입력해주세요.');
            return;
        }

        setLoading(true);
        setError('');

        try {
            const response = await authAPI.login(formData.email, formData.password);

            tokenManager.saveToken(response.accessToken, response.refreshToken);
            tokenManager.saveUser({
                userId: response.userId,
                email: response.email,
                username: response.username,
            });

            console.log('✅ 로그인 성공!');
            onLoginSuccess();
        } catch (err) {
            console.error('❌ 에러:', err);
            setError(err.response?.data?.message || '로그인에 실패했습니다.');
        } finally {
            setLoading(false);
        }
    };

    // 카카오 로그인
    const handleKakaoLogin = () => {
        const backendUrl = import.meta.env.VITE_BACKEND_URL || 'http://localhost:8080';
        window.location.href = `${backendUrl}/oauth2/authorization/kakao`;
    };

    // 초기화
    const handleReset = () => {
        setStep('email');
        setFormData({
            email: '',
            password: '',
            username: '',
            verificationCode: '',
        });
        setError('');
        setMessage('');
    };

    return (
        <div className="login-container">
            <div className="login-box">
                <h1>🔒 Safe Masking</h1>
                <p className="subtitle">비디오 내 개인정보 자동 마스킹</p>

                {/* 탭 */}
                <div className="tab-buttons">
                    <button
                        className={isLogin ? 'active' : ''}
                        onClick={() => {
                            setIsLogin(true);
                            handleReset();
                        }}
                    >
                        로그인
                    </button>
                    <button
                        className={!isLogin ? 'active' : ''}
                        onClick={() => {
                            setIsLogin(false);
                            handleReset();
                        }}
                    >
                        회원가입
                    </button>
                </div>

                {/* 카카오 로그인 */}
                <div className="social-login-section">
                    <button className="btn-kakao" onClick={handleKakaoLogin}>
                        <img src="/src/assets/kakao-icon.png" alt="카카오" className="kakao-icon-img" />
                        카카오로 {isLogin ? '로그인' : '회원가입'}
                    </button>
                </div>

                <div className="divider">
                    <span>또는</span>
                </div>

                {/* 로그인 폼 */}
                {isLogin ? (
                    <form onSubmit={handleLogin}>
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
                            {loading ? '처리 중...' : '🔓 로그인'}
                        </button>
                    </form>
                ) : (
                    /* 회원가입 폼 */
                    <div>
                        {/* Step 1: 이메일 입력 */}
                        {step === 'email' && (
                            <div>
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

                                {error && <div className="error-message">{error}</div>}

                                <button
                                    onClick={handleSendCode}
                                    className="btn-primary"
                                    disabled={loading}
                                >
                                    {loading ? '발송 중...' : '📧 인증 코드 받기'}
                                </button>
                            </div>
                        )}

                        {/* Step 2: 인증 코드 입력 */}
                        {step === 'verify' && (
                            <div>
                                {message && <div className="success-message">{message}</div>}

                                <div className="form-group">
                                    <label>인증 코드 (6자리)</label>
                                    <input
                                        type="text"
                                        name="verificationCode"
                                        value={formData.verificationCode}
                                        onChange={handleChange}
                                        placeholder="123456"
                                        maxLength={6}
                                        required
                                    />
                                </div>

                                {error && <div className="error-message">{error}</div>}

                                <div className="button-group">
                                    <button
                                        onClick={handleVerifyCode}
                                        className="btn-primary"
                                        disabled={loading}
                                    >
                                        {loading ? '확인 중...' : '✅ 인증 확인'}
                                    </button>
                                    <button
                                        onClick={handleSendCode}
                                        className="btn-secondary"
                                        disabled={loading}
                                    >
                                        🔄 코드 재발송
                                    </button>
                                </div>
                            </div>
                        )}

                        {/* Step 3: 비밀번호 및 이름 입력 */}
                        {step === 'complete' && (
                            <div>
                                {message && <div className="success-message">{message}</div>}

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

                                <button
                                    onClick={handleRegister}
                                    className="btn-primary"
                                    disabled={loading}
                                >
                                    {loading ? '처리 중...' : '✨ 회원가입 완료'}
                                </button>
                            </div>
                        )}
                    </div>
                )}
            </div>
        </div>
    );
}

export default LoginPage;