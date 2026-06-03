import request from './request'

export const foodApi = {
  list: (keyword, category, nutritionStatus) => request.get('/foods', { params: { keyword, category, nutritionStatus } }),
  get: (id) => request.get(`/foods/${id}`),
  create: (data) => request.post('/foods', data),
  update: (id, data) => request.put(`/foods/${id}`, data),
  remove: (id) => request.delete(`/foods/${id}`),
  nutritionConfigStatus: () => request.get('/admin/foods/nutrition/config/status'),
  pendingNutrition: (params) => request.get('/admin/foods/nutrition/pending', { params }),
  enrichNutritionPreview: (foodItemIds) => request.post(
    '/admin/foods/nutrition/enrich-preview',
    { foodItemIds },
    { timeout: 120000 }
  ),
  startNutritionPreviewJob: (foodItemIds) => request.post('/admin/foods/nutrition/enrich-preview-jobs', { foodItemIds }),
  nutritionPreviewJobStatus: (jobId) => request.get(`/admin/foods/nutrition/enrich-preview-jobs/${jobId}`),
  cancelNutritionPreviewJob: (jobId) => request.post(`/admin/foods/nutrition/enrich-preview-jobs/${jobId}/cancel`),
  applyNutritionSuggestions: (items) => request.post('/admin/foods/nutrition/apply', { items }, { timeout: 60000 })
}
