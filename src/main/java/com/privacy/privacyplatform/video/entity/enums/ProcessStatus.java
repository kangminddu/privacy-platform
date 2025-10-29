package com.privacy.privacyplatform.video.entity.enums;

public enum ProcessStatus {
    UPLOADED("업로드 완료"),
    PROCESSING("처리 중"),
    COMPLETED("처리 완료"),
    FAILED("처리 실패");

    private final String description;

    ProcessStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}