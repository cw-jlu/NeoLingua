# Netty WebSocket 客户端示例

## JavaScript/TypeScript 客户端

### 基础连接示例

```javascript
// 连接参数
const userId = 123;
const meetingId = 456;
const wsUrl = `ws://localhost:8090/ws/meeting?userId=${userId}&meetingId=${meetingId}`;

// 创建 WebSocket 连接
const ws = new WebSocket(wsUrl);

// 连接成功
ws.onopen = () => {
    console.log('WebSocket 连接成功');
    
    // 发送文本消息
    sendTextMessage('大家好！');
};

// 接收消息
ws.onmessage = (event) => {
    const message = JSON.parse(event.data);
    console.log('收到消息:', message);
    
    switch (message.messageType) {
        case 0:  // 文本消息
            handleTextMessage(message);
            break;
        case 1:  // 音频消息
            handleAudioMessage(message);
            break;
        case 2:  // 系统消息
            handleSystemMessage(message);
            break;
        case -1: // 错误消息
            handleErrorMessage(message);
            break;
    }
};

// 连接关闭
ws.onclose = (event) => {
    console.log('WebSocket 连接关闭:', event.code, event.reason);
    // 实现重连逻辑
    setTimeout(() => reconnect(), 3000);
};

// 连接错误
ws.onerror = (error) => {
    console.error('WebSocket 错误:', error);
};

// 发送文本消息
function sendTextMessage(content) {
    const message = {
        messageType: 0,
        content: content,
        audioUrl: ''
    };
    ws.send(JSON.stringify(message));
}

// 发送音频消息
function sendAudioMessage(audioUrl) {
    const message = {
        messageType: 1,
        content: '',
        audioUrl: audioUrl
    };
    ws.send(JSON.stringify(message));
}

// 处理文本消息
function handleTextMessage(message) {
    console.log(`用户 ${message.senderId}: ${message.content}`);
    // 更新 UI
}

// 处理音频消息
function handleAudioMessage(message) {
    console.log(`用户 ${message.senderId} 发送了音频: ${message.audioUrl}`);
    // 播放音频
}

// 处理系统消息
function handleSystemMessage(message) {
    console.log(`系统消息: ${message.content}`);
    // 显示系统通知
}

// 处理错误消息
function handleErrorMessage(message) {
    console.error(`错误: ${message.content}`);
    // 显示错误提示
}

// 重连逻辑
function reconnect() {
    console.log('尝试重新连接...');
    // 重新创建连接
}
```

---

## Vue 3 客户端示例

### Composition API

```vue
<template>
  <div class="meeting-room">
    <div class="status">
      <span :class="{ connected: isConnected }">
        {{ isConnected ? '已连接' : '未连接' }}
      </span>
    </div>
    
    <div class="messages">
      <div v-for="msg in messages" :key="msg.id" class="message">
        <span class="sender">用户 {{ msg.senderId }}:</span>
        <span class="content">{{ msg.content }}</span>
        <audio v-if="msg.audioUrl" :src="msg.audioUrl" controls></audio>
      </div>
    </div>
    
    <div class="input-area">
      <input 
        v-model="inputText" 
        @keyup.enter="sendMessage"
        placeholder="输入消息..."
      />
      <button @click="sendMessage">发送</button>
      <button @click="recordAudio">录音</button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue';

const props = defineProps({
  userId: Number,
  meetingId: Number
});

const ws = ref(null);
const isConnected = ref(false);
const messages = ref([]);
const inputText = ref('');

// 连接 WebSocket
const connect = () => {
  const wsUrl = `ws://localhost:8090/ws/meeting?userId=${props.userId}&meetingId=${props.meetingId}`;
  ws.value = new WebSocket(wsUrl);
  
  ws.value.onopen = () => {
    console.log('WebSocket 连接成功');
    isConnected.value = true;
  };
  
  ws.value.onmessage = (event) => {
    const message = JSON.parse(event.data);
    
    if (message.messageType >= 0) {
      messages.value.push(message);
    } else if (message.messageType === -1) {
      console.error('错误:', message.content);
    }
  };
  
  ws.value.onclose = () => {
    console.log('WebSocket 连接关闭');
    isConnected.value = false;
    // 3秒后重连
    setTimeout(connect, 3000);
  };
  
  ws.value.onerror = (error) => {
    console.error('WebSocket 错误:', error);
  };
};

// 发送消息
const sendMessage = () => {
  if (!inputText.value.trim()) return;
  
  const message = {
    messageType: 0,
    content: inputText.value,
    audioUrl: ''
  };
  
  ws.value.send(JSON.stringify(message));
  inputText.value = '';
};

// 录音（示例）
const recordAudio = () => {
  // 实现录音逻辑
  console.log('开始录音...');
};

