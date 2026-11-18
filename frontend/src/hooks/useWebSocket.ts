import { useEffect, useRef, useState } from 'react';
import { Client } from '@stomp/stompjs';

export const useWebSocket = (topic: string | null, onMessage: (message: any) => void) => {
  const [connected, setConnected] = useState(false);
  const clientRef = useRef<Client | null>(null);

  useEffect(() => {
    if (!topic) return;

    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const brokerUrl = `${protocol}//${window.location.host}/ws`;

    const client = new Client({
      brokerURL: brokerUrl,
      reconnectDelay: 3000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
      onConnect: () => {
        setConnected(true);
        client.subscribe(topic, (msg) => {
          if (msg.body) {
            try {
              const payload = JSON.parse(msg.body);
              onMessage(payload);
            } catch (e) {
              // Ignore
            }
          }
        });
      },
      onDisconnect: () => {
        setConnected(false);
      },
      onStompError: (frame) => {
        console.error('Broker reported error: ' + frame.headers['message']);
      },
    });

    client.activate();
    clientRef.current = client;

    return () => {
      client.deactivate();
    };
  }, [topic]);

  return { connected };
};
