import { createRouter, createWebHashHistory } from 'vue-router'
import Home from '../views/Home.vue'
import Login from '../views/Login.vue'
import Register from '../views/Register.vue'
import Notes from '../views/Notes.vue'

const router = createRouter({
  // 保持与原项目一致的 hash 模式
  history: createWebHashHistory(),
  routes: [
    {
      path: '/',
      name: 'home',
      // 路由元信息 meta
      meta: {
        requireAuth: true // 添加该字段，表示进入这个路由是需要登录的
      },
      component: Home
    },
    {
      path: '/notes',
      name: 'notes',
      meta: {
        requireAuth: true // 随手记需要登录
      },
      component: Notes
    },
    {
      path: '/login',
      name: 'login',
      component: Login
    },
    {
      path: '/register',
      name: 'register',
      component: Register
    }
  ]
})

// 路由全局前置守卫：未登录访问需要登录的页面时跳转到登录页
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  if (to.meta.requireAuth) {
    if (token) {
      next()
    } else {
      next({
        path: '/login',
        query: {
          redirect: to.fullPath
        }
      })
    }
  } else {
    next()
  }
})

export default router
