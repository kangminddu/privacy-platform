import { useState, useRef, useEffect } from "react";
import { videoAPI } from "../services/api";
import {
    RiUserSmileLine,
    RiCarLine,
    RiFocus3Line,
    RiBlurOffLine,
    RiRobot2Line,
    RiUploadCloud2Line,
    RiMovieLine,
    RiCheckLine,
    RiDownloadLine,
    RiRefreshLine,
    RiFileListLine,
    RiErrorWarningLine
} from "react-icons/ri";
import "../App.css"; // CSS 연결 확인

function UploadPage({ onNavigateToList }) {
    const [file, setFile] = useState(null);
    const [videoId, setVideoId] = useState(null);
    const [uploadProgress, setUploadProgress] = useState(0);
    const [status, setStatus] = useState("idle");
    const [message, setMessage] = useState("");
    const [result, setResult] = useState(null);

    const fileInputRef = useRef(null);
    const pollingRef = useRef(null);

    const [isAvatarMode, setIsAvatarMode] = useState(false);

    const [maskingOptions, setMaskingOptions] = useState({
        face: true,
        licensePlate: true,
        object: false,
        objectName: ""
    });

    // 컴포넌트 언마운트 시 폴링 정리
    useEffect(() => {
        return () => {
            if (pollingRef.current) {
                clearInterval(pollingRef.current);
            }
        };
    }, []);

    // 상태 폴링 함수
    const startPolling = (vid) => {
        pollingRef.current = setInterval(async () => {
            try {
                const statusData = await videoAPI.getStatus(vid);
                setMessage(statusData.message);

                if (statusData.status === "COMPLETED") {
                    clearInterval(pollingRef.current);
                    setStatus("completed");
                    loadResult(vid);
                } else if (statusData.status === "FAILED") {
                    clearInterval(pollingRef.current);
                    setStatus("failed");
                    setMessage("처리 실패");
                }
            } catch (error) {
                console.error("상태 조회 실패:", error);
            }
        }, 3000);
    };

    const handleFileSelect = (e) => {
        const selectedFile = e.target.files[0];
        if (selectedFile && selectedFile.type.startsWith("video/")) {
            setFile(selectedFile);
            setStatus("idle");
            setMessage("");
        } else {
            alert("비디오 파일을 선택해주세요!");
        }
    };

    const handleDragOver = (e) => e.preventDefault();
    const handleDrop = (e) => {
        e.preventDefault();
        const droppedFile = e.dataTransfer.files[0];
        if (droppedFile?.type.startsWith("video/")) {
            setFile(droppedFile);
        } else {
            alert("비디오 파일을 드롭해주세요!");
        }
    };

    const toggleOption = (key) => {
        setMaskingOptions((prev) => ({
            ...prev,
            [key]: !prev[key]
        }));
    };

    const handleUpload = async () => {
        if (!file) return alert("파일을 선택해주세요!");

        try {
            setStatus("uploading");
            setMessage("업로드 URL 생성 중...");

            const { videoId: newVideoId, uploadUrl, s3Key } = await videoAPI.initUpload(
                file.name,
                file.type
            );

            setVideoId(newVideoId);
            setMessage("파일 업로드 중...");
            await videoAPI.uploadToS3(uploadUrl, file, setUploadProgress);

            setStatus("processing");
            setMessage("AI 분석 요청 중...");

            await videoAPI.processVideo(newVideoId, s3Key, file.size, {
                face: maskingOptions.face,
                licensePlate: maskingOptions.licensePlate,
                object: maskingOptions.object,
                objectName: maskingOptions.objectName.trim(),
                useAvatar: isAvatarMode
            });

            startPolling(newVideoId);

        } catch (error) {
            setStatus("failed");
            setMessage("에러 발생: " + error.message);
        }
    };

    const loadResult = async (vid) => {
        try {
            const data = await videoAPI.getResult(vid);
            setResult(data);
        } catch (e) {
            console.error("결과 조회 실패:", e);
        }
    };

    const handleReset = () => {
        setFile(null);
        setVideoId(null);
        setStatus("idle");
        setResult(null);
        setUploadProgress(0);
        setMessage("");
        setIsAvatarMode(false);
        setMaskingOptions({
            face: true,
            licensePlate: true,
            object: false,
            objectName: ""
        });
        if (pollingRef.current) clearInterval(pollingRef.current);
    };

    return (
        <div className="upload-page-container">

            {/* --- 화면 1: 결과 완료 시 (Completed) --- */}
            {status === "completed" && result ? (
                <div className="section-card result-container" style={{textAlign: 'center'}}>
                    <div className="result-header">
                        <div className="success-icon">🎉</div>
                        <h2>작업이 완료되었습니다!</h2>
                        <p style={{color: '#64748b'}}>AI가 영상을 성공적으로 처리했습니다.</p>
                    </div>

                    <div className="stats-grid">
                        <div className="stat-item total">
                            <span className="stat-label">총 탐지 객체</span>
                            <span className="stat-value">{result.statistics?.totalDetections || 0}</span>
                        </div>
                        <div className="stat-item">
                            <span className="stat-label">🙂 얼굴</span>
                            <span className="stat-value">{result.statistics?.faceCount || 0}</span>
                        </div>
                        <div className="stat-item">
                            <span className="stat-label">🚗 번호판</span>
                            <span className="stat-value">{result.statistics?.licensePlateCount || 0}</span>
                        </div>
                        {result.processingTimeMs && (
                            <div className="stat-item">
                                <span className="stat-label">⏱️ 처리 시간</span>
                                <span className="stat-value">{(result.processingTimeMs / 1000).toFixed(1)}초</span>
                            </div>
                        )}
                    </div>

                    <div className="download-actions">
                        <a href={result.processedDownloadUrl} className="download-card processed" target="_blank" rel="noreferrer">
                            <div className="icon-box"><RiDownloadLine /></div>
                            <div className="download-text-group">
                                <span className="download-title">결과 영상 다운로드</span>
                                <span className="download-desc">마스킹 처리된 파일</span>
                            </div>
                        </a>
                        <a href={result.originalDownloadUrl} className="download-card original" target="_blank" rel="noreferrer">
                            <div className="icon-box"><RiMovieLine /></div>
                            <div className="download-text-group">
                                <span className="download-title">원본 영상 다운로드</span>
                                <span className="download-desc">업로드한 파일</span>
                            </div>
                        </a>
                    </div>

                    <div className="footer-actions">
                        <button onClick={handleReset} className="btn-secondary">
                            <RiRefreshLine style={{marginRight: '6px'}}/> 다른 영상 작업하기
                        </button>
                        {onNavigateToList && (
                            <button onClick={onNavigateToList} className="btn-secondary">
                                <RiFileListLine style={{marginRight: '6px'}}/> 내 보관함 가기
                            </button>
                        )}
                    </div>
                </div>
            ) : (
                /* --- 화면 2: 업로드 및 설정 (Idle / Uploading / Processing / Failed) --- */
                <>
                    {/* 1. 처리 방식 선택 */}
                    <div className="section-card">
                        <div className="section-header">
                            <h3>🛠️ 처리 방식 선택</h3>
                            <p>개인정보를 어떻게 가릴지 선택하세요.</p>
                        </div>
                        <div className="masking-grid">
                            <div className={`masking-card ${!isAvatarMode ? "active" : ""}`} onClick={() => setIsAvatarMode(false)}>
                                <div className="checkbox-indicator">{!isAvatarMode && <RiCheckLine/>}</div>
                                <div className="icon"><RiBlurOffLine /></div><div className="label">블러 (모자이크)</div>
                            </div>
                            <div className={`masking-card ${isAvatarMode ? "active" : ""}`} onClick={() => setIsAvatarMode(true)}>
                                <div className="checkbox-indicator">{isAvatarMode && <RiCheckLine/>}</div>
                                <div className="icon"><RiRobot2Line /></div><div className="label">AI 아바타 변환</div>
                            </div>
                        </div>
                    </div>

                    {/* 2. 마스킹 대상 */}
                    <div className="section-card">
                        <div className="section-header">
                            <h3>🎯 마스킹 대상</h3>
                            <p>영상에서 가리고 싶은 대상을 선택하세요.</p>
                        </div>
                        <div className="masking-grid">
                            <div className={`masking-card ${maskingOptions.face ? "active" : ""}`} onClick={() => toggleOption("face")}>
                                <div className="checkbox-indicator">{maskingOptions.face && <RiCheckLine/>}</div>
                                <div className="icon"><RiUserSmileLine /></div><div className="label">얼굴</div>
                            </div>
                            <div className={`masking-card ${maskingOptions.licensePlate ? "active" : ""}`} onClick={() => toggleOption("licensePlate")}>
                                <div className="checkbox-indicator">{maskingOptions.licensePlate && <RiCheckLine/>}</div>
                                <div className="icon"><RiCarLine /></div><div className="label">번호판</div>
                            </div>
                            <div className="custom-card">
                                <div className={`masking-card ${maskingOptions.object ? "active" : ""}`} onClick={() => toggleOption("object")} style={{width: '100%'}}>
                                    <div className="checkbox-indicator">{maskingOptions.object && <RiCheckLine/>}</div>
                                    <div className="icon"><RiFocus3Line /></div><div className="label">사용자 지정</div>
                                </div>
                                <div className={`custom-input-wrapper ${maskingOptions.object ? "show" : ""}`}>
                                    <input type="text" className="masking-custom-input" placeholder="예: cat, dog..." value={maskingOptions.objectName}
                                           onChange={(e) => setMaskingOptions(prev => ({ ...prev, objectName: e.target.value }))}
                                    />
                                </div>
                            </div>
                        </div>
                    </div>

                    {/* 3. 업로드 영역 (상태에 따라 디자인 변경) */}
                    <div className="section-card">
                        <div className="section-header">
                            <h3>📹 비디오 업로드</h3>
                            <p>MP4, MOV, AVI 형식 지원</p>
                        </div>

                        {/* A. 파일 선택 전 */}
                        {!file && status === 'idle' && (
                            <div
                                className="upload-dropzone"
                                onDragOver={handleDragOver}
                                onDrop={handleDrop}
                                onClick={() => fileInputRef.current?.click()}
                            >
                                <div style={{ pointerEvents: 'none' }}>
                                    <div className="upload-icon"><RiUploadCloud2Line /></div>
                                    <p style={{ fontWeight: 600, color: '#1e293b' }}>클릭하여 업로드하거나 파일을 드래그하세요</p>
                                </div>
                                <input ref={fileInputRef} type="file" accept="video/*" onChange={handleFileSelect} style={{ display: "none" }} />
                            </div>
                        )}

                        {/* B. 파일 선택 후 (미리보기 카드 + 시작 버튼) */}
                        {file && status === 'idle' && (
                            <div>
                                <div className="file-preview-card">
                                    <div className="preview-icon"><RiMovieLine /></div>
                                    <div className="preview-info">
                                        <div className="preview-filename">{file.name}</div>
                                        <div className="preview-size">{(file.size / 1024 / 1024).toFixed(2)} MB</div>
                                    </div>
                                    <button className="btn-change-file" onClick={() => fileInputRef.current?.click()}>
                                        파일 변경
                                    </button>
                                    <input ref={fileInputRef} type="file" accept="video/*" onChange={handleFileSelect} style={{ display: "none" }} />
                                </div>
                                <button onClick={handleUpload} className="btn-primary">
                                    🚀 마스킹 시작하기
                                </button>
                            </div>
                        )}

                        {/* C. 업로드 중 (프로그레스) */}
                        {status === "uploading" && (
                            <div className="processing-card">
                                <div className="spinner-large"></div>
                                <div className="status-text">서버로 전송 중...</div>
                                <div className="status-sub">잠시만 기다려주세요</div>
                                <div className="progress-wrapper">
                                    <div className="progress-info"><span>진행률</span><span>{uploadProgress}%</span></div>
                                    <div className="progress-bar-bg">
                                        <div className="progress-bar-fill" style={{ width: `${uploadProgress}%` }} />
                                    </div>
                                </div>
                            </div>
                        )}

                        {/* D. 처리 중 (스피너) */}
                        {status === "processing" && (
                            <div className="processing-card">
                                <div className="spinner-large"></div>
                                <div className="status-text">{message}</div>
                                <div className="status-sub">AI가 열심히 분석 중입니다...</div>
                            </div>
                        )}

                        {/* E. 실패 (에러) */}
                        {status === "failed" && (
                            <div className="error-alert" style={{background: '#fef2f2', padding: '20px', borderRadius: '12px', border: '1px solid #fecaca', textAlign: 'center'}}>
                                <div style={{fontSize: '2rem', color: '#dc2626', marginBottom: '10px'}}><RiErrorWarningLine/></div>
                                <h4 style={{color: '#991b1b', marginBottom: '5px'}}>작업 실패</h4>
                                <p style={{color: '#b91c1c', fontSize: '0.9rem', marginBottom: '20px'}}>{message}</p>
                                <button className="btn-secondary" onClick={() => { setStatus("idle"); setMessage(""); }}>
                                    다시 시도
                                </button>
                            </div>
                        )}
                    </div>
                </>
            )}
        </div>
    );
}

export default UploadPage;