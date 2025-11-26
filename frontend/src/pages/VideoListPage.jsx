import { useState, useEffect } from 'react';
import { videoAPI } from '../services/api';

function VideoListPage({ onNavigateToUpload }) {
    const [videos, setVideos] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [selectedVideo, setSelectedVideo] = useState(null);

    // 🔍 검색 및 필터 상태
    const [searchTerm, setSearchTerm] = useState('');
    const [filterStatus, setFilterStatus] = useState('ALL'); // ALL, COMPLETED, PROCESSING

    useEffect(() => {
        loadVideos();
    }, []);

    const loadVideos = async () => {
        try {
            setLoading(true);
            // 스켈레톤 UI를 보여주기 위해 약간의 지연 효과 (실제 느낌)
            await new Promise(r => setTimeout(r, 600));
            const data = await videoAPI.getMyVideos();
            setVideos(data);
        } catch (err) {
            setError('비디오 목록을 불러올 수 없습니다.');
        } finally {
            setLoading(false);
        }
    };

    // 필터링 로직
    const filteredVideos = videos.filter(video => {
        const matchesSearch = video.originalFilename.toLowerCase().includes(searchTerm.toLowerCase());
        const matchesStatus = filterStatus === 'ALL' || video.status === filterStatus;
        return matchesSearch && matchesStatus;
    });

    const getStatusBadge = (status) => {
        const config = {
            UPLOADED: { label: '대기 중', className: 'badge-waiting' },
            PROCESSING: { label: '분석 중', className: 'badge-processing' },
            COMPLETED: { label: '완료됨', className: 'badge-success' },
            FAILED: { label: '실패', className: 'badge-error' },
        };
        const { label, className } = config[status] || config.UPLOADED;
        return <span className={`status-badge ${className}`}>{label}</span>;
    };

    // ... (formatDate, formatFileSize, handleVideoClick 등 기존 함수 유지) ...
    const formatDate = (dateString) => new Date(dateString).toLocaleDateString();
    const formatFileSize = (bytes) => (bytes / 1024 / 1024).toFixed(1) + ' MB';
    const handleVideoClick = (v) => setSelectedVideo(v);
    const handleCloseDetail = () => setSelectedVideo(null);
    const handleDelete = async (id) => {
        if(!confirm('삭제하시겠습니까?')) return;
        await videoAPI.deleteVideo(id);
        loadVideos();
        setSelectedVideo(null);
    };


    return (
        <div className="container dashboard-container">
            {/* 상단 헤더 & 액션 */}
            <div className="dashboard-header">
                <div>
                    <h2>내 보관함</h2>
                    <p className="subtitle">업로드한 영상의 처리 상태를 확인하세요.</p>
                </div>
                <button onClick={onNavigateToUpload} className="btn-primary btn-icon">
                    <span>+</span> 새 영상 업로드
                </button>
            </div>

            {/* 🛠️ 툴바 (검색 & 필터) */}
            <div className="dashboard-toolbar">
                <div className="search-box">
                    <span className="search-icon">🔍</span>
                    <input
                        type="text"
                        placeholder="파일 이름 검색..."
                        value={searchTerm}
                        onChange={(e) => setSearchTerm(e.target.value)}
                    />
                </div>
                <div className="filter-tabs">
                    {['ALL', 'COMPLETED', 'PROCESSING'].map(status => (
                        <button
                            key={status}
                            className={`filter-tab ${filterStatus === status ? 'active' : ''}`}
                            onClick={() => setFilterStatus(status)}
                        >
                            {status === 'ALL' ? '전체' : status === 'COMPLETED' ? '완료' : '처리중'}
                        </button>
                    ))}
                </div>
            </div>

            {/* 로딩 중일 때 스켈레톤 UI 표시 */}
            {loading ? (
                <div className="video-grid">
                    {[1, 2, 3, 4].map(n => (
                        <div key={n} className="video-card skeleton-card">
                            <div className="skeleton-img"></div>
                            <div className="skeleton-text short"></div>
                            <div className="skeleton-text long"></div>
                        </div>
                    ))}
                </div>
            ) : filteredVideos.length === 0 ? (
                <div className="empty-state-modern">
                    <div className="empty-icon">📂</div>
                    <h3>표시할 비디오가 없습니다</h3>
                    <p>{searchTerm ? "검색 결과가 없습니다." : "영상을 업로드하여 AI 마스킹을 시작해보세요."}</p>
                    {!searchTerm && (
                        <button onClick={onNavigateToUpload} className="btn-secondary">
                            업로드하러 가기
                        </button>
                    )}
                </div>
            ) : (
                <div className="video-grid">
                    {filteredVideos.map((video) => (
                        <div key={video.videoId} className="video-card" onClick={() => handleVideoClick(video)}>
                            <div className="card-status-bar">
                                {getStatusBadge(video.status)}
                                <span className="card-date">{formatDate(video.uploadedAt)}</span>
                            </div>
                            <div className="card-content">
                                <div className="file-icon-wrapper">🎬</div>
                                <div className="file-info">
                                    <h3>{video.originalFilename}</h3>
                                    <span className="file-meta">{formatFileSize(video.fileSizeBytes)}</span>
                                </div>
                            </div>
                            {video.status === 'COMPLETED' && (
                                <div className="card-footer-stats">
                                    <div>🙂 {video.statistics?.faceCount || 0}</div>
                                    <div>🚗 {video.statistics?.licensePlateCount || 0}</div>
                                </div>
                            )}
                        </div>
                    ))}
                </div>
            )}

            {/* 상세 모달 (기존 코드 유지하되 스타일만 클래스로 제어) */}
            {selectedVideo && (
                <div className="modal-overlay" onClick={handleCloseDetail}>
                    <div className="modal-content" onClick={e => e.stopPropagation()}>
                        <div className="modal-header">
                            <h3>영상 상세 정보</h3>
                            <button className="close-btn" onClick={handleCloseDetail}>✕</button>
                        </div>
                        <div className="modal-body">
                            <h2 className="modal-filename">{selectedVideo.originalFilename}</h2>
                            <div className="modal-tags">
                                {getStatusBadge(selectedVideo.status)}
                                <span className="tag-date">{formatDate(selectedVideo.uploadedAt)}</span>
                            </div>

                            {/* 통계 박스 */}
                            {selectedVideo.status === 'COMPLETED' && (
                                <div className="stats-dashboard">
                                    <div className="stat-box">
                                        <span className="label">총 탐지</span>
                                        <span className="value">{selectedVideo.statistics.totalDetections}</span>
                                    </div>
                                    <div className="stat-box">
                                        <span className="label">얼굴</span>
                                        <span className="value">{selectedVideo.statistics.faceCount}</span>
                                    </div>
                                    <div className="stat-box">
                                        <span className="label">번호판</span>
                                        <span className="value">{selectedVideo.statistics.licensePlateCount}</span>
                                    </div>
                                </div>
                            )}

                            {/* 다운로드 버튼들 */}
                            {selectedVideo.status === 'COMPLETED' && (
                                <div className="modal-actions">
                                    <a href={selectedVideo.processedDownloadUrl} className="btn-download primary" target="_blank" rel="noreferrer">
                                        ✨ 처리된 영상 다운로드
                                    </a>
                                    <a href={selectedVideo.originalDownloadUrl} className="btn-download secondary" target="_blank" rel="noreferrer">
                                        📥 원본 영상
                                    </a>
                                </div>
                            )}

                            <div className="modal-danger-zone">
                                <button onClick={() => handleDelete(selectedVideo.videoId)} className="btn-delete">
                                    영상 삭제
                                </button>
                            </div>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}

export default VideoListPage;