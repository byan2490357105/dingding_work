<template>
  <div id="app">
    <!-- 全局独立导航/个人中心：路由跳转时始终显示，登录、注册页隐藏 -->
    <nav-bar v-if="!isAuthPage"></nav-bar>
    <main class="app-main">
      <router-view v-slot="{ Component }">
        <transition name="route-fade" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </main>
  </div>
</template>

<script>
import NavBar from './components/NavBar.vue'

export default {
  name: 'App',
  components: { NavBar },
  computed: {
    isAuthPage() {
      const path = this.$route.path
      return path === '/login' || path === '/register'
    }
  }
}
</script>

<style>
#app {
  font-family: var(--wb-font);
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  color: var(--wb-text-primary);
  min-height: 100vh;
}
.app-main {
  min-height: calc(100vh - 60px);
}
ul {
  list-style-type: none;
  padding: 0;
}
a {
  color: var(--wb-primary);
  text-decoration: none;
}
</style>
