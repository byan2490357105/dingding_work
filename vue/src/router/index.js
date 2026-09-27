import { createRouter, createWebHashHistory } from 'vue-router'

/**
 * 视图组件全部改为动态 import：
 * Vite 会把每个路由打成独立 chunk，首屏只加载当前页面所需代码，
 * 而不是把 Notes / Todo / Dashboard / echarts 全部塞进入口包。
 */
const Home = () => import('../views/Home.vue')
const Login = () => import('../views/Login.vue')
const Register = () => import('../views/Register.vue')
const Notes = () => import('../views/Notes.vue')
const Todo = () => import('../views/Todo.vue')
const Dashboard = () => import('../views/Dashboard.vue')
const OAuthCallback = () => import('../views/OAuthCallback.vue')
const Profile = () => import('../views/Profile.vue')

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
      redirect: () => ({ path: '/settings/profile', query: { tab: 'bindings' } })
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
