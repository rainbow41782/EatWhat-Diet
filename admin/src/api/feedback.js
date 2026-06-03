import request from './request'

export const feedbackApi = {
  // 按用户 id 查询反馈（管理端用）
  listByUser: (userId) => request.get('/feedback', { params: { userId } }),
  // 管理端登录（复用用户认证接口）
  login: (data) => request.post('/auth/login', data)
}
