import { useState, useEffect } from 'react';
import { videoAPI } from '../services/api';
import './VideoListPage.css'; // ★ CSS 파일 import 확인

// 아이콘 정의
const Icons = {
    Search: () => <svg width="20" height="20" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" /></svg>,
    Plus: () => <svg width="20" height="20" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" /></svg>,
    Refresh: () => <svg width="20" height="20" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" /></svg>,
    Video: () => <svg width="24" height="24" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M15 10l4.553-2.276A1 1 0 0121 8.618v6.764a1 1 0 01-1.447.894L15 14M5 18h8a2 2 0 002-2V8a2 2 0 00-2-2H5a2 2 0 00-2 2v8a2 2 0 002 2z" /></svg>,
    Close: () => <svg width="24" height="24" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" /></svg>,
    Download: () => <svg width="20" height="20" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4" /></svg>,
    File: () => <svg width="20" height="20" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" /></svg>
};

function VideoListPage({ onNavigateToUpload }) {
    const [videos, setVideos] = useState([]);
    const [loading, setLoading] = useState(true);
    const [selectedVideo, setSelectedVideo] = useState(null);
    const [searchTerm, setSearchTerm] = useState('');
    const [filterStatus, setFilterStatus] = useState('ALL');
    const [isDeleting, setIsDeleting] = useState(false);

    useEffect(() => { loadVideos(); }, []);

    const loadVideos = async () => {
        try {
            setLoading(true);
            const data = await videoAPI.getMyVideos();
            setVideos(data);
        } catch (err) { console.error(err); }
        finally { setLoading(false); }
    };

    const handleDelete = async (id) => {
        if (!confirm('정말 삭제하시겠습니까? 삭제된 데이터는 복구할 수 없습니다.')) return;
        try {
            setIsDeleting(true);
            await videoAPI.deleteVideo(id);
            await loadVideos();
            setSelectedVideo(null);
        } catch (err) { alert('삭제 중 오류가 발생했습니다.'); }
        finally { setIsDeleting(false); }
    };

    const filteredVideos = videos.filter(video => {
        const matchesSearch = video.originalFilename.toLowerCase().includes(searchTerm.toLowerCase());
        const matchesStatus = filterStatus === 'ALL' || video.status === filterStatus;
        return matchesSearch && matchesStatus;
    });

    const getStatusBadge = (status) => {
        const config = {
            UPLOADED: { label: '대기 중', className: 'badge-gray' },
            PROCESSING: { label: '분석 중', className: 'badge-yellow' },
            COMPLETED: { label: '완료됨', className: 'badge-green' },
            FAILED: { label: '실패', className: 'badge-red' },
        };
        const { label, className } = config[status] || config.UPLOADED;
        return <span className={`status-badge ${className}`}>{label}</span>;
    };

    const formatFileSize = (bytes) => (!bytes ? '0 MB' : (bytes / 1024 / 1024).toFixed(1) + ' MB');

    return (
        <div className="container dashboard-container">
            {/* 헤더 */}
            <div className="dashboard-header-row">
                <div className="title-area">
                    <h2>내 보관함</h2>
                    <button onClick={loadVideos} className="btn-icon" title="새로고침"><Icons.Refresh /></button>
                </div>
                <button onClick={onNavigateToUpload} className="btn-primary-new">
                    <Icons.Plus /><span>새 영상 업로드</span>
                </button>
            </div>

            {/* 툴바 */}
            <div className="toolbar-row">
                <div className="search-wrapper">
                    <div className="search-icon"><Icons.Search /></div>
                    <input type="text" placeholder="파일 이름 검색..." value={searchTerm} onChange={(e) => setSearchTerm(e.target.value)} />
                </div>
                <div className="filter-group">
                    {['ALL', 'COMPLETED', 'PROCESSING'].map(status => (
                        <button key={status} className={`filter-btn ${filterStatus === status ? 'active' : ''}`} onClick={() => setFilterStatus(status)}>
                            {status === 'ALL' ? '전체' : status === 'COMPLETED' ? '완료' : '진행중'}
                        </button>
                    ))}
                </div>
            </div>

            {/* 리스트 */}
            {loading ? (
                <div className="grid-layout">
                    {[1, 2, 3, 4].map(n => <div key={n} className="video-card-item skeleton-card" style={{height: '200px', background: '#f8fafc'}}></div>)}
                </div>
            ) : filteredVideos.length === 0 ? (
                <div className="empty-state-box">
                    <span className="empty-icon">📂</span>
                    <p>{searchTerm ? "검색 결과가 없습니다." : "아직 업로드한 영상이 없습니다."}</p>
                </div>
            ) : (
                <div className="grid-layout">
                    {filteredVideos.map((video) => (
                        <div key={video.videoId} className="video-card-item" onClick={() => setSelectedVideo(video)}>
                            <div className="card-top">
                                {getStatusBadge(video.status)}
                                <span>{new Date(video.uploadedAt).toLocaleDateString()}</span>
                            </div>
                            <div className="card-middle">
                                <div className="file-icon-box"><Icons.Video /></div>
                                <div className="text-info">
                                    <h3 className="text-truncate">{video.originalFilename}</h3>
                                    <span className="file-size">{formatFileSize(video.fileSizeBytes)}</span>
                                </div>
                            </div>
                            <div className="card-bottom">
                                <div className="stat-pill">얼굴 {video.statistics?.faceCount || 0}</div>
                                <div className="stat-pill">번호판 {video.statistics?.licensePlateCount || 0}</div>
                            </div>
                        </div>
                    ))}
                </div>
            )}

            {/* 상세 모달 */}
            {selectedVideo && (
                <div className="modal-backdrop" onClick={() => !isDeleting && setSelectedVideo(null)}>
                    <div className="modal-panel" onClick={e => e.stopPropagation()}>
                        <div className="modal-header">
                            <h3>영상 상세 정보</h3>
                            <button className="close-button" onClick={() => setSelectedVideo(null)}><Icons.Close /></button>
                        </div>
                        <div className="modal-body">
                            <div className="video-detail-hero">
                                <h2>{selectedVideo.originalFilename}</h2>
                                <div className="video-meta-info">
                                    {getStatusBadge(selectedVideo.status)}
                                    <span>•</span><span>{new Date(selectedVideo.uploadedAt).toLocaleDateString()}</span>
                                    <span>•</span><span>{formatFileSize(selectedVideo.fileSizeBytes)}</span>
                                </div>
                            </div>

                            {selectedVideo.status === 'PROCESSING' && (
                                <div style={{textAlign: 'center', padding: '30px', color: '#d97706'}}>
                                    <p>AI가 영상을 분석 중입니다...</p>
                                </div>
                            )}

                            {selectedVideo.status === 'COMPLETED' && (
                                <div className="stats-grid-box">
                                    <div className="stat-card total">
                                        <span className="label">총 탐지</span><span className="value">{selectedVideo.statistics?.totalDetections || 0}</span>
                                    </div>
                                    <div className="stat-card">
                                        <span className="label">얼굴</span><span className="value">{selectedVideo.statistics?.faceCount || 0}</span>
                                    </div>
                                    <div className="stat-card">
                                        <span className="label">번호판</span><span className="value">{selectedVideo.statistics?.licensePlateCount || 0}</span>
                                    </div>
                                </div>
                            )}

                            {selectedVideo.status === 'COMPLETED' && (
                                <div className="modal-action-buttons">
                                    <a href={selectedVideo.processedDownloadUrl} className="btn-download-large primary" target="_blank" rel="noreferrer">
                                        <Icons.Download /> 결과 영상 다운로드
                                    </a>
                                    <a href={selectedVideo.originalDownloadUrl} className="btn-download-large secondary" target="_blank" rel="noreferrer">
                                        <Icons.File /> 원본 영상 다운로드
                                    </a>
                                </div>
                            )}

                            <div className="delete-section">
                                <button onClick={() => handleDelete(selectedVideo.videoId)} className="btn-delete-link" disabled={isDeleting}>
                                    {isDeleting ? '삭제 중...' : '이 영상 영구 삭제하기'}
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