// 组件挂载时连接
onMounted(() => {
  connect();
});

// 组件卸载时断开连接
onUnmounted(() => {
  if (ws.value) {
    ws.value.close();
  }
});
</script>

<style scoped>
.meeting-room {
  max-width: 800px;
  margin: 0 auto;
  padding: 20px;
}

.status {
  margin-bottom: 10px;
}

.connected {
  color: green;
}

.messages {
  height: 400px;
  overflow-y: auto;
  border: 1px solid #ccc;
  padding: 10px;
  margin-bottom: 10px;
}

.message {
  margin-bottom: 10px;
}

.input-area {
  display: flex;
  gap: 10px;
}

input {
  flex: 1;
  padding: 8px;
}

button {
  padding: 8px 16px;
}
</style>
```

---

## React 客户端示例

### Hooks 版本

```jsx
import React, { useState, useEffect, useRef } from 'react';

function MeetingRoom({ userId, meetingId }) {
  const [isConnected, setIsConnected] = useState(false);
  const [messages, setMessages] = useState([]);
  const [inputText, setInputText] = useState('');
  const wsRef = useRef(null);

  useEffect(() => {
    // 连接 WebSocket
    const connect = () => {
      const wsUrl = `ws://localhost:8090/ws/meeting?userId=${userId}&meetingId=${meetingId}`;
      const ws = new WebSocket(wsUrl);
      
      ws.onopen = () => {
        console.log('WebSocket 连接成功');
        setIsConnected(true);
      };
      
      ws.onmessage = (event) => {
        const message = JSON.parse(event.data);
        
        if (message.messageType >= 0) {
          setMessages(prev => [...prev, message]);
        } else if (message.messageType === -1) {
          console.error('错误:', message.content);
        }
      };
      
      ws.onclose = () => {
        console.log('WebSocket 连接关闭');
        setIsConnected(false);
        // 3秒后重连
        setTimeout(connect, 3000);
      };
      
      ws.onerror = (error) => {
        console.error('WebSocket 错误:', error);
      };
      
      wsRef.current = ws;
    };
    
    connect();
    
    // 清理函数
    return () => {
      if (wsRef.current) {
        wsRef.current.close();
      }
    };
  }, [userId, meetingId]);

  // 发送消息
  const sendMessage = () => {
    if (!inputText.trim()) return;
    
    const message = {
      messageType: 0,
      content: inputText,
      audioUrl: ''
    };
    
    wsRef.current.send(JSON.stringify(message));
    setInputText('');
  };

  return (
    <div className="meeting-room">
      <div className="status">
        <span className={isConnected ? 'connected' : ''}>
          {isConnected ? '已连接' : '未连接'}
        </span>
      </div>
      
      <div className="messages">
        {messages.map((msg, index) => (
          <div key={index} className="message">
            <span className="sender">用户 {msg.senderId}:</span>
            <span className="content">{msg.content}</span>
            {msg.audioUrl && <audio src={msg.audioUrl} controls />}
          </div>
        ))}
      </div>
      
      <div className="input-area">
        <input
          value={inputText}
          onChange={(e) => setInputText(e.target.value)}
          onKeyPress={(e) => e.key === 'Enter' && sendMessage()}
          placeholder="输入消息..."
        />
        <button onClick={sendMessage}>发送</button>
      </div>
    </div>
  );
}

export default MeetingRoom;
```

---

## 心跳机制示例

### 客户端心跳

```javascript
class MeetingWebSocket {
  constructor(userId, meetingId) {
    this.userId = userId;
    this.meetingId = meetingId;
    this.ws = null;
    this.heartbeatTimer = null;
    this.reconnectTimer = null;
    this.reconnectAttempts = 0;
    this.maxReconnectAttempts = 5;
  }

  connect() {
    const wsUrl = `ws://localhost:8090/ws/meeting?userId=${this.userId}&meetingId=${this.meetingId}`;
    this.ws = new WebSocket(wsUrl);
    
    this.ws.onopen = () => {
      console.log('WebSocket 连接成功');
      this.reconnectAttempts = 0;
      this.startHeartbeat();
    };
    
    this.ws.onmessage = (event) => {
      const message = JSON.parse(event.data);
      this.handleMessage(message);
    };
    
    this.ws.onclose = () => {
      console.log('WebSocket 连接关闭');
      this.stopHeartbeat();
      this.reconnect();
    };
    
    this.ws.onerror = (error) => {
      console.error('WebSocket 错误:', error);
    };
  }

  // 启动心跳
  startHeartbeat() {
    this.heartbeatTimer = setInterval(() => {
      if (this.ws && this.ws.readyState === WebSocket.OPEN) {
        // 发送心跳消息（空消息或特定格式）
        this.ws.send(JSON.stringify({ type: 'ping' }));
      }
    }, 30000); // 每30秒发送一次心跳
  }

