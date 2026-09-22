import { createRouter, createWebHashHistory } from 'vue-router'
import Home from '../views/Home.vue'
import Login from '../views/Login.vue'
import Register from '../views/Register.vue'
import Notes from '../views/Notes.vue'
import Todo from '../views/Todo.vue'
import Dashboard from '../views/Dashboard.vue'
import OAuthCallback from '../views/OAuthCallback.vue'
import Bindings from '../views/Bindings.vue'
import Profile from '../views/Profile.vue'

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
      path: '/todo',
      name: 'todo',
      meta: {
        requireAuth: true // 待办任务需要登录
      },
      component: Todo
    },
    {
      path: '/oauth/callback',
      name: 'oauthCallback',
      component: OAuthCallback
    },
    {
      path: '/dashboard',
      name: 'dashboard',
      meta: {
        requireAuth: true // 数据看板需要登录
      },
      component: Dashboard
    },
    {
      path: '/settings/profile',
      name: 'profile',
      meta: {
        requireAuth: true // 账号设置需要登录
      },
      component: Profile
    },
    {
      path: '/settings/bindings',
      name: 'bindings',
      meta: {
        requireAuth: true // 账号绑定需要登录
      },
      component: Bindings
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
