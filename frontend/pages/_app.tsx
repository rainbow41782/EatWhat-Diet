import '../styles/globals.css'
import type { AppProps } from 'next/app'
import Head from 'next/head'
import { useRouter } from 'next/router'

export default function App({ Component, pageProps }: AppProps) {
  const router = useRouter()

  const getPageTitle = () => {
    const typeLabelMap: Record<string, string> = {
      breakfast: '早餐记录',
      lunch: '午餐记录',
      dinner: '晚餐记录',
      snack: '加餐记录',
    }

    if (router.pathname === '/meals/[type]') {
      const type = typeof router.query.type === 'string' ? router.query.type : ''
      const mealLabel = typeLabelMap[type] || '餐食记录'
      return `${mealLabel} - 食乜`
    }

    const routeTitleMap: Record<string, string> = {
      '/': '首页 - 食乜',
      '/login': '登录 - 食乜',
      '/register': '注册 - 食乜',
      '/onboarding': '新手引导 - 食乜',
      '/profile': '个人中心 - 食乜',
      '/checkin': '每日打卡 - 食乜',
      '/recipes': '食谱推荐 - 食乜',
      '/restaurants/nearby': '附近餐厅 - 食乜',
      '/reports/health': '健康报告 - 食乜',
      '/contact/feedback': '用户反馈 - 食乜',
      '/sign-in-card-demo': '登录卡片演示 - 食乜',
      '/test': '测试页面 - 食乜',
    }

    return routeTitleMap[router.pathname] || '食乜'
  }

  return (
    <>
      <Head>
        <title>{getPageTitle()}</title>
      </Head>
      <Component {...pageProps} />
    </>
  )
}