  // 停止心跳
  stopHeartbeat() {
    if (this.heartbeatTimer) {
      clearInterval(this.heartbeatTimer);
      this.heartbeatTimer = null;
    }
  }

  // 重连
  reconnect() {
    if (this.reconnectAttempts >= this.maxReconnectAttempts) {
      console.error('达到最大重连次数，停止重连');
      return;
    }
    
    this.reconnectAttempts++;
    const delay = Math.min(1000 * Math.pow(2, this.reconnectAttempts), 30000);
    
    console.log(`${delay}ms 后尝试第 ${this.reconnectAttempts} 次重连...`);
    
    this.reconnectTimer = setTimeout(() => {
      this.connect();
    }, delay);
  }

  // 发送消息
  send(message) {
    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      this.ws.send(JSON.stringify(message));
    } else {
      console.error('WebSocket 未连接');
    }
  }

  // 处理消息
  handleMessage(message) {
    // 实现消息处理逻辑
    console.log('收到消息:', message);
  }

  // 关闭连接
  close() {
    this.stopHeartbeat();
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer);
    }
    if (this.ws) {
      this.ws.close();
    }
  }
}

// 使用示例
const meetingWs = new MeetingWebSocket(123, 456);
meetingWs.connect();

// 发送消息
meetingWs.send({
  messageType: 0,
  content: 'Hello',
  audioUrl: ''
});

// 关闭连接
// meetingWs.close();
```

---

## 音频录制和发送示例

### 使用 MediaRecorder API

```javascript
class AudioRecorder {
  constructor(meetingWs) {
    this.meetingWs = meetingWs;
    this.mediaRecorder = null;
    this.audioChunks = [];
  }

  async startRecording() {
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
      this.mediaRecorder = new MediaRecorder(stream);
      
      this.mediaRecorder.ondataavailable = (event) => {
        this.audioChunks.push(event.data);
      };
      
      this.mediaRecorder.onstop = async () => {
        const audioBlob = new Blob(this.audioChunks, { type: 'audio/webm' });
        this.audioChunks = [];
        
        // 上传音频到服务器
        const audioUrl = await this.uploadAudio(audioBlob);
        
        // 发送音频消息
        this.meetingWs.send({
          messageType: 1,
          content: '',
          audioUrl: audioUrl
        });
      };
      
      this.mediaRecorder.start();
      console.log('开始录音...');
    } catch (error) {
      console.error('录音失败:', error);
    }
  }

  stopRecording() {
    if (this.mediaRecorder && this.mediaRecorder.state !== 'inactive') {
      this.mediaRecorder.stop();
      console.log('停止录音');
    }
  }

  async uploadAudio(audioBlob) {
    const formData = new FormData();
    formData.append('audio', audioBlob, 'recording.webm');
    
    try {
      const response = await fetch('/api/v1/user/meetings/upload-audio', {
        method: 'POST',
        body: formData
      });
      
      const data = await response.json();
      return data.data.url;
    } catch (error) {
      console.error('上传音频失败:', error);
      throw error;
    }
  }
}

// 使用示例
const meetingWs = new MeetingWebSocket(123, 456);
meetingWs.connect();

const audioRecorder = new AudioRecorder(meetingWs);

// 开始录音
document.getElementById('recordBtn').addEventListener('mousedown', () => {
  audioRecorder.startRecording();
});

// 停止录音
document.getElementById('recordBtn').addEventListener('mouseup', () => {
  audioRecorder.stopRecording();
});
```

---

## 错误处理和重试策略

### 完整的错误处理示例

```javascript
class RobustMeetingWebSocket {
  constructor(userId, meetingId, options = {}) {
    this.userId = userId;
    this.meetingId = meetingId;
    this.options = {
      maxReconnectAttempts: 5,
      reconnectDelay: 1000,
      heartbeatInterval: 30000,
      messageTimeout: 10000,
      ...options
    };
    
    this.ws = null;
    this.reconnectAttempts = 0;
    this.messageQueue = [];
    this.pendingMessages = new Map();
    this.messageId = 0;
  }

  connect() {
    return new Promise((resolve, reject) => {
      const wsUrl = `ws://localhost:8090/ws/meeting?userId=${this.userId}&meetingId=${this.meetingId}`;
      this.ws = new WebSocket(wsUrl);
      
      const timeout = setTimeout(() => {
        reject(new Error('连接超时'));
        this.ws.close();
      }, 10000);
      
      this.ws.onopen = () => {
        clearTimeout(timeout);
        console.log('WebSocket 连接成功');
        this.reconnectAttempts = 0;
        this.flushMessageQueue();
        resolve();
      };
      
      this.ws.onmessage = (event) => {
        try {
          const message = JSON.parse(event.data);
          this.handleMessage(message);
        } catch (error) {
          console.error('解析消息失败:', error);
        }
      };
      
      this.ws.onclose = (event) => {
        clearTimeout(timeout);
        console.log('WebSocket 连接关闭:', event.code, event.reason);
        this.handleClose(event);
      };
      
      this.ws.onerror = (error) => {
        console.error('WebSocket 错误:', error);
      };
    });
  }

