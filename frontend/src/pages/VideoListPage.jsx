import { useState, useEffect } from 'react';
import { videoAPI } from '../services/api';

function VideoListPage({ onNavigateToUpload }) {
    const [videos, setVideos] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [selectedVideo, setSelectedVideo] = useState(null);

    useEffect(() => {
        loadVideos();
    }, []);

    const loadVideos = async () => {
        try {
            setLoading(true);
            const data = await videoAPI.getMyVideos();
            setVideos(data);
            console.log('📋 비디오 목록:', data);
        } catch (err) {
            console.error('❌ 목록 조회 실패:', err);
            setError('비디오 목록을 불러올 수 없습니다.');
        } finally {
            setLoading(false);
        }
    };

    const getStatusBadge = (status) => {
        const statusConfig = {
            UPLOADED: { emoji: '📤', text: '업로드 완료', color: '#2196F3' },
            PROCESSING: { emoji: '⚙️', text: '처리 중', color: '#FF9800' },
            COMPLETED: { emoji: '✅', text: '완료', color: '#4CAF50' },
            FAILED: { emoji: '❌', text: '실패', color: '#F44336' },
        };
        const config = statusConfig[status] || statusConfig.UPLOADED;
        return (
            <span className="status-badge" style={{ backgroundColor: config.color }}>
                {config.emoji} {config.text}
            </span>
        );
    };

    const formatDate = (dateString) => {
        if (!dateString) return '-';
        const date = new Date(dateString);
        return date.toLocaleString('ko-KR', {
            year: 'numeric',
            month: '2-digit',
            day: '2-digit',
            hour: '2-digit',
            minute: '2-digit',
        });
    };

    const formatFileSize = (bytes) => {
        if (!bytes) return '-';
        return (bytes / 1024 / 1024).toFixed(2) + ' MB';
    };

    const handleVideoClick = (video) => {
        setSelectedVideo(video);
    };

    const handleCloseDetail = () => {
        setSelectedVideo(null);
    };

    const handleDelete = async (videoId) => {
        if (!window.confirm('정말 삭제하시겠습니까?')) return;
        try {
            await videoAPI.deleteVideo(videoId);
            alert('삭제되었습니다.');
            loadVideos();
            setSelectedVideo(null);
        } catch (err) {
            console.error('❌ 삭제 실패:', err);
            alert('삭제에 실패했습니다.');
        }
    };

    if (loading) {
        return (
            <div className="container">
                <h2>📋 내 비디오 목록</h2>
                <div className="loading">로딩 중...</div>
            </div>
        );
    }

    if (error) {
        return (
            <div className="container">
                <h2>📋 내 비디오 목록</h2>
                <div className="error-section">
                    <p>{error}</p>
                    <button onClick={loadVideos} className="btn-primary">
                        🔄 다시 시도
                    </button>
                </div>
            </div>
        );
    }

    return (
        <div className="container">
            <div className="page-header">
                <h2>📋 내 비디오 목록</h2>
                <button onClick={onNavigateToUpload} className="btn-primary">
                    ➕ 새 비디오 업로드
                </button>
            </div>

            {videos.length === 0 ? (
                <div className="empty-state">
                    <p>📭 업로드한 비디오가 없습니다.</p>
                    <button onClick={onNavigateToUpload} className="btn-primary">
                        첫 비디오 업로드하기
                    </button>
                </div>
            ) : (
                <div className="video-grid">
                    {videos.map((video) => (
                        <div
                            key={video.videoId}
                            className="video-card"
                            onClick={() => handleVideoClick(video)}
                        >
                            <div className="video-card-header">
                                <h3>📹 {video.originalFilename}</h3>
                                {getStatusBadge(video.status)}
                            </div>
                            <div className="video-card-body">
                                <p>📅 업로드: {formatDate(video.uploadedAt)}</p>
                                <p>💾 크기: {formatFileSize(video.fileSizeBytes)}</p>
                                {video.statistics && (
                                    <p>🔍 탐지: {video.statistics.totalDetections}개</p>
                                )}
                            </div>
                        </div>
                    ))}
                </div>
            )}

            {/* 상세 모달 */}
            {selectedVideo && (
                <div className="modal-overlay" onClick={handleCloseDetail}>
                    <div className="modal-content" onClick={(e) => e.stopPropagation()}>
                        <div className="modal-header">
                            <h2>📹 {selectedVideo.originalFilename}</h2>
                            <button className="close-button" onClick={handleCloseDetail}>
                                ✕
                            </button>
                        </div>

                        <div className="modal-body">
                            <div className="info-row">
                                <span>상태:</span>
                                {getStatusBadge(selectedVideo.status)}
                            </div>
                            <div className="info-row">
                                <span>업로드:</span>
                                <span>{formatDate(selectedVideo.uploadedAt)}</span>
                            </div>
                            <div className="info-row">
                                <span>파일 크기:</span>
                                <span>{formatFileSize(selectedVideo.fileSizeBytes)}</span>
                            </div>

                            {selectedVideo.status === 'COMPLETED' && (
                                <>
                                    <div className="info-row">
                                        <span>처리 완료:</span>
                                        <span>{formatDate(selectedVideo.processedAt)}</span>
                                    </div>

                                    <div className="stats-box">
                                        <h3>📊 탐지 통계</h3>
                                        <p>총 탐지: {selectedVideo.statistics.totalDetections}개</p>
                                        <p>😊 얼굴: {selectedVideo.statistics.faceCount}개</p>
                                        <p>🚗 번호판: {selectedVideo.statistics.licensePlateCount}개</p>
                                        <p>
                                            📈 평균 신뢰도:{' '}
                                            {(selectedVideo.statistics.averageConfidence * 100).toFixed(1)}%
                                        </p>
                                    </div>

                                    <div className="download-buttons">
                                        <a
                                            href={selectedVideo.originalDownloadUrl}
                                            target="_blank"
                                            rel="noopener noreferrer"
                                            className="btn-secondary"
                                        >
                                            📥 원본 다운로드
                                        </a>

                                        <a
                                            href={selectedVideo.processedDownloadUrl}
                                            target="_blank"
                                            rel="noopener noreferrer"
                                            className="btn-primary"
                                        >
                                            ✨ 처리본 다운로드
                                        </a>
                                    </div>
                                </>
                            )}

                            {selectedVideo.status === 'PROCESSING' && (
                                <div className="processing-info">
                                    <p>⚙️ AI가 비디오를 처리하고 있습니다...</p>
                                    <button onClick={loadVideos} className="btn-secondary">
                                        🔄 새로고침
                                    </button>
                                </div>
                            )}

                            {selectedVideo.status === 'FAILED' && (
                                <div className="failed-info">
                                    <p>❌ 처리에 실패했습니다.</p>
                                </div>
                            )}
                        </div>

                        <div className="modal-footer">
                            <button
                                onClick={() => handleDelete(selectedVideo.videoId)}
                                className="btn-danger"
                            >
                                🗑️ 삭제
                            </button>
                            <button onClick={handleCloseDetail} className="btn-secondary">
                                닫기
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}

export default VideoListPage;