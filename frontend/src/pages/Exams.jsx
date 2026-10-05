import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api, fmtDate, fmtTime, DEPARTMENTS } from '../api'
import useForm from '../hooks/useForm'
import { useToast } from '../context/ToastContext'

const EMPTY = { code: '', name: '', department: 'CSE', examDate: '', startTime: '09:00', endTime: '12:00' }

const validate = (v) => {
  const e = {}
  if (!v.code.trim()) e.code = 'Exam code is required'
  if (!v.name.trim()) e.name = 'Subject name is required'
  if (!v.examDate) e.examDate = 'Choose the exam date'
  if (!v.startTime) e.startTime = 'Start time is required'
  if (!v.endTime) e.endTime = 'End time is required'
  else if (v.startTime && v.endTime <= v.startTime) e.endTime = 'End time must be after start time'
  return e
}

export default function Exams() {
  const [exams, setExams] = useState([])
  const [open, setOpen] = useState(false)
  const [busy, setBusy] = useState(null)
  const toast = useToast()
  const form = useForm(EMPTY, validate)

  const load = () => api('/exams').then(setExams).catch((e) => toast(e.message, 'err'))
  useEffect(() => { load() }, [])

  const create = form.handleSubmit(async (v) => {
    try {
      await api('/exams', { method: 'POST', body: v })
      toast('Exam scheduled')
      setOpen(false)
      form.reset(EMPTY)
      load()
    } catch (err) { toast(err.message, 'err') }
  })

  const act = async (exam, kind) => {
    const paths = { seats: 'allocate-seats', inv: 'allocate-invigilators' }
    setBusy(exam.id + kind)
    try {
      if (kind === 'reset') {
        if (!window.confirm(`Remove all seats and invigilators of ${exam.code}?`)) return
        await api(`/exams/${exam.id}/allocation`, { method: 'DELETE' })
        toast(`${exam.code} reset`)
      } else if (kind === 'delete') {
        if (!window.confirm(`Delete exam ${exam.code}?`)) return
        await api(`/exams/${exam.id}`, { method: 'DELETE' })
        toast('Exam deleted')
      } else {
        const res = await api(`/exams/${exam.id}/${paths[kind]}`, { method: 'POST' })
        toast(`${exam.code}: ${res.message}`)
      }
      load()
    } catch (err) {
      toast(err.message, 'err')
    } finally { setBusy(null) }
  }

  const { values, errors, handleChange } = form
  const err = (k) => errors[k] && <small className="err">{errors[k]}</small>

  return (
    <div className="page">
      <div className="page-head">
        <h1>Exams and allocation</h1>
        <button className="btn" onClick={() => setOpen(!open)}>{open ? 'Close form' : 'Schedule exam'}</button>
      </div>

      {open && (
        <form className="panel formgrid" onSubmit={create} noValidate>
          <h2>New exam</h2>
          <label className="field"><span>Exam code</span><input name="code" value={values.code} onChange={handleChange} />{err('code')}</label>
          <label className="field"><span>Subject</span><input name="name" value={values.name} onChange={handleChange} />{err('name')}</label>
          <label className="field"><span>Department</span>
            <select name="department" value={values.department} onChange={handleChange}>
              {DEPARTMENTS.map((d) => <option key={d}>{d}</option>)}
              <option value="ALL">ALL (common subject)</option>
            </select>
          </label>
          <label className="field"><span>Date</span><input name="examDate" type="date" value={values.examDate} onChange={handleChange} />{err('examDate')}</label>
          <label className="field"><span>Start</span><input name="startTime" type="time" value={values.startTime} onChange={handleChange} />{err('startTime')}</label>
          <label className="field"><span>End</span><input name="endTime" type="time" value={values.endTime} onChange={handleChange} />{err('endTime')}</label>
          <div className="row"><button className="btn">Schedule exam</button></div>
        </form>
      )}

      <p className="muted">Allocate seats first, then invigilators. Resetting removes both.</p>

      <div className="scroll">
        <table className="table">
          <thead><tr><th>Exam</th><th>Dept</th><th>When</th><th>Students seated</th><th>Invigilators</th><th>Status</th><th>Actions</th></tr></thead>
          <tbody>
            {exams.map((e) => (
              <tr key={e.id}>
                <td><Link to={`/exams/${e.id}`}>{e.code}</Link><br /><span className="muted small">{e.name}</span></td>
                <td>{e.department}</td>
                <td>{fmtDate(e.examDate)}<br /><span className="muted small">{fmtTime(e.startTime)} to {fmtTime(e.endTime)}</span></td>
                <td>{e.seated} / {e.enrolled}</td>
                <td>{e.invigilatorsAssigned} / {e.invigilatorsRequired || '-'}</td>
                <td><span className={`pill pill-${e.status}`}>{e.status}</span></td>
                <td className="actions">
                  <button className="btn btn-small" disabled={busy || e.seated > 0} onClick={() => act(e, 'seats')}>Allocate seats</button>
                  <button className="btn btn-small" disabled={busy || e.seated === 0 || e.invigilatorsAssigned > 0} onClick={() => act(e, 'inv')}>Allocate invigilators</button>
                  <button className="btn btn-small btn-ghost" disabled={busy || e.seated === 0} onClick={() => act(e, 'reset')}>Reset</button>
                  <button className="btn btn-small btn-danger" disabled={busy} onClick={() => act(e, 'delete')}>Delete</button>
                </td>
              </tr>
            ))}
            {exams.length === 0 && <tr><td colSpan="7" className="muted">No exams scheduled.</td></tr>}
          </tbody>
        </table>
      </div>
    </div>
  )
}
