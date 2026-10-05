import { useEffect, useState } from 'react'
import { api } from '../api'

export default function KotlinLab() {
  const [sections, setSections] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => {
    api('/kotlin-demo', { auth: false }).then(setSections).catch((e) => setError(e.message))
  }, [])

  return (
    <div className="page">
      <h1>Kotlin Lab</h1>
      <p className="muted">Every result below is computed live by Kotlin code running inside the ExamGuard backend.</p>
      {error && <p className="err">{error}</p>}
      {!sections && !error && <p className="muted">Loading...</p>}
      {sections && sections.map((s) => (
        <section key={s.title} className="panel">
          <h2>{s.title}</h2>
          <p className="muted">{s.concept}</p>
          <div className="scroll">
            <table className="table">
              <thead><tr><th>Kotlin code</th><th>Result</th></tr></thead>
              <tbody>
                {s.lines.map((l, i) => (
                  <tr key={i}><td><code>{l.code}</code></td><td>{l.result}</td></tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>
      ))}
    </div>
  )
}
