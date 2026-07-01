<template>
  <view class="owner-portal">
    <owner-sidebar current-page="/owner/pages/customerService/ai-chat" />

<view class="main-area">
      <view class="top-bar">
        <view class="breadcrumb">
          <text class="breadcrumb-item">棣栭〉</text>
          <text class="breadcrumb-separator">/</text>
          <text class="breadcrumb-item active">AI 瀹㈡湇</text>
        </view>
      </view>

      <view class="content-area">
        <view class="chat-shell">
          <view class="chat-header">
            <view>
              <text class="chat-title">AI 瀹㈡湇</text>
              <text class="chat-subtitle">鍙挩璇㈠叕鍛婂埗搴︺€佹姤淇祦绋嬨€佸仠杞︾即璐圭瓑绀惧尯鏈嶅姟闂銆?/text>
            </view>
            <view class="header-actions">
              <view class="secondary-btn" @tap="clearConversation">
                <text>娓呯┖璁板綍</text>
              </view>
              <view class="human-btn" @tap="goHumanService">
                <text>杞汉宸?/text>
              </view>
            </view>
          </view>

          <scroll-view class="message-list" scroll-y :scroll-top="scrollTop">
            <view v-for="(message, index) in messages" :key="index" class="message-row" :class="message.role">
              <view class="message-bubble">
                <text class="message-text">{{ message.content }}</text>
                <view v-if="message.cannotAnswer" class="cannot-answer">
                  <text>杩欎釜闂寤鸿鑱旂郴鐗╀笟浜哄伐纭銆?/text>
                  <button class="inline-btn" @tap="goHumanService">杞汉宸ュ鏈?/button>
                </view>
                <view v-if="message.sources && message.sources.length" class="sources">
                  <text class="sources-title">鍙傝€冭祫鏂?/text>
                  <view v-for="source in message.sources" :key="source.sourceId || source.title" class="source-item">
                    <text class="source-title">{{ source.title || '绀惧尯璧勬枡' }}</text>
                    <text class="source-excerpt">{{ source.excerpt || '' }}</text>
                  </view>
                </view>
                <view v-if="message.actions && message.actions.length" class="follow-actions">
                  <text v-for="action in message.actions" :key="action" class="follow-action">{{ action }}</text>
                </view>
              </view>
            </view>
          </scroll-view>

          <view class="quick-questions">
            <view v-for="item in quickQuestions" :key="item" class="quick-chip" @tap="useQuickQuestion(item)">
              <text>{{ item }}</text>
            </view>
          </view>

          <view class="ask-bar">
            <textarea
              v-model="question"
              class="ask-input"
              maxlength="300"
              auto-height
              placeholder="璇疯緭鍏ユ偍鐨勯棶棰?
            />
            <button class="send-btn" :loading="asking" :disabled="!question.trim()" @tap="askQuestion">鍙戦€?/button>
          </view>
          <text class="disclaimer">AI 绛旀浠呬緵鍙傝€冿紝鍏蜂綋鍔炵悊缁撴灉浠ョ墿涓氫汉宸ョ‘璁や负鍑嗐€?/text>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
import { askCustomerService } from '@/api/ai'
import { getAiErrorMessage } from '@/utils/request'
import ownerSidebar from '@/components/owner-sidebar/owner-sidebar'

const DEFAULT_MESSAGES = [
  {
    role: 'assistant',
    content: '鎮ㄥソ锛屾垜鏄ぞ鍖?AI 瀹㈡湇銆傛偍鍙互闂垜鍏憡閫氱煡銆佹姤淇祦绋嬨€佸仠杞︾即璐圭瓑闂銆?
  }
]
const MAX_CACHED_MESSAGES = 60

