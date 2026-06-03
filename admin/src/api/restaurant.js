import request from './request'

export const restaurantApi = {
  save: (data) => request.post('/restaurants', data),
  get: (id) => request.get(`/restaurants/${id}`),
  nearby: (params) => request.get('/restaurants/nearby', { params }),
  addMenuItem: (restaurantId, data) => request.post(`/restaurants/${restaurantId}/menu`, data),
  getMenu: (restaurantId) => request.get(`/restaurants/${restaurantId}/menu`)
}
