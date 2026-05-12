<template>
  <div class="app-shell">
    <aside class="app-nav">
      <div class="brand-block">
        <div class="brand-mark" aria-hidden="true">
          <span></span>
        </div>
        <div>
          <h1>AIOps Agent</h1>
          <p>智能运维工作台</p>
        </div>
      </div>

      <nav class="nav-links" aria-label="主导航">
        <RouterLink to="/chat" class="nav-link">
          <span class="nav-icon nav-icon-chat" aria-hidden="true"></span>
          <span>运维对话</span>
        </RouterLink>
        <RouterLink to="/knowledge" class="nav-link">
          <span class="nav-icon nav-icon-knowledge" aria-hidden="true"></span>
          <span>知识库</span>
        </RouterLink>
        <RouterLink to="/models" class="nav-link">
          <span class="nav-icon nav-icon-model" aria-hidden="true"></span>
          <span>模型配置</span>
        </RouterLink>
      </nav>

      <div class="nav-foot">
        <span class="status-dot"></span>
        <span>Workspace ready</span>
      </div>
    </aside>

    <main class="app-main">
      <RouterView />
    </main>
  </div>
</template>

<script setup>
import { RouterLink, RouterView } from 'vue-router'
</script>
