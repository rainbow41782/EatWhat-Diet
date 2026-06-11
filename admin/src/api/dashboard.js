import request from './request'

export const dashboardApi = {
  stats: () => request.get('/admin/dashboard/stats')
}