export default {
  components: { ownerSidebar },
  data() {
    return {
      userName: '',
      communityId: '',
      question: '',
      asking: false,
      scrollTop: 0,
      messages: DEFAULT_MESSAGES.slice(),
      quickQuestions: [
        '灏忓尯鐢靛姩杞﹀湪鍝噷鍏呯數锛?,
        '鍘ㄦ埧婕忔按鎬庝箞鎶ヤ慨锛?,
        '鐗╀笟璐规€庝箞缂寸撼锛?
      ]
    }
  },
  onShow() {
    uni.hideTabBar()
    this.loadUserInfo()
    this.restoreConversation()
  },
  onHide() {
    this.saveConversation()
  },
  onUnload() {
    this.saveConversation()
  },
  methods: {
    getConversationStorageKey() {
      const user = uni.getStorageSync('userInfo') || {}
      const userId = user.id || user.userId || user.username || 'guest'
      return `ownerAiCustomerMessages:${userId}`
    },
    loadUserInfo() {
      const user = uni.getStorageSync('userInfo') || {}
      this.userName = user.username || user.name || '涓氫富'
      this.communityId = user.communityId || user.community_id || ''
    },
    restoreConversation() {
      const cached = uni.getStorageSync(this.getConversationStorageKey())
      if (Array.isArray(cached) && cached.length) {
        this.messages = cached
      } else {
        this.messages = DEFAULT_MESSAGES.slice()
      }
      this.bumpScroll()
    },
    saveConversation() {
      const list = this.messages.slice(-MAX_CACHED_MESSAGES)
      uni.setStorageSync(this.getConversationStorageKey(), list)
    },
    clearConversation() {
      uni.showModal({
        title: '娓呯┖鑱婂ぉ璁板綍',
        content: '纭畾瑕佹竻绌哄綋鍓?AI 瀹㈡湇鑱婂ぉ璁板綍鍚楋紵',
        success: (res) => {
          if (!res.confirm) return
          this.messages = DEFAULT_MESSAGES.slice()
          uni.removeStorageSync(this.getConversationStorageKey())
          this.bumpScroll()
        }
      })
    },
    useQuickQuestion(text) {
      this.question = text
      this.askQuestion()
    },
    async askQuestion() {
      const text = this.question.trim()
      if (!text || this.asking) return

      this.messages.push({ role: 'user', content: text })
      this.saveConversation()
      this.question = ''
      this.asking = true
      this.bumpScroll()

      try {
        const data = await askCustomerService({
          communityId: this.communityId ? Number(this.communityId) : undefined,
          question: text,
          topK: 3
        })

        // ===== 打字机效果：先创建空消息，然后逐字显示 =====
        const fullText = data.answer || '暂时没有找到明确答案，请联系物业客服确认。'
        const typingMsg = {
          role: 'assistant',
          content: '',
          cannotAnswer: Boolean(data.cannotAnswer),
          sources: Array.isArray(data.sources) ? data.sources.slice(0, 3) : [],
          actions: Array.isArray(data.followUpActions) ? data.followUpActions : []
        }
        this.messages.push(typingMsg)
        this.bumpScroll()

        this.typingMessage = typingMsg
        this.typingFullText = fullText
        this.typingIndex = 0

        this.typingTimer = setInterval(() => {
          if (this.typingIndex < this.typingFullText.length) {
            this.typingMessage.content += this.typingFullText[this.typingIndex]
            this.typingIndex++
            if (this.typingIndex % 3 === 0) {
              this.bumpScroll()
            }
          } else {
            clearInterval(this.typingTimer)
            this.typingTimer = null
            this.typingMessage = null
            this.typingFullText = ''
            this.typingIndex = 0
            this.asking = false
            this.bumpScroll()
            this.saveConversation()
          }
        }, this.typingSpeed)

      } catch (error) {
        console.error('业主端 AI 客服问答失败:', error)
        this.messages.push({
          role: 'assistant',
          content: getAiErrorMessage(error),
          cannotAnswer: true
        })
      } finally {
        if (!this.typingTimer) {
          this.asking = false
          this.saveConversation()
          this.bumpScroll()
        }
      }
    },
    bumpScroll() {
      this.$nextTick(() => {
        this.scrollTop += 1000
      })
    },
    goHumanService() {
      uni.navigateTo({ url: '/owner/pages/communityService/complaint' })
    },
    navTo(url) {
      if (!url) return
      const tabbarPages = [
        'owner/pages/index/index',
        'owner/pages/parking/index',
        'owner/pages/topic/index',
        'owner/pages/mine/index'
      ]
      const isTabbar = tabbarPages.some(page => url.includes(page))
      if (isTabbar) {
        uni.switchTab({ url })
      } else {
        uni.navigateTo({ url })
      }
    },
    logout() {
      uni.showModal({
        title: '纭閫€鍑?,
        content: '纭畾瑕侀€€鍑虹櫥褰曞悧锛?,
        success: (res) => {
          if (res.confirm) {
            uni.removeStorageSync('userInfo')
            uni.removeStorageSync('token')
            uni.redirectTo({ url: '/owner/pages/login/login' })
          }
        }
      })
    }
  }
}
</script>

