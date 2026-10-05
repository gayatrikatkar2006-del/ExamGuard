import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api, fmtDate, fmtTime } from '../api'
import { useAuth } from '../context/AuthContext'

export default function MyDuties() {
  const { user } = useAuth()
  const [rows, setRows] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => { api('/me/duties').then(setRows).catch((e) => setError(e.message)) }, [])

  return (
    <div className="page">
      <h1>My invigilation duties</h1>
      <p className="muted">{user.name} | {user.department}</p>
      {error && <p className="err">{error}</p>}
      {rows && rows.length === 0 && <p className="panel">You have no duties assigned yet.</p>}
      {rows && rows.length > 0 && (
        <div className="scroll">
          <table className="table">
            <thead><tr><th>Exam</th><th>Date</th><th>Time</th><th>Hall</th><th>Students</th><th>Role</th></tr></thead>
            <tbody>
              {rows.map((d) => (
                <tr key={d.examId + d.hallName}>
                  <td><Link to={`/exams/${d.examId}`}>{d.examCode}</Link> {d.examName}</td>
                  <td>{fmtDate(d.examDate)}</td>
                  <td>{fmtTime(d.startTime)} to {fmtTime(d.endTime)}</td>
                  <td>{d.hallName}, {d.building}</td>
                  <td>{d.studentsInHall}</td>
                  <td><span className={d.duty === 'CHIEF' ? 'tag tag-chief' : 'tag'}>{d.duty.toLowerCase()}</span></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}