  send(message) {
    return new Promise((resolve, reject) => {
      if (!this.ws || this.ws.readyState !== WebSocket.OPEN) {
        // 连接未就绪，加入队列
        this.messageQueue.push({ message, resolve, reject });
        return;
      }
      
      const messageId = ++this.messageId;
      const wrappedMessage = { ...message, _id: messageId };
      
      try {
        this.ws.send(JSON.stringify(wrappedMessage));
        
        // 设置超时
        const timeout = setTimeout(() => {
          this.pendingMessages.delete(messageId);
          reject(new Error('消息发送超时'));
        }, this.options.messageTimeout);
        
        this.pendingMessages.set(messageId, { resolve, reject, timeout });
      } catch (error) {
        reject(error);
      }
    });
  }

  handleMessage(message) {
    // 处理消息确认
    if (message._id && this.pendingMessages.has(message._id)) {
      const { resolve, timeout } = this.pendingMessages.get(message._id);
      clearTimeout(timeout);
      this.pendingMessages.delete(message._id);
      resolve(message);
    }
    
    // 处理业务消息
    this.onMessage(message);
  }

  handleClose(event) {
    // 清理待确认消息
    this.pendingMessages.forEach(({ reject, timeout }) => {
      clearTimeout(timeout);
      reject(new Error('连接已关闭'));
    });
    this.pendingMessages.clear();
    
    // 尝试重连
    if (this.reconnectAttempts < this.options.maxReconnectAttempts) {
      this.reconnect();
    } else {
      console.error('达到最大重连次数');
      this.onMaxReconnectAttemptsReached();
    }
  }

  reconnect() {
    this.reconnectAttempts++;
    const delay = this.options.reconnectDelay * Math.pow(2, this.reconnectAttempts - 1);
    
    console.log(`${delay}ms 后尝试第 ${this.reconnectAttempts} 次重连...`);
    
    setTimeout(() => {
      this.connect().catch(error => {
        console.error('重连失败:', error);
      });
    }, delay);
  }

  flushMessageQueue() {
    while (this.messageQueue.length > 0) {
      const { message, resolve, reject } = this.messageQueue.shift();
      this.send(message).then(resolve).catch(reject);
    }
  }

  // 钩子方法，由外部实现
  onMessage(message) {
    console.log('收到消息:', message);
  }

  onMaxReconnectAttemptsReached() {
    console.error('无法重新连接到服务器');
  }

  close() {
    if (this.ws) {
      this.ws.close();
    }
  }
}

// 使用示例
const meetingWs = new RobustMeetingWebSocket(123, 456);

meetingWs.onMessage = (message) => {
  console.log('业务消息:', message);
};

meetingWs.onMaxReconnectAttemptsReached = () => {
  alert('无法连接到服务器，请检查网络');
};

// 连接
meetingWs.connect()
  .then(() => {
    console.log('连接成功');
    
    // 发送消息
    return meetingWs.send({
      messageType: 0,
      content: 'Hello',
      audioUrl: ''
    });
  })
  .then(() => {
    console.log('消息发送成功');
  })
  .catch(error => {
    console.error('操作失败:', error);
  });
```

---

## 测试工具

### 使用 wscat 测试

```bash
# 安装 wscat
npm install -g wscat

# 连接到 WebSocket
wscat -c "ws://localhost:8090/ws/meeting?userId=123&meetingId=456"

# 发送消息
> {"messageType":0,"content":"Hello","audioUrl":""}

# 查看响应
< {"id":1,"meetingId":456,"senderId":123,"messageType":0,"content":"Hello","audioUrl":"","createTime":"2026-03-02T10:00:00"}
```

### 使用 Postman 测试

1. 创建新的 WebSocket 请求
2. URL: `ws://localhost:8090/ws/meeting?userId=123&meetingId=456`
3. 点击 Connect
4. 在消息框中输入 JSON 消息
5. 点击 Send

---

## 常见问题

### Q: 如何处理大量消息？
A: 实现消息分页加载和虚拟滚动。

### Q: 如何保证消息顺序？
A: 服务端已保证消息顺序，客户端按接收顺序显示即可。

### Q: 如何处理网络波动？
A: 实现心跳机制和自动重连，消息队列缓存未发送消息。

### Q: 如何优化音频传输？
A: 使用音频压缩格式（如 Opus），控制录音质量和时长。

---

**更新时间**: 2026-03-02
