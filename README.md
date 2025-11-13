#  2025 캡스톤 디자인 프로젝트  
## AI 기반 개인정보 탐지·비식별화 플랫폼 (privacy-platform)

---

###  전남대학교 컴퓨터정보통신공학과  
**팀원**
- **강민수 (minddu)** — 백엔드 개발 (Spring Boot, AWS, DB, WebSocket)
- **장인환 (janginh)** — AI 모델 개발 / 비식별화 (Grounding DINO, Stable Diffusion, Gemini API)

---

##  프로젝트 개요
영상/이미지 속 개인정보(얼굴, 차량번호판, 민감 텍스트 등)를 **AI로 탐지**하고 **블러/마스킹 처리**하여 **AWS S3**에 저장합니다.  
탐지 메타데이터는 **MariaDB**에 기록하고, 진행 로그는 **WebSocket**으로 실시간 전송합니다.  
**Gemini API**로 보안 리포트를 자동 생성합니다.

---

##  시스템 구조

```text
[사용자] → [Web UI] → [Spring Boot Backend]
                        ↘
                         ↘ [AI Server (Grounding DINO, Stable Diffusion)]
                            ↘ 결과 이미지/영상 저장 (AWS S3)
                              ↘ 메타데이터 저장 (MariaDB)
                                ↘ LLM 리포트 (Gemini API)


---
# CI/CD Test
