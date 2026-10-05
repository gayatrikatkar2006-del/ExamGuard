import { createContext, useContext, useState } from 'react'
import { api } from '../api'

const AuthContext = createContext(null)
export const useAuth = () => useContext(AuthContext)

export const homePathFor = (role) =>
  role === 'ADMIN' ? '/admin' : role === 'FACULTY' ? '/faculty' : '/student'

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    try { return JSON.parse(localStorage.getItem('eg_user')) } catch { return null }
  })

  const save = (res) => {
    localStorage.setItem('eg_token', res.token)
    localStorage.setItem('eg_user', JSON.stringify(res.user))
    setUser(res.user)
    return res.user
  }

  const login = async (email, password) =>
    save(await api('/auth/login', { method: 'POST', body: { email, password }, auth: false }))

  const register = async (form) =>
    save(await api('/auth/register', { method: 'POST', body: form, auth: false }))

  const logout = () => {
    localStorage.removeItem('eg_token')
    localStorage.removeItem('eg_user')
    setUser(null)
  }

  return <AuthContext.Provider value={{ user, login, register, logout }}>{children}</AuthContext.Provider>
}
