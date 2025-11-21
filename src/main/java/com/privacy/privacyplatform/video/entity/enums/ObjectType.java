package com.privacy.privacyplatform.video.entity.enums;

public enum ObjectType {
    FACE("얼굴"),
    LICENSE_PLATE("차량 번호판"),
    OBJECT("기타 객체");

    private final String description;

    ObjectType(String description) {
        this.description = description;
    }
    public String getDescription() {
        return description;
    }
}
