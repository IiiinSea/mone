<template>
  <div class="chat-panel">
    <div class="chat-panel-body">
      <div id="scrollRef" ref="messageContainer" style="overflow: auto; height: 100%">
        <Message
          v-for="(item, index) of conversions"
          :avatarUrl="''"
          :avatar="item.avatar"
          :username="item.name"
          :key="index"
          :date-time="item.dateTime"
          :text="item.text"
          :inversion="item.inversion"
          :error="item.error"
          :textType="item.textType"
          :loading="item.loading"
          :show-cursor="item.showCursor"
          :type="item.type"
          :show-operate="item.isShowOperate"
          :voice-setting="{}"
          :class="[{ 'message-item-question': item.type === 'question' }]"
          @onTryAgain="tryAgain"
          :flowData="{}"
          language=""
          :multimodal="item.multimodal"
          :imgList="item.imgList"
        />
      </div>
    </div>
    <div class="chat-panel-footer">
      <div class="footer-opts">
        <el-popover width="90px" popper-class="footer-clear-popper">
          <div class="clear-tip">清空会话</div>
          <template #reference>
            <el-button
              style="margin-right: 10px"
              @click="clearMessages"
              :icon="Delete"
              circle
            ></el-button>
          </template>
        </el-popover>
        <CommonTextarea
          ref="inputRef"
          v-model="messageInput"
          class="flex-1"
          :placeholder="placeholder"
          @enterFn="sendMessage"
          :disabled="loading"
          :showImg="showImg"
          size="large"
        ></CommonTextarea>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick } from 'vue'
import { ElMessage, type UploadUserFile } from 'element-plus'
import { Delete } from '@element-plus/icons-vue'
import { Message } from '@/components/common-message'
import CommonTextarea from '@/components/common-textarea/CommonTextarea.vue'

interface ChatMessage {
  multimodal?: number
  type?: string
  name?: string
  text: string
  msgType: string // 标识该message的类别，history要用到。
  dateTime?: string
  inversion: boolean
  aiModel?: string
  avatar?: string
  error?: boolean
  textType?: string
  loading?: boolean
  showCursor?: boolean
  isShowOperate?: boolean
  flowData?: Object
  //  如果上一条是bot Message 则新发送时需要加一个Type
  isBotMessage?: Boolean
  imgList?: UploadUserFile[]
}

