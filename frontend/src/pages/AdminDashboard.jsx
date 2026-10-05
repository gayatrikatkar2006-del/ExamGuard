import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api, fmtDate, fmtTime } from '../api'

function Bars({ items, max }) {
  const top = max || Math.max(1, ...items.map((i) => i.value))
  return (
    <ul className="bars">
      {items.map((i) => (
        <li key={i.label}>
          <span className="bar-label">{i.label}</span>
          <span className="bar-track"><span className="bar-fill" style={{ width: `${(i.value / top) * 100}%` }} /></span>
          <span className="bar-value">{i.value}</span>
        </li>
      ))}
    </ul>
  )
}

export default function AdminDashboard() {
  const [d, setD] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => { api('/dashboard/admin').then(setD).catch((e) => setError(e.message)) }, [])

  if (error) return <p className="err">{error}</p>
  if (!d) return <p className="muted">Loading...</p>

  const stats = [
    ['Students', d.totalStudents], ['Faculty', d.totalFaculty], ['Halls', d.totalHalls],
    ['Usable seats', d.usableCapacity], ['Exams', d.totalExams],
  ]

  return (
    <div className="page">
      <h1>Admin dashboard</h1>
      <div className="stats">
        {stats.map(([label, value]) => (
          <div key={label} className="stat"><strong>{value}</strong><span>{label}</span></div>
        ))}
      </div>

      <div className="pipeline">
        <span className="pill pill-DRAFT">{d.draftExams} waiting for seats</span>
        <span className="pill pill-SEATED">{d.seatedExams} waiting for invigilators</span>
        <span className="pill pill-READY">{d.readyExams} ready</span>
      </div>

      <div className="two">
        <section className="panel">
          <h2>Students by department</h2>
          <Bars items={Object.entries(d.studentsByDepartment).map(([label, value]) => ({ label, value }))} />
        </section>
        <section className="panel">
          <h2>Invigilation duties per faculty</h2>
          <Bars max={6} items={d.dutyLoad.map((f) => ({ label: `${f.faculty} (${f.department})`, value: f.duties }))} />
          <p className="muted small">The bar scale ends at the maximum of 6 duties.</p>
        </section>
      </div>

      <section className="panel">
        <h2>Hall usage</h2>
        <div className="scroll">
          <table className="table">
            <thead><tr><th>Hall</th><th>Seats</th><th>Exams hosted</th></tr></thead>
            <tbody>{d.hallUsage.map((h) => <tr key={h.hall}><td>{h.hall}</td><td>{h.capacity}</td><td>{h.examsHosted}</td></tr>)}</tbody>
          </table>
        </div>
      </section>

      <section className="panel">
        <h2>Upcoming exams</h2>
        {d.upcoming.length === 0 ? <p className="muted">No upcoming exams.</p> : (
          <div className="scroll">
            <table className="table">
              <thead><tr><th>Exam</th><th>Date</th><th>Time</th><th>Status</th></tr></thead>
              <tbody>
                {d.upcoming.map((e) => (
                  <tr key={e.id}>
                    <td><Link to={`/exams/${e.id}`}>{e.code}</Link> {e.name}</td>
                    <td>{fmtDate(e.examDate)}</td>
                    <td>{fmtTime(e.startTime)} to {fmtTime(e.endTime)}</td>
                    <td><span className={`pill pill-${e.status}`}>{e.status}</span></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </div>
  )
}
