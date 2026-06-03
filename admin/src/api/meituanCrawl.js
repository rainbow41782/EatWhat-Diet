import request from './request'

export const meituanCrawlApi = {
  list: (params) => request.get('/admin/meituan-crawl/tasks', { params }),
  createTasks: (tasks) => request.post('/admin/meituan-crawl/tasks', { tasks }),
  run: () => request.post('/admin/meituan-crawl/run'),
  retry: (taskId) => request.post(`/admin/meituan-crawl/tasks/${taskId}/retry`),
  deleteTask: (taskId) => request.delete(`/admin/meituan-crawl/tasks/${taskId}`),
  openBrowser: () => request.post('/admin/meituan-crawl/browser/open'),
  browserStatus: () => request.get('/admin/meituan-crawl/browser/status'),
  startCapture: () => request.post('/admin/meituan-crawl/capture/start'),
  stopCapture: () => request.post('/admin/meituan-crawl/capture/stop'),
  captureStatus: () => request.get('/admin/meituan-crawl/capture/status'),
  capturePreview: () => request.get('/admin/meituan-crawl/capture/preview'),
  importLatestCapture: () => request.post('/admin/meituan-crawl/capture/import-latest'),
  captures: () => request.get('/admin/meituan-crawl/captures'),
  capturePreviewByKey: (captureKey) => request.get(`/admin/meituan-crawl/captures/${encodeURIComponent(captureKey)}/preview`),
  importCapture: (captureKey) => request.post(`/admin/meituan-crawl/captures/${encodeURIComponent(captureKey)}/import`),
  deleteCapture: (captureKey) => request.delete(`/admin/meituan-crawl/captures/${encodeURIComponent(captureKey)}`),
  cleanupPreview: () => request.post('/admin/meituan-crawl/cleanup-preview'),
  cleanupBadData: () => request.post('/admin/meituan-crawl/cleanup-bad-data')
}
