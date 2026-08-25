import axios from 'axios'

const baseURL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'

const api = axios.create({
  baseURL,
  headers: { 'Content-Type': 'application/json' },
  timeout: 30000,
})

/**
 * Normalizes every failure (network error, validation error, 404, 500, ...)
 * into a single shape { status, code, message } so components never have to
 * branch on axios internals. Backend error bodies already look like
 * { timestamp, status, error, message, path } (see GlobalExceptionHandler) -
 * we just lift `message`/`error` out and always fall back to something
 * readable if the backend is unreachable entirely.
 */
api.interceptors.response.use(
  (response) => response,
  (error) => {
    let normalized = {
      status: null,
      code: 'NETWORK_ERROR',
      message: 'Unable to reach the server. Check your connection and try again.',
    }

    if (error.response) {
      const { status, data } = error.response
      normalized = {
        status,
        code: data?.error || `HTTP_${status}`,
        message: data?.message || defaultMessageForStatus(status),
      }
    } else if (error.request) {
      normalized = {
        status: null,
        code: 'NETWORK_ERROR',
        message: 'No response from the server. It may be offline.',
      }
    }

    return Promise.reject(normalized)
  }
)

function defaultMessageForStatus(status) {
  switch (status) {
    case 400:
      return 'The request was invalid.'
    case 401:
      return 'You are not authorized to perform this action.'
    case 404:
      return 'The requested resource was not found.'
    case 409:
      return 'This action conflicts with the current state of the resource.'
    case 500:
      return 'An unexpected server error occurred. Please try again.'
    default:
      return 'Something went wrong. Please try again.'
  }
}

// ---------- Emails ----------
export const getEmails = (params = {}) => api.get('/emails', { params }).then((r) => r.data)
export const getEmail = (id) => api.get(`/emails/${id}`).then((r) => r.data)
export const getEmailClassification = (id) =>
  api.get(`/emails/${id}/classification`).then((r) => r.data)
export const getEmailActions = (id) => api.get(`/emails/${id}/actions`).then((r) => r.data)
export const getEmailAudit = (id) => api.get(`/emails/${id}/audit`).then((r) => r.data)
export const processEmail = (id) => api.post(`/emails/${id}/process`).then((r) => r.data)
export const processAllEmails = () => api.post('/emails/process-all').then((r) => r.data)
export const reviewEmail = (id, intent) =>
  api.post(`/emails/${id}/review`, { intent }).then((r) => r.data)

// ---------- Dashboard ----------
export const getDashboardStats = () => api.get('/dashboard/stats').then((r) => r.data)

// ---------- Invoices / Tasks / Reply Drafts ----------
export const getInvoices = () => api.get('/invoices').then((r) => r.data)
export const getTasks = () => api.get('/tasks').then((r) => r.data)
export const getReplyDrafts = () => api.get('/reply-drafts').then((r) => r.data)

export default api