// 格式化时间YYYY-MM-DD HH:mm:ss
const formatTime = (timestamp: number) => {
  const date = new Date(timestamp)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  const seconds = String(date.getSeconds()).padStart(2, '0')
  return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`
}

let lastConversionRes: any | null = null;

const _botStreamBegin = "BOT_STREAM_BEGIN";
const _botStreamResult = "BOT_STREAM_RESULT";
const _botStreamEvent = "BOT_STREAM_EVENT";

const placeholder = ref('请输入消息')
const showImg = ref(false)
const loading = ref(false)
const messageInput = ref('')
const isSending = ref(false)
const messageContainer = ref<HTMLElement | null>(null)
const conversions = ref<ChatMessage[]>([])
const inputRef = ref<{ $el: HTMLElement } | null>(null)

// 滚动到最新消息
const scrollToBottom = async () => {
  await nextTick()
  if (messageContainer.value) {
    messageContainer.value.scrollTop = messageContainer.value.scrollHeight
  }
}

const tryAgain = () => {
  console.log('tryAgain')
}

const clearMessages = () => {
  conversions.value = []
  chrome.runtime.sendMessage({ type: 'clearMessageHistory' })
}

const sendMessage = async (msg: string = "") => {
  // 获取粘贴的图片数据
  const imgList = []
  if (inputRef.value?.pasteFileList?.length) {
    for (const file of inputRef.value.pasteFileList) {
      // 将图片URL转换为base64
      try {
        const response = await fetch(file.url)
        const blob = await response.blob()
        const reader = new FileReader()
        const base64 = await new Promise((resolve) => {
          reader.onloadend = () => resolve(reader.result)
          reader.readAsDataURL(blob)
        })
        imgList.push(base64)
      } catch (error) {
        console.error('转换图片失败:', error)
      }
    }
  }

  const message = messageInput.value.trim() || msg.trim()

  if ((!message && !imgList.length) || isSending.value) return

  // 添加用户消息
  conversions.value.push({
    type: 'user',
    text: message,
    dateTime: formatTime(Date.now()),
    msgType: 'chat',
    inversion: true,
    avatar: '',
    name: '用户',
    imgList: imgList?.length ? [...inputRef.value?.pasteFileList] : []
  })

  messageInput.value = ''
  isSending.value = true
  await scrollToBottom()

  try {
    // 发送消息到后台
    chrome.runtime.sendMessage({
      type: 'sendWebSocketMessage',
      text: JSON.stringify({
        text: message,
        img: imgList
      })
    }, response => {
      if (response.success) {
        if (inputRef.value) {
          inputRef.value.pasteFileList = []
        }
      } else {
        ElMessage.error('发送消息失败')
      }
      isSending.value = false
    })
  } catch (error) {
    ElMessage.error('发送消息失败')
    isSending.value = false
  }
}

const updateStreamLastConversion = async (text: string, user: any) => {
  const res = lastConversionRes;
  res.text = res.text + text;
  res.loading = false;
  res.isBotMessage = true;
  if (user) {
    res.name = user.name;
    res.fontColor = user.fontColor;
  }
  conversions.value = [...conversions.value];
  await scrollToBottom();
};

const onMessage = function (message: {
  roleId: string,
  roleName: string,
  roleType: string,
  content: string,
  messageType: string,
  message?: any
}) {
    const messageType = message.messageType;
    if (_botStreamBegin == messageType) {
      if (!lastConversionRes) {
        lastConversionRes = {
          text: "",
          textType: "",
          msgType: "ASSISTANT",
          inversion: false,
          avatar: "",
          aiModel: "",
          loading: true,
          dateTime: new Date().toLocaleString(),
          showCursor: false,
          isShowOperate: true,
          fontColor: "",
          // name: message.roleName,
        };
        conversions.value.push(lastConversionRes);
      }
      // 开始目前不做处理
      const res = lastConversionRes;
      res.text = "";
      res.loading = false;
      res.isBotMessage = true;
      conversions.value = [...conversions.value];
    } else if (_botStreamEvent == messageType) {
      updateStreamLastConversion(message.content, null);
    } else if (_botStreamResult == messageType) {
      // 结束, 如果有message，在末尾在展示
      if (message.message) {
        const conversionRes = { ...lastConversionRes };
        if (typeof message.message == "string") {
          conversionRes.text = message.message;
        } else {
          let text = JSON.stringify(message.message);
          if (message.message?.display) {
            const key = message.message?.display;
            text =
              typeof message.message[key] == "string"
                ? message.message[key]
                : JSON.stringify(message.message[key]);
          }
          conversionRes.text = text;
        }
        conversions.value = [...conversions.value, conversionRes];
      } else {
        const res = lastConversionRes;
        res.loading = false;
        res.isBotMessage = true;
        conversions.value = [...conversions.value];
        lastConversionRes = null;
      }
    }
  };

const onAction = function (action: {
  type: string,
  data: string
}) {
  conversions.value.push({
    type: 'assistant',
    text: action.data,
    dateTime: formatTime(Date.now()),
    msgType: 'md',
    inversion: false
  })
}

// 生命周期钩子
onMounted(() => {
  // 监听来自 background script 的消息
  chrome.runtime.onMessage.addListener((message: {
    type: string,
    message: {
      type: 'json',
      data: {
        roleId: string,
        roleName: string,
        roleType: string,
        content: string,
        messageType: string,
        data?: string
        type: 'chat'
      } | {
        type: 'action',
        data: string
      },
      timestamp: string
    } | {
      type: 'text',
      message: string
    }
  }, sender, sendResponse) => {
    if (message.type === 'newWebSocketMessage') {
      console.log('message', message)
      if (message.message.type === 'json') {
        if (message.message.data.type === 'chat') {
          onMessage(message.message.data)
        } else if (message.message.data.type === 'action') {
          onAction(message.message.data)
        } else {
          console.error('未知消息类型数据:' + message.message.data)
        }
      } else if (message.message.type === 'text') {
        conversions.value.push({
          type: 'assistant',
          text: message.message.message,
          dateTime: formatTime(Date.now()),
          msgType: 'md',
          inversion: true
        })
      } else {
        console.error('未知消息类型:' + message.message)
      }
      scrollToBottom()
    }
  })

  // 获取历史消息
  chrome.runtime.sendMessage({ type: 'getMessageHistory' }, (response) => {
    if (response.messages) {
      conversions.value = response.messages
      scrollToBottom()
    }
  })
})
</script>

<style scoped lang="scss">
.chat-panel {
  display: flex;
  flex-direction: column;
  width: 100%;
  height: 100%;
  padding: 20px;

  &-header {
    display: flex;
    flex-direction: column;
    justify-content: center;
    justify-items: center;
    flex-grow: 0;
    flex-shrink: 0;

    &-icon {
      display: flex;
      padding-bottom: 16px;
      justify-content: center;
      justify-items: center;
    }

    &-name {
      display: flex;
      padding-bottom: 16px;
      justify-content: center;
      justify-items: center;
    }

    &-desc {
      display: flex;
      padding-bottom: 16px;
      justify-content: center;
    }
  }

  &-body {
    overflow: hidden;
    flex: 1;
  }

  &-footer {
    display: flex;
    flex-direction: column;
    flex-shrink: 0;

    .footer-opts {
      display: flex;
      align-items: center;
    }
  }
}
.footer-clear-popper {
  min-width: 90px;
  .clear-tip {
    text-align: center !important;
  }
}
.message-item-question {
  margin-top: -1.5rem;
  cursor: pointer !important;
  opacity: 0.6;
}
</style>