<style scoped>
.owner-portal {
  display: flex;
  width: 100vw;
  height: 100vh;
  background: #f5f7fa;
  overflow: hidden;
}

.sidebar {
  width: 280px;
  height: 100vh;
  background: linear-gradient(180deg, #1e3a8a 0%, #1e40af 100%);
  display: flex;
  flex-direction: column;
  box-shadow: 2px 0 8px rgba(0, 0, 0, 0.1);
}

.sidebar-header {
  padding: 32px 24px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.logo-section,
.nav-item,
.logout-btn,
.top-bar,
.breadcrumb,
.chat-header,
.ask-bar,
.human-btn,
.secondary-btn,
.header-actions {
  display: flex;
  align-items: center;
}

.logo-section {
  gap: 16px;
}

.logo-icon {
  font-size: 48px;
}

.logo-text,
.user-card,
.profile-info,
.message-list,
.sources,
.follow-actions {
  display: flex;
  flex-direction: column;
}

.logo-title {
  font-size: 24px;
  font-weight: 700;
  color: #ffffff;
  line-height: 1.2;
}

.logo-subtitle {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.7);
  margin-top: 4px;
}

.user-card {
  padding: 24px;
  align-items: center;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.user-avatar-large {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 16px;
  border: 3px solid rgba(255, 255, 255, 0.2);
}

.avatar-text {
  font-size: 32px;
  color: #ffffff;
  font-weight: 700;
}

.user-name-large {
  font-size: 18px;
  color: #ffffff;
  font-weight: 600;
  margin-bottom: 8px;
}

.user-role-tag {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.8);
  background: rgba(255, 255, 255, 0.15);
  padding: 4px 16px;
  border-radius: 12px;
}

.nav-menu {
  flex: 1;
  padding: 16px 12px;
  overflow-y: auto;
}

.nav-item {
  padding: 14px 16px;
  margin-bottom: 8px;
  border-radius: 12px;
  color: rgba(255, 255, 255, 0.8);
  cursor: pointer;
  transition: all 0.3s;
}

.nav-item:hover,
.nav-item.active {
  background: rgba(255, 255, 255, 0.15);
  color: #ffffff;
}

.nav-item.active {
  font-weight: 600;
}

.nav-icon {
  font-size: 20px;
  margin-right: 12px;
}

.nav-text {
  flex: 1;
  font-size: 15px;
}

.sidebar-footer {
  padding: 16px 12px;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
}

.logout-btn {
  padding: 14px 16px;
  border-radius: 12px;
  color: rgba(255, 255, 255, 0.8);
  cursor: pointer;
}

.logout-btn:hover {
  background: rgba(255, 255, 255, 0.1);
  color: #ffffff;
}

.logout-icon {
  font-size: 20px;
  margin-right: 12px;
}

.logout-text {
  font-size: 15px;
}

.main-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.top-bar {
  height: 72px;
  background: #ffffff;
  border-bottom: 1px solid #e5e7eb;
  padding: 0 32px;
}

.breadcrumb {
  gap: 8px;
}

.breadcrumb-item {
  font-size: 14px;
  color: #9ca3af;
}

.breadcrumb-item.active {
  color: #374151;
  font-weight: 500;
}

.breadcrumb-separator {
  font-size: 14px;
  color: #d1d5db;
}

.content-area {
  flex: 1;
  overflow: hidden;
  padding: 32px;
}

.chat-shell {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 20px;
  overflow: hidden;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.06);
}

