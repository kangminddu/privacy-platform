from flask import Flask, request, jsonify, send_file
from flask_cors import CORS
import cv2
import os
from datetime import datetime

app = Flask(__name__)
CORS(app)

UPLOAD_FOLDER = 'uploads'
RESULT_FOLDER = 'results'
os.makedirs(UPLOAD_FOLDER, exist_ok=True)
os.makedirs(RESULT_FOLDER, exist_ok=True)


def detect_faces_simple(image_path):
    face_cascade = cv2.CascadeClassifier(
        cv2.data.haarcascades + 'haarcascade_frontalface_default.xml'
    )
    
    img = cv2.imread(image_path)
    if img is None:
        raise ValueError("이미지를 읽을 수 없습니다")
    
    gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
    faces = face_cascade.detectMultiScale(gray, 1.1, 5, minSize=(30, 30))
    
    print(f"✅ 탐지된 얼굴: {len(faces)}개")
    
    detections = []
    for i, (x, y, w, h) in enumerate(faces):
        detections.append({
            "id": i,
            "type": "face",
            "boundingBox": {"x": int(x), "y": int(y), "width": int(w), "height": int(h)},
            "confidence": 0.95
        })
        
        roi = img[y:y+h, x:x+w]
        blurred = cv2.GaussianBlur(roi, (99, 99), 30)
        img[y:y+h, x:x+w] = blurred
        cv2.rectangle(img, (x, y), (x+w, y+h), (0, 255, 0), 2)
    
    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
    result_filename = f"masked_{timestamp}.jpg"
    result_path = os.path.join(RESULT_FOLDER, result_filename)
    cv2.imwrite(result_path, img)
    
    return detections, result_filename


@app.route('/health', methods=['GET'])
def health_check():
    return jsonify({"status": "ok", "message": "AI Server is running"})


@app.route('/detect', methods=['POST'])
def detect():
    print("📥 /detect 요청")
    
    if 'file' not in request.files:
        return jsonify({"error": "파일이 없습니다"}), 400
    
    file = request.files['file']
    if file.filename == '':
        return jsonify({"error": "파일명이 비어있습니다"}), 400
    
    try:
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        original_filename = f"original_{timestamp}_{file.filename}"
        upload_path = os.path.join(UPLOAD_FOLDER, original_filename)
        file.save(upload_path)
        
        import time
        start_time = time.time()
        detections, result_filename = detect_faces_simple(upload_path)
        processing_time = time.time() - start_time
        
        print(f"✅ 처리 완료: {processing_time:.2f}초")
        
        return jsonify({
            "status": "success",
            "detections": detections,
            "originalFilename": original_filename,
            "resultFilename": result_filename,
            "downloadUrl": f"/download/{result_filename}",
            "processingTimeSeconds": round(processing_time, 2)
        })
    
    except Exception as e:
        print(f"❌ 에러: {e}")
        return jsonify({"error": str(e)}), 500


@app.route('/download/<filename>', methods=['GET'])
def download(filename):
    file_path = os.path.join(RESULT_FOLDER, filename)
    if not os.path.exists(file_path):
        return jsonify({"error": "파일 없음"}), 404
    return send_file(file_path, mimetype='image/jpeg')


if __name__ == '__main__':
    print("🚀 AI 서버 시작 (포트 5001)")
    app.run(host='0.0.0.0', port=5001, debug=True)