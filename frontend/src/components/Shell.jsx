import { Link, NavLink, useNavigate } from 'react-router-dom'
import { useAuth, homePathFor } from '../context/AuthContext'

const NAV = {
  ADMIN: [['/admin', 'Dashboard'], ['/admin/halls', 'Halls'], ['/admin/exams', 'Exams and allocation']],
  FACULTY: [['/faculty', 'My invigilation duties']],
  STUDENT: [['/student', 'My exam seats']],
}

export default function Shell({ children }) {
  const { user, logout } = useAuth()
  const navigate = useNavigate()

  const out = () => { logout(); navigate('/') }

  return (
    <div className="shell">
      <header className="topbar">
        <Link to="/" className="brand">ExamGuard</Link>
        <nav className="topnav">
          <NavLink to="/" end>Home</NavLink>
          <NavLink to="/kotlin">Kotlin Lab</NavLink>
          {user ? (
            <>
              <Link to={homePathFor(user.role)}>{user.name}</Link>
              <button className="btn btn-ghost" onClick={out}>Log out</button>
            </>
          ) : (
            <NavLink to="/login">Log in</NavLink>
          )}
        </nav>
      </header>

      <div className={user ? 'body with-side' : 'body'}>
        {user && (
          <aside className="side">
            <p className="side-role">{user.role.toLowerCase()}</p>
            {NAV[user.role].map(([to, label]) => (
              <NavLink key={to} to={to} end className="side-link">{label}</NavLink>
            ))}
          </aside>
        )}
        <main className="main">{children}</main>
      </div>
    </div>
  )
}