.chat-header {
  justify-content: space-between;
  gap: 24px;
  padding: 24px 28px;
  border-bottom: 1px solid #e5e7eb;
}

.chat-title {
  display: block;
  font-size: 28px;
  color: #111827;
  font-weight: 700;
}

.chat-subtitle {
  display: block;
  margin-top: 6px;
  font-size: 14px;
  color: #6b7280;
}

.human-btn {
  height: 40px;
  padding: 0 18px;
  border-radius: 12px;
  background: #eff6ff;
  color: #2563eb;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
}

.header-actions {
  gap: 10px;
}

.secondary-btn {
  height: 40px;
  padding: 0 18px;
  border-radius: 12px;
  background: #f1f5f9;
  color: #475569;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
}

.message-list {
  flex: 1;
  min-height: 0;
  padding: 24px 28px;
  box-sizing: border-box;
  background: #f8fafc;
}

.message-row {
  display: flex;
  margin-bottom: 18px;
}

.message-row.user {
  justify-content: flex-end;
}

.message-bubble {
  max-width: 72%;
  padding: 16px 18px;
  border-radius: 16px;
  background: #ffffff;
  border: 1px solid #e5e7eb;
}

.message-row.user .message-bubble {
  background: #2563eb;
  color: #ffffff;
  border-color: #2563eb;
}

.message-text {
  display: block;
  font-size: 15px;
  line-height: 1.65;
  white-space: pre-wrap;
}

.cannot-answer {
  margin-top: 12px;
  padding: 12px;
  border-radius: 12px;
  background: #fff7ed;
  color: #9a3412;
  font-size: 13px;
}

.inline-btn {
  margin: 10px 0 0;
  height: 34px;
  line-height: 34px;
  padding: 0 14px;
  border: none;
  border-radius: 10px;
  background: #f97316;
  color: #ffffff;
  font-size: 13px;
}

.sources,
.follow-actions {
  margin-top: 14px;
  gap: 10px;
}

.sources-title {
  font-size: 13px;
  color: #2563eb;
  font-weight: 700;
}

.source-item {
  padding: 12px;
  border-radius: 12px;
  background: #f1f5f9;
}

.source-title {
  display: block;
  font-size: 13px;
  color: #334155;
  font-weight: 700;
}

.source-excerpt,
.follow-action {
  display: block;
  margin-top: 5px;
  font-size: 13px;
  color: #64748b;
  line-height: 1.5;
}

.quick-questions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  padding: 14px 28px 0;
  background: #ffffff;
}

.quick-chip {
  padding: 8px 14px;
  border-radius: 999px;
  background: #f1f5f9;
  color: #475569;
  font-size: 13px;
  cursor: pointer;
}

.ask-bar {
  gap: 14px;
  padding: 16px 28px 10px;
  background: #ffffff;
}

.ask-input {
  flex: 1;
  min-height: 44px;
  max-height: 120px;
  padding: 12px 14px;
  border-radius: 14px;
  border: 1px solid #dbe3ef;
  background: #f8fafc;
  font-size: 15px;
  box-sizing: border-box;
}

.send-btn {
  width: 96px;
  height: 44px;
  line-height: 44px;
  margin: 0;
  border: none;
  border-radius: 14px;
  background: #2563eb;
  color: #ffffff;
  font-size: 15px;
  font-weight: 600;
}

.disclaimer {
  display: block;
  padding: 0 28px 16px;
  font-size: 12px;
  color: #94a3b8;
  background: #ffffff;
}
</style>
