// One small wrapper around fetch: adds the JWT, parses JSON and turns errors into exceptions.
export async function api(path, { method = 'GET', body, auth = true } = {}) {
  const token = localStorage.getItem('eg_token')
  const headers = { 'Content-Type': 'application/json' }
  if (auth && token) headers.Authorization = `Bearer ${token}`

  let res
  try {
    res = await fetch('/api' + path, { method, headers, body: body ? JSON.stringify(body) : undefined })
  } catch {
    throw new Error('Cannot reach the server. Is the backend running?')
  }

  let data = null
  try { data = await res.json() } catch { /* empty body (204) */ }

  if (res.status === 401 && auth) {
    localStorage.removeItem('eg_token')
    localStorage.removeItem('eg_user')
    window.location.hash = '#/login'
    window.location.reload()
  }
  if (!res.ok) {
    const err = new Error((data && data.error) || `Request failed (${res.status})`)
    err.status = res.status
    err.fields = data && data.fields
    throw err
  }
  return data
}

export const DEPARTMENTS = ['CSE', 'ECE', 'MECH', 'AIML']

export const fmtDate = (d) =>
  new Date(d + 'T00:00:00').toLocaleDateString('en-IN', { weekday: 'short', day: 'numeric', month: 'short', year: 'numeric' })

export const fmtTime = (t) => t.slice(0, 5)
