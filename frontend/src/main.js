import { createApp } from 'vue'
import { createRouter, createWebHistory } from 'vue-router'
import App from './App.vue'
import ChatPage from './pages/ChatPage.vue'
import KnowledgePage from './pages/KnowledgePage.vue'
import ModelConfigPage from './pages/ModelConfigPage.vue'
import './style.css'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/chat' },
    { path: '/chat', component: ChatPage },
    { path: '/knowledge', component: KnowledgePage },
    { path: '/models', component: ModelConfigPage },
  ],
})

createApp(App).use(router).mount('#app')
