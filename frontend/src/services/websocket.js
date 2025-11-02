import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

export class WebSocketService {
    constructor() {
        this.client = null;
        this.connected = false;
    }

    connect(videoId, onProgress) {
        return new Promise((resolve, reject) => {
            this.client = new Client({
                webSocketFactory: () => new SockJS('http://localhost:8080/ws'),
                onConnect: () => {
                    console.log('✅ WebSocket 연결 성공!');
                    this.connected = true;

                    // 진행 상황 구독
                    this.client.subscribe(`/topic/progress/${videoId}`, (message) => {
                        const progress = JSON.parse(message.body);
                        console.log('📡 Progress:', progress);
                        onProgress && onProgress(progress);
                    });

                    resolve();
                },
                onStompError: (error) => {
                    console.error('❌ WebSocket 에러:', error);
                    this.connected = false;
                    reject(error);
                },
                debug: (str) => {
                    console.log('🔍 STOMP:', str);
                },
            });

            this.client.activate();
        });
    }

    disconnect() {
        if (this.client && this.connected) {
            this.client.deactivate();
            this.connected = false;
            console.log('🔌 WebSocket 연결 종료');
        }
    }
}