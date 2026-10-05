import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import useForm from '../hooks/useForm'
import { useAuth, homePathFor } from '../context/AuthContext'
import { useToast } from '../context/ToastContext'
import { DEPARTMENTS } from '../api'

const emailOk = (v) => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(v)

const validateLogin = (v) => {
  const e = {}
  if (!v.email.trim()) e.email = 'Email is required'
  else if (!emailOk(v.email)) e.email = 'Enter a valid email address'
  if (!v.password) e.password = 'Password is required'
  return e
}

const validateRegister = (v) => {
  const e = {}
  if (v.name.trim().length < 3) e.name = 'Name must be at least 3 characters'
  if (!v.email.trim()) e.email = 'Email is required'
  else if (!emailOk(v.email)) e.email = 'Enter a valid email address'
  if (v.password.length < 6) e.password = 'Password must be at least 6 characters'
  if (v.confirm !== v.password) e.confirm = 'Passwords do not match'
  if (!v.department) e.department = 'Choose a department'
  if (v.role === 'STUDENT' && !v.rollNo.trim()) e.rollNo = 'Roll number is required for students'
  return e
}

const DEMOS = [
  ['Admin', 'admin@examguard.com', 'admin123'],
  ['Faculty', 'isha@faculty.com', 'faculty123'],
  ['Student', 'cse001@student.com', 'student123'],
]

function Field({ label, error, children }) {
  return (
    <label className="field">
      <span>{label}</span>
      {children}
      {error && <small className="err">{error}</small>}
    </label>
  )
}

export default function Login() {
  const [mode, setMode] = useState('login')
  const [busy, setBusy] = useState(false)
  const { login, register } = useAuth()
  const toast = useToast()
  const navigate = useNavigate()

  const lf = useForm({ email: '', password: '' }, validateLogin)
  const rf = useForm({ name: '', email: '', password: '', confirm: '', role: 'STUDENT', department: '', rollNo: '' }, validateRegister)

  const run = async (fn) => {
    setBusy(true)
    try {
      const user = await fn()
      toast(`Welcome, ${user.name}`)
      navigate(homePathFor(user.role))
    } catch (err) {
      toast(err.message, 'err')
    } finally {
      setBusy(false)
    }
  }

  const submitLogin = lf.handleSubmit((v) => run(() => login(v.email.trim(), v.password)))
  const submitRegister = rf.handleSubmit((v) =>
    run(() => register({
      name: v.name, email: v.email, password: v.password, department: v.department,
      role: v.role, rollNo: v.role === 'STUDENT' ? v.rollNo : null,
    })))

  return (
    <div className="auth">
      <div className="tabs" role="tablist">
        <button role="tab" aria-selected={mode === 'login'} className={mode === 'login' ? 'tab on' : 'tab'} onClick={() => setMode('login')}>Log in</button>
        <button role="tab" aria-selected={mode === 'register'} className={mode === 'register' ? 'tab on' : 'tab'} onClick={() => setMode('register')}>Register</button>
      </div>

      {mode === 'login' ? (
        <form onSubmit={submitLogin} noValidate>
          <Field label="Email" error={lf.errors.email}>
            <input name="email" type="email" value={lf.values.email} onChange={lf.handleChange} autoComplete="username" />
          </Field>
          <Field label="Password" error={lf.errors.password}>
            <input name="password" type="password" value={lf.values.password} onChange={lf.handleChange} autoComplete="current-password" />
          </Field>
          <button className="btn wide" disabled={busy}>{busy ? 'Signing in...' : 'Log in'}</button>
          <div className="demo-row">
            <span>Demo accounts</span>
            {DEMOS.map(([label, email, password]) => (
              <button type="button" key={label} className="chip" onClick={() => lf.reset({ email, password })}>{label}</button>
            ))}
          </div>
        </form>
      ) : (
        <form onSubmit={submitRegister} noValidate>
          <Field label="I am a" error={rf.errors.role}>
            <select name="role" value={rf.values.role} onChange={rf.handleChange}>
              <option value="STUDENT">Student</option>
              <option value="FACULTY">Faculty member</option>
            </select>
          </Field>
          <Field label="Full name" error={rf.errors.name}>
            <input name="name" value={rf.values.name} onChange={rf.handleChange} />
          </Field>
          <Field label="Email" error={rf.errors.email}>
            <input name="email" type="email" value={rf.values.email} onChange={rf.handleChange} />
          </Field>
          <Field label="Department" error={rf.errors.department}>
            <select name="department" value={rf.values.department} onChange={rf.handleChange}>
              <option value="">Choose...</option>
              {DEPARTMENTS.map((d) => <option key={d}>{d}</option>)}
            </select>
          </Field>
          {rf.values.role === 'STUDENT' && (
            <Field label="Roll number (for example CSE025)" error={rf.errors.rollNo}>
              <input name="rollNo" value={rf.values.rollNo} onChange={rf.handleChange} />
            </Field>
          )}
          <Field label="Password" error={rf.errors.password}>
            <input name="password" type="password" value={rf.values.password} onChange={rf.handleChange} autoComplete="new-password" />
          </Field>
          <Field label="Confirm password" error={rf.errors.confirm}>
            <input name="confirm" type="password" value={rf.values.confirm} onChange={rf.handleChange} autoComplete="new-password" />
          </Field>
          <button className="btn wide" disabled={busy}>{busy ? 'Creating...' : 'Create account'}</button>
        </form>
      )}
    </div>
  )
}
