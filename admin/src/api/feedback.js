import request from './request'

export const feedbackApi = {
  listAll: () => request.get('/admin/feedback'),
  listByUser: (userId) => request.get('/feedback', { params: { userId } }),
  login: (data) => request.post('/auth/login', data)
}
