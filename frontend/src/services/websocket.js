import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

class WebSocketService {
  constructor() {
    this.client = null;
    this.isConnected = false;
    this.subscriptions = new Map();
  }

  connect(userEmail, onNotification, onConnect, onDisconnect) {
    if (this.isConnected) {
      console.log('WebSocket already connected');
      return;
    }

    this.client = new Client({
      webSocketFactory: () => new SockJS('http://localhost:8080/api/ws'),
      
      connectHeaders: {},
      
      debug: (str) => {
        console.log('STOMP Debug:', str);
      },
      
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,

      onConnect: (frame) => {
        console.log('WebSocket Connected:', frame);
        this.isConnected = true;
        
        // Subscribe to user's notification topic
        const subscription = this.client.subscribe(
          `/topic/notifications/${userEmail}`,
          (message) => {
            try {
              const notification = JSON.parse(message.body);
              console.log('Received notification:', notification);
              onNotification(notification);
            } catch (error) {
              console.error('Error parsing notification:', error);
            }
          }
        );
        
        this.subscriptions.set(userEmail, subscription);
        
        if (onConnect) onConnect();
      },

      onStompError: (frame) => {
        console.error('STOMP Error:', frame);
        this.isConnected = false;
        if (onDisconnect) onDisconnect();
      },

      onWebSocketClose: () => {
        console.log('WebSocket connection closed');
        this.isConnected = false;
        if (onDisconnect) onDisconnect();
      },
    });

    this.client.activate();
  }

  disconnect() {
    if (this.client) {
      // Unsubscribe all
      this.subscriptions.forEach((subscription) => {
        subscription.unsubscribe();
      });
      this.subscriptions.clear();
      
      this.client.deactivate();
      this.isConnected = false;
      console.log('WebSocket disconnected');
    }
  }

  isConnectedStatus() {
    return this.isConnected;
  }
}

export default new WebSocketService();
