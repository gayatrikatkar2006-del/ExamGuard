import { Link } from 'react-router-dom'
import { useAuth, homePathFor } from '../context/AuthContext'

// A small static hall: exam CS301 (filled) and EC302 (hatched) share one room.
function DemoHall() {
  const rows = 4, cols = 8
  const cells = []
  for (let r = 0; r < rows; r++) {
    for (let c = 0; c < cols; c++) {
      const first = c % 2 === 0
      cells.push(<div key={`${r}${c}`} className={first ? 'seat seat-taken dept-CSE' : 'seat seat-other'}>{first ? 'CS301' : 'EC302'}</div>)
    }
  }
  return (
    <div className="seat-scroll">
      <div className="seat-grid demo" style={{ gridTemplateColumns: `repeat(${cols}, minmax(3.4rem, 1fr))` }}>{cells}</div>
    </div>
  )
}

export default function Home() {
  const { user } = useAuth()
  return (
    <div className="home">
      <section className="hero">
        <div>
          <h1>Every seat and every invigilator, decided by a rule you can read.</h1>
          <p className="lead">
            ExamGuard builds exam seating plans and invigilation duty rosters for a college from a short list of written
            rules. No spreadsheet shuffling, no clashes, and every result can be explained.
          </p>
          <div className="row">
            {user
              ? <Link className="btn" to={homePathFor(user.role)}>Open my workspace</Link>
              : <Link className="btn" to="/login">Log in</Link>}
            <Link className="btn btn-ghost" to="/kotlin">See the Kotlin Lab</Link>
          </div>
        </div>
        <figure className="hero-figure">
          <DemoHall />
          <figcaption>Two exams sharing one hall: no student ever sits next to a student of the same exam.</figcaption>
        </figure>
      </section>

      <section className="rules">
        <div>
          <h2>Seating rules</h2>
          <dl>
            <dt>Roll-number order</dt><dd>Students are placed in roll-number order, hall by hall.</dd>
            <dt>Alternate seats</dt><dd>Two students of the same exam are never seated side by side.</dd>
            <dt>Shared slots</dt><dd>A seat already used by another exam in the same time slot is skipped.</dd>
            <dt>Hall status</dt><dd>Halls under maintenance are never used.</dd>
            <dt>All or nothing</dt><dd>If someone cannot be seated, nothing is saved and you see how many are left.</dd>
          </dl>
        </div>
        <div>
          <h2>Invigilation rules</h2>
          <dl>
            <dt>One per 30</dt><dd>One invigilator for every 30 students in a hall, at least one per hall.</dd>
            <dt>No home duty</dt><dd>Faculty never invigilate an exam of their own department.</dd>
            <dt>No double duty</dt><dd>Nobody is placed in two exams that overlap in time.</dd>
            <dt>Fair workload</dt><dd>The faculty member with the fewest duties is chosen first, up to a maximum of 6.</dd>
            <dt>Chief invigilator</dt><dd>The first invigilator in each hall is marked as chief.</dd>
          </dl>
        </div>
      </section>
    </div>
  )
}
