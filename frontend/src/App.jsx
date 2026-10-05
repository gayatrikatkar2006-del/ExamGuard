import { Navigate, Route, Routes } from 'react-router-dom'
import Shell from './components/Shell.jsx'
import { useAuth, homePathFor } from './context/AuthContext.jsx'
import Home from './pages/Home.jsx'
import Login from './pages/Login.jsx'
import KotlinLab from './pages/KotlinLab.jsx'
import AdminDashboard from './pages/AdminDashboard.jsx'
import Halls from './pages/Halls.jsx'
import Exams from './pages/Exams.jsx'
import ExamDetail from './pages/ExamDetail.jsx'
import MySeats from './pages/MySeats.jsx'
import MyDuties from './pages/MyDuties.jsx'

function Protected({ roles, children }) {
  const { user } = useAuth()
  if (!user) return <Navigate to="/login" replace />
  if (!roles.includes(user.role)) return <Navigate to={homePathFor(user.role)} replace />
  return children
}

export default function App() {
  return (
    <Shell>
      <Routes>
        <Route path="/" element={<Home />} />
        <Route path="/login" element={<Login />} />
        <Route path="/kotlin" element={<KotlinLab />} />
        <Route path="/admin" element={<Protected roles={['ADMIN']}><AdminDashboard /></Protected>} />
        <Route path="/admin/halls" element={<Protected roles={['ADMIN']}><Halls /></Protected>} />
        <Route path="/admin/exams" element={<Protected roles={['ADMIN']}><Exams /></Protected>} />
        <Route path="/exams/:id" element={<Protected roles={['ADMIN', 'FACULTY']}><ExamDetail /></Protected>} />
        <Route path="/faculty" element={<Protected roles={['FACULTY']}><MyDuties /></Protected>} />
        <Route path="/student" element={<Protected roles={['STUDENT']}><MySeats /></Protected>} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </Shell>
  )
}
