<script setup lang="ts">
import HeaderComponent from './HeaderComponent.vue'
</script>

<template>
  <div class="user-task-page">
    <HeaderComponent />
    <div class="content">
      <RouterView />
    </div>
  </div>
</template>

<style scoped>
.user-task-page {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.content {
  flex: 1;
  overflow: auto;
}
</style>
