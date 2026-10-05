import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { api, fmtDate, fmtTime } from '../api'
import SeatGrid from '../components/SeatGrid'
import { useAuth } from '../context/AuthContext'

export default function ExamDetail() {
  const { id } = useParams()
  const { user } = useAuth()
  const [data, setData] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => { api(`/exams/${id}/seating`).then(setData).catch((e) => setError(e.message)) }, [id])

  if (error) return <p className="err">{error}</p>
  if (!data) return <p className="muted">Loading...</p>
  const { exam, halls } = data

  return (
    <div className="page">
      <p className="small"><Link to={user.role === 'ADMIN' ? '/admin/exams' : '/faculty'}>Back</Link></p>
      <div className="page-head">
        <div>
          <h1>{exam.code}: {exam.name}</h1>
          <p className="muted">
            {exam.department} | {fmtDate(exam.examDate)} | {fmtTime(exam.startTime)} to {fmtTime(exam.endTime)} |{' '}
            <span className={`pill pill-${exam.status}`}>{exam.status}</span>
          </p>
        </div>
        {halls.length > 0 && <button className="btn btn-ghost noprint" onClick={() => window.print()}>Print seating plan</button>}
      </div>

      {halls.length === 0 && <p className="panel">Seats have not been allocated yet.{user.role === 'ADMIN' && <> Go to <Link to="/admin/exams">Exams and allocation</Link>.</>}</p>}

      {halls.map((h) => (
        <section key={h.hallId} className="panel hall-block">
          <div className="page-head">
            <h2>{h.hallName} <span className="muted small">{h.building} | {h.seats.length} students</span></h2>
          </div>
          <p className="small invig">
            <strong>Invigilators:</strong>{' '}
            {h.invigilators.length === 0 ? 'not assigned yet' : h.invigilators.map((i) => (
              <span key={i.facultyId} className={i.duty === 'CHIEF' ? 'tag tag-chief' : 'tag'}>{i.name} ({i.duty.toLowerCase()})</span>
            ))}
          </p>
          <SeatGrid rows={h.rowCount} cols={h.colCount} seats={h.seats} others={h.others} />
          <details className="noprint">
            <summary>Student list</summary>
            <div className="scroll">
              <table className="table">
                <thead><tr><th>Seat</th><th>Roll no</th><th>Name</th><th>Dept</th></tr></thead>
                <tbody>{h.seats.map((s) => <tr key={s.studentId}><td>{s.seatLabel}</td><td>{s.rollNo}</td><td>{s.name}</td><td>{s.department}</td></tr>)}</tbody>
              </table>
            </div>
          </details>
        </section>
      ))}
      {halls.length > 0 && (
        <p className="legend small muted">
          <span className="seat seat-taken dept-CSE">filled</span> this exam
          <span className="seat seat-other">hatched</span> another exam in the same slot
          <span className="seat seat-empty">blank</span> left empty by the alternate-seat rule
        </p>
      )}
    </div>
  )
}
