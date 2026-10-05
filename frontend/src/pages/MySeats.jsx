import { useEffect, useState } from 'react'
import { api, fmtDate, fmtTime } from '../api'
import SeatGrid from '../components/SeatGrid'
import { useAuth } from '../context/AuthContext'

export default function MySeats() {
  const { user } = useAuth()
  const [rows, setRows] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => { api('/me/seats').then(setRows).catch((e) => setError(e.message)) }, [])

  return (
    <div className="page">
      <h1>My exam seats</h1>
      <p className="muted">{user.name} | {user.rollNo} | {user.department}</p>
      {error && <p className="err">{error}</p>}
      {rows && rows.length === 0 && <p className="panel">No seats have been allotted to you yet. Check again after the exam cell publishes the plan.</p>}
      {rows && rows.map((s) => (
        <section key={s.examId} className="panel seat-card">
          <div>
            <h2>{s.examCode}: {s.examName}</h2>
            <p>{fmtDate(s.examDate)}, {fmtTime(s.startTime)} to {fmtTime(s.endTime)}</p>
            <p className="big-seat">{s.hallName} <strong>{s.seatLabel}</strong></p>
            <p className="muted">{s.building}. Row {s.seatLabel.split('-')[0]}, seat {s.seatCol}.</p>
          </div>
          <SeatGrid rows={s.rowCount} cols={s.colCount} highlight={{ row: s.seatRow, col: s.seatCol }} />
        </section>
      ))}
    </div>
  )
}
