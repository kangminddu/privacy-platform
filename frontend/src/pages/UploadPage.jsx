import { useState, useRef } from 'react';
import { videoAPI } from '../services/api';
import { WebSocketService } from '../services/websocket';
import '../App.css';

function UploadPage() {
    const [file, setFile] = useState(null);
    const [videoId, setVideoId] = useState(null);
    const [uploadProgress, setUploadProgress] = useState(0);
    const [processProgress, setProcessProgress] = useState(0);
    const [status, setStatus] = useState('idle'); // idle, uploading, processing, completed, failed
    const [message, setMessage] = useState('');
    const [result, setResult] = useState(null);
    const fileInputRef = useRef(null);
    const wsService = useRef(null);

    // 파일 선택
    const handleFileSelect = (event) => {
        const selectedFile = event.target.files[0];
        if (selectedFile && selectedFile.type.startsWith('video/')) {
            setFile(selectedFile);
            setStatus('idle');
            setMessage('');
        } else {
            alert('비디오 파일을 선택해주세요!');
        }
    };

    // 드래그 앤 드롭
    const handleDragOver = (e) => e.preventDefault();
    const handleDrop = (e) => {
        e.preventDefault();
        const droppedFile = e.dataTransfer.files[0];
        if (droppedFile && droppedFile.type.startsWith('video/')) {
            setFile(droppedFile);
        } else {
            alert('비디오 파일을 드롭해주세요!');
        }
    };

    // 업로드 및 처리
    const handleUpload = async () => {
        if (!file) {
            alert('파일을 선택해주세요!');
            return;
        }

        try {
            // 1️⃣ Pre-signed URL 요청
            setStatus('uploading');
            setMessage('업로드 URL 생성 중...');

            const { videoId: newVideoId, uploadUrl, s3Key } = await videoAPI.initUpload(
                file.name,
                file.type
            );

            setVideoId(newVideoId);
            console.log('📝 VideoID:', newVideoId);

            // 2️⃣ S3 업로드
            setMessage('파일 업로드 중...');
            await videoAPI.uploadToS3(uploadUrl, file, setUploadProgress);
            console.log('✅ S3 업로드 완료!');

            setMessage('업로드 완료! AI 처리 시작...');

            // 3️⃣ WebSocket 연결
            wsService.current = new WebSocketService();
            await wsService.current.connect(newVideoId, (progress) => {
                setProcessProgress(progress.percentage);
                setMessage(progress.message);

                if (progress.status === 'COMPLETED') {
                    setStatus('completed');
                    loadResult(newVideoId);
                } else if (progress.status === 'FAILED') {
                    setStatus('failed');
                    setMessage('처리 실패: ' + progress.message);
                }
            });

            // 4️⃣ 처리 요청
            setStatus('processing');
            await videoAPI.processVideo(newVideoId, s3Key, file.size);
            console.log('🚀 처리 시작 요청 완료!');
        } catch (error) {
            console.error('❌ 에러:', error);
            setStatus('failed');
            setMessage('에러 발생: ' + error.message);
        }
    };

    // 결과 조회
    const loadResult = async (vid) => {
        try {
            const data = await videoAPI.getResult(vid);
            setResult(data);
            console.log('📊 결과:', data);
        } catch (error) {
            console.error('❌ 결과 조회 실패:', error);
        }
    };

    // 초기화
    const handleReset = () => {
        setFile(null);
        setVideoId(null);
        setStatus('idle');
        setResult(null);
        setUploadProgress(0);
        setProcessProgress(0);
        setMessage('');
        if (wsService.current) wsService.current.disconnect();
    };

    return (
        <div className="container">
            <h1>🔒 Privacy Platform</h1>
            <p>비디오 내 개인정보 자동 마스킹</p>

            {/* 파일 업로드 구역 */}
            <div
                className="upload-area"
                onDragOver={handleDragOver}
                onDrop={handleDrop}
                onClick={() => fileInputRef.current?.click()}
            >
                {file ? (
                    <div>
                        <p>📹 {file.name}</p>
                        <p>{(file.size / 1024 / 1024).toFixed(2)} MB</p>
                    </div>
                ) : (
                    <div>
                        <p>📁 비디오 파일을 드래그하거나 클릭하세요</p>
                        <p style={{ fontSize: '14px', color: '#666' }}>
                            지원 형식: MP4, AVI, MOV
                        </p>
                    </div>
                )}
                <input
                    ref={fileInputRef}
                    type="file"
                    accept="video/*"
                    onChange={handleFileSelect}
                    style={{ display: 'none' }}
                />
            </div>

            {/* 업로드 버튼 */}
            {file && status === 'idle' && (
                <button onClick={handleUpload} className="btn-primary">
                    🚀 처리 시작
                </button>
            )}

            {/* 업로드 진행률 */}
            {status === 'uploading' && (
                <div className="progress-section">
                    <h3>📤 업로드 중...</h3>
                    <div className="progress-bar">
                        <div
                            className="progress-fill"
                            style={{ width: `${uploadProgress}%` }}
                        />
                    </div>
                    <p>{uploadProgress}%</p>
                </div>
            )}

            {/* AI 처리 중 */}
            {status === 'processing' && (
                <div className="progress-section">
                    <h3>⚙️ AI 처리 중...</h3>
                    <div className="progress-bar">
                        <div
                            className="progress-fill processing"
                            style={{ width: `${processProgress}%` }}
                        />
                    </div>
                    <p>{processProgress}% - {message}</p>
                </div>
            )}

            {/* 결과 */}
            {status === 'completed' && result && (
                <div className="result-section">
                    <h2>✅ 처리 완료!</h2>

                    <div className="stats">
                        <div className="stat-card">
                            <h3>📊 탐지 통계</h3>
                            <p>총 탐지: {result.statistics.totalDetections}개</p>
                            <p>😊 얼굴: {result.statistics.faceCount}개</p>
                            <p>🚗 번호판: {result.statistics.licensePlateCount}개</p>
                            <p>📈 평균 신뢰도: {(result.statistics.averageConfidence * 100).toFixed(1)}%</p>
                        </div>
                    </div>

                    <div className="download-buttons">
                        <a
                            href={result.originalDownloadUrl}
                            target="_blank"
                            rel="noopener noreferrer"
                            className="btn-secondary"
                        >
                            📥 원본 다운로드
                        </a>
                        <a
                            href={result.processedDownloadUrl}
                            target="_blank"
                            rel="noopener noreferrer"
                            className="btn-primary"
                        >
                            ✨ 처리본 다운로드
                        </a>
                    </div>

                    <button onClick={handleReset} className="btn-secondary">
                        🔄 새로 시작
                    </button>
                </div>
            )}

            {/* 실패 */}
            {status === 'failed' && (
                <div className="error-section">
                    <h2>❌ 처리 실패</h2>
                    <p>{message}</p>
                    <button
                        onClick={() => {
                            setStatus('idle');
                            setMessage('');
                        }}
                        className="btn-secondary"
                    >
                        🔄 다시 시도
                    </button>
                </div>
            )}
        </div>
    );
}

export default UploadPage;