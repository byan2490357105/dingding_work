<template>
  <div id="app">
    <!-- 全局独立导航/个人中心：路由跳转时始终显示，登录、注册页隐藏 -->
    <nav-bar v-if="!isAuthPage"></nav-bar>
    <main class="app-main">
      <router-view></router-view>
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
  font-family: 'Avenir', Helvetica, Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  color: #2c3e50;
  min-height: 100vh;
}
.app-main {
  min-height: calc(100vh - 60px);
}
h1, h2 {
  font-weight: normal;
}
ul {
  list-style-type: none;
  padding: 0;
}
li {
  margin: 10px;
}
a {
  color: #42b983;
  text-decoration: none;
}
</style>
