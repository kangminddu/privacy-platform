import { useState, useRef } from "react";
import { videoAPI } from "../services/api";
import { WebSocketService } from "../services/websocket";
// ✨ 아이콘 추가 (모자이크, 아바타용 아이콘)
import {
    RiUserSmileLine,
    RiCarLine,
    RiFocus3Line,
    RiBlurOffLine,   // 모자이크 아이콘
    RiRobot2Line     // 아바타 아이콘
} from "react-icons/ri";
import "../App.css";

function UploadPage({ onNavigateToList }) {
    const [file, setFile] = useState(null);
    const [videoId, setVideoId] = useState(null);
    const [uploadProgress, setUploadProgress] = useState(0);
    const [processProgress, setProcessProgress] = useState(0);
    const [status, setStatus] = useState("idle");
    const [message, setMessage] = useState("");
    const [result, setResult] = useState(null);

    const fileInputRef = useRef(null);
    const wsService = useRef(null);

    // ✨ [추가] 처리 방식 상태 (false: 모자이크, true: 아바타)
    const [isAvatarMode, setIsAvatarMode] = useState(false);

    const [maskingOptions, setMaskingOptions] = useState({
        face: true,
        licensePlate: true,
        object: false,
        objectName: ""
    });

    // 파일 선택
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

            wsService.current = new WebSocketService();
            await wsService.current.connect(newVideoId, (progress) => {
                setProcessProgress(progress.percentage);
                setMessage(progress.message);

                if (progress.status === "COMPLETED") {
                    setStatus("completed");
                    loadResult(newVideoId);
                } else if (progress.status === "FAILED") {
                    setStatus("failed");
                    setMessage("처리 실패: " + progress.message);
                }
            });

            setStatus("processing");

            // ✨ [수정] API 호출 시 isAvatarMode (true/false) 추가 전송
            await videoAPI.processVideo(newVideoId, s3Key, file.size, {
                face: maskingOptions.face,
                licensePlate: maskingOptions.licensePlate,
                object: maskingOptions.object,
                objectName: maskingOptions.objectName.trim(),
                useAvatar: isAvatarMode // 여기가 추가된 부분!
            });

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
        setProcessProgress(0);
        setMessage("");
        setIsAvatarMode(false); // 리셋 시 모자이크로 초기화

        setMaskingOptions({
            face: true,
            licensePlate: true,
            object: false,
            objectName: ""
        });

        wsService.current?.disconnect();
    };

    return (
        <div className="upload-page-container">

            {/* ✨ 1. 처리 방식 선택 섹션 (새로 추가됨) */}
            <div className="section-card masking-section" style={{ marginBottom: '20px' }}>
                <div className="section-header">
                    <h3>🎨 처리 방식 선택</h3>
                    <p>개인정보를 어떻게 가릴지 선택하세요.</p>
                </div>
                <div className="masking-grid">
                    {/* 모자이크 선택 */}
                    <div
                        className={`masking-card ${!isAvatarMode ? "active" : ""}`}
                        onClick={() => setIsAvatarMode(false)}
                    >
                        <div className="icon"><RiBlurOffLine /></div>
                        <div className="label">블러 (모자이크)</div>
                        <div className="checkbox-indicator"></div>
                    </div>

                    {/* 아바타 선택 */}
                    <div
                        className={`masking-card ${isAvatarMode ? "active" : ""}`}
                        onClick={() => setIsAvatarMode(true)}
                    >
                        <div className="icon"><RiRobot2Line /></div>
                        <div className="label">AI 아바타 변환</div>
                        <div className="checkbox-indicator"></div>
                    </div>
                </div>
            </div>

            {/* 2. 마스킹 대상 옵션 섹션 */}
            <div className="section-card masking-section">
                <div className="section-header">
                    <h3>🎯 마스킹 대상</h3>
                    <p>영상에서 가리고 싶은 대상을 선택하세요.</p>
                </div>

                <div className="masking-grid">
                    {/* 얼굴 */}
                    <div
                        className={`masking-card ${maskingOptions.face ? "active" : ""}`}
                        onClick={() => toggleOption("face")}
                    >
                        <div className="icon"><RiUserSmileLine /></div>
                        <div className="label">얼굴</div>
                        <div className="checkbox-indicator"></div>
                    </div>

                    {/* 번호판 */}
                    <div
                        className={`masking-card ${maskingOptions.licensePlate ? "active" : ""}`}
                        onClick={() => toggleOption("licensePlate")}
                    >
                        <div className="icon"><RiCarLine /></div>
                        <div className="label">번호판</div>
                        <div className="checkbox-indicator"></div>
                    </div>

                    {/* 기타 + 입력칸 */}
                    <div
                        className={`masking-card custom-card ${maskingOptions.object ? "active" : ""}`}
                        onClick={() => toggleOption("object")}
                    >
                        <div className="card-top">
                            <div className="icon"><RiFocus3Line /></div>
                            <div className="label">사용자 지정</div>
                            <div className="checkbox-indicator"></div>
                        </div>

                        <div className={`custom-input-wrapper ${maskingOptions.object ? "show" : ""}`}>
                            <input
                                type="text"
                                className="masking-custom-input"
                                placeholder="예: cat, dog, car"
                                value={maskingOptions.objectName}
                                onChange={(e) =>
                                    setMaskingOptions((prev) => ({
                                        ...prev,
                                        objectName: e.target.value,
                                    }))
                                }
                                onClick={(e) => e.stopPropagation()}
                                disabled={!maskingOptions.object}
                            />
                        </div>
                    </div>
                </div>
            </div>

            {/* 3. 업로드 영역 */}
            {status !== "completed" && (
                <div className="section-card upload-section">
                    <div
                        className={`upload-dropzone ${file ? "has-file" : ""}`}
                        onDragOver={handleDragOver}
                        onDrop={handleDrop}
                        onClick={() => status === "idle" && fileInputRef.current?.click()}
                    >
                        {file ? (
                            <div className="file-info-box">
                                <div className="file-icon">🎬</div>
                                <div className="file-details">
                                    <p className="filename">{file.name}</p>
                                    <p className="filesize">{(file.size / 1024 / 1024).toFixed(2)} MB</p>
                                </div>
                                {status === "idle" && <button className="change-btn">변경</button>}
                            </div>
                        ) : (
                            <div className="empty-dropzone">
                                <div className="upload-icon">☁️</div>
                                <h4>비디오 업로드</h4>
                                <p>파일을 드래그하거나 클릭하여 선택하세요</p>
                                <span className="support-text">MP4, AVI, MOV 지원</span>
                            </div>
                        )}

                        <input
                            ref={fileInputRef}
                            type="file"
                            accept="video/*"
                            onChange={handleFileSelect}
                            style={{ display: "none" }}
                            disabled={status !== "idle"}
                        />
                    </div>

                    {file && status === "idle" && (
                        <div className="action-area">
                            <button onClick={handleUpload} className="btn-primary btn-large btn-animate">
                                🚀 마스킹 시작하기
                            </button>
                        </div>
                    )}

                    {status === "uploading" && (
                        <div className="progress-container">
                            <div className="progress-header">
                                <span>서버로 전송 중...</span>
                                <span>{uploadProgress}%</span>
                            </div>
                            <div className="progress-track">
                                <div className="progress-bar-fill" style={{ width: `${uploadProgress}%` }} />
                            </div>
                        </div>
                    )}

                    {status === "processing" && (
                        <div className="progress-container processing-mode">
                            <div className="progress-header">
                                <span>AI 영상 분석 및 마스킹 중...</span>
                                <span>{processProgress}%</span>
                            </div>
                            <div className="progress-track">
                                <div className="progress-bar-fill processing" style={{ width: `${processProgress}%` }} />
                            </div>
                            <p className="status-message">💡 {message}</p>
                        </div>
                    )}

                    {status === "failed" && (
                        <div className="error-alert">
                            <div className="error-icon">❌</div>
                            <div className="error-content">
                                <h4>작업 실패</h4>
                                <p>{message}</p>
                            </div>
                            <button className="btn-retry" onClick={() => { setStatus("idle"); setMessage(""); }}>
                                다시 시도
                            </button>
                        </div>
                    )}
                </div>
            )}

            {/* 4. 결과 화면 */}
            {status === "completed" && result && (
                <div className="section-card result-container">
                    <div className="result-header">
                        <div className="success-icon">🎉</div>
                        <h2>작업이 완료되었습니다!</h2>
                        <p>AI가 영상을 성공적으로 처리했습니다.</p>
                    </div>

                    <div className="stats-grid">
                        <div className="stat-item total">
                            <span className="stat-label">총 탐지 객체</span>
                            <span className="stat-value">{result.statistics.totalDetections}</span>
                        </div>
                        <div className="stat-item">
                            <span className="stat-label">🙂 얼굴</span>
                            <span className="stat-value">{result.statistics.faceCount}</span>
                        </div>
                        <div className="stat-item">
                            <span className="stat-label">🚗 번호판</span>
                            <span className="stat-value">{result.statistics.licensePlateCount}</span>
                        </div>
                        <div className="stat-item">
                            <span className="stat-label">📈 정확도</span>
                            <span className="stat-value">{(result.statistics.averageConfidence * 100).toFixed(1)}%</span>
                        </div>
                    </div>

                    <div className="download-actions">
                        <a href={result.processedDownloadUrl} className="download-card processed">
                            <span className="icon">✨</span>
                            <div className="text">
                                <strong>완료 영상 다운로드</strong>
                                <span>마스킹 처리된 파일</span>
                            </div>
                        </a>
                        <a href={result.originalDownloadUrl} className="download-card original">
                            <span className="icon">📥</span>
                            <div className="text">
                                <strong>원본 영상 다운로드</strong>
                                <span>업로드한 파일</span>
                            </div>
                        </a>
                    </div>

                    <div className="footer-actions">
                        <button onClick={handleReset} className="btn-text">🔄 다른 영상 작업하기</button>
                        {onNavigateToList && (
                            <button onClick={onNavigateToList} className="btn-secondary">📋 내 보관함 가기</button>
                        )}
                    </div>
                </div>
            )}
        </div>
    );
}

export default UploadPage;