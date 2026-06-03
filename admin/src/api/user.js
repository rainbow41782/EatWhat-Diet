import request from './request'

export const userApi = {
  // 获取用户列表（支持关键词搜索）
  list: (keyword) => request.get('/users', { params: { keyword } }),
  // 获取单个用户
  get: (userId) => request.get(`/users/${userId}`),
  // 修改基本信息
  updateBasic: (userId, data) => request.patch(`/users/${userId}/basic`, data),
}
