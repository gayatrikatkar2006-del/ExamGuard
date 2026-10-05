import { useEffect, useState } from 'react'
import { api } from '../api'
import useForm from '../hooks/useForm'
import { useToast } from '../context/ToastContext'

const EMPTY = { name: '', building: '', rowCount: '5', colCount: '6', status: 'AVAILABLE' }

const validate = (v) => {
  const e = {}
  if (!v.name.trim()) e.name = 'Hall name is required'
  if (!v.building.trim()) e.building = 'Building is required'
  const r = Number(v.rowCount), c = Number(v.colCount)
  if (!Number.isInteger(r) || r < 1 || r > 26) e.rowCount = 'Rows must be between 1 and 26'
  if (!Number.isInteger(c) || c < 1 || c > 20) e.colCount = 'Columns must be between 1 and 20'
  return e
}

export default function Halls() {
  const [halls, setHalls] = useState([])
  const [editing, setEditing] = useState(null)
  const [open, setOpen] = useState(false)
  const [search, setSearch] = useState('')
  const toast = useToast()
  const form = useForm(EMPTY, validate)

  const load = () => api('/halls').then(setHalls).catch((e) => toast(e.message, 'err'))
  useEffect(() => { load() }, [])

  const startAdd = () => { setEditing(null); form.reset(EMPTY); setOpen(true) }
  const startEdit = (h) => {
    setEditing(h.id)
    form.reset({ name: h.name, building: h.building, rowCount: String(h.rowCount), colCount: String(h.colCount), status: h.status })
    setOpen(true)
  }

  const save = form.handleSubmit(async (v) => {
    const body = { ...v, rowCount: Number(v.rowCount), colCount: Number(v.colCount) }
    try {
      if (editing) await api(`/halls/${editing}`, { method: 'PUT', body })
      else await api('/halls', { method: 'POST', body })
      toast(editing ? 'Hall updated' : 'Hall added')
      setOpen(false)
      load()
    } catch (err) {
      toast(err.message, 'err')
    }
  })

  const remove = async (h) => {
    if (!window.confirm(`Delete ${h.name}?`)) return
    try { await api(`/halls/${h.id}`, { method: 'DELETE' }); toast('Hall deleted'); load() }
    catch (err) { toast(err.message, 'err') }
  }

  const shown = halls.filter((h) => (h.name + h.building).toLowerCase().includes(search.toLowerCase()))
  const { values, errors, handleChange } = form

  return (
    <div className="page">
      <div className="page-head">
        <h1>Halls</h1>
        <button className="btn" onClick={startAdd}>Add hall</button>
      </div>

      {open && (
        <form className="panel formgrid" onSubmit={save} noValidate>
          <h2>{editing ? 'Edit hall' : 'New hall'}</h2>
          <label className="field"><span>Name</span><input name="name" value={values.name} onChange={handleChange} />{errors.name && <small className="err">{errors.name}</small>}</label>
          <label className="field"><span>Building</span><input name="building" value={values.building} onChange={handleChange} />{errors.building && <small className="err">{errors.building}</small>}</label>
          <label className="field"><span>Rows</span><input name="rowCount" type="number" value={values.rowCount} onChange={handleChange} />{errors.rowCount && <small className="err">{errors.rowCount}</small>}</label>
          <label className="field"><span>Seats per row</span><input name="colCount" type="number" value={values.colCount} onChange={handleChange} />{errors.colCount && <small className="err">{errors.colCount}</small>}</label>
          <label className="field"><span>Status</span>
            <select name="status" value={values.status} onChange={handleChange}>
              <option>AVAILABLE</option><option>MAINTENANCE</option>
            </select>
          </label>
          <div className="row">
            <button className="btn">{editing ? 'Save changes' : 'Add hall'}</button>
            <button type="button" className="btn btn-ghost" onClick={() => setOpen(false)}>Cancel</button>
          </div>
        </form>
      )}

      <input className="search" placeholder="Search halls" value={search} onChange={(e) => setSearch(e.target.value)} />

      <div className="scroll">
        <table className="table">
          <thead><tr><th>Hall</th><th>Building</th><th>Layout</th><th>Seats</th><th>Alternate-seat capacity</th><th>Status</th><th /></tr></thead>
          <tbody>
            {shown.map((h) => (
              <tr key={h.id}>
                <td>{h.name}</td>
                <td>{h.building}</td>
                <td>{h.rowCount} rows x {h.colCount}</td>
                <td>{h.capacity}</td>
                <td>{h.rowCount * Math.ceil(h.colCount / 2)}</td>
                <td><span className={`pill pill-${h.status}`}>{h.status}</span></td>
                <td className="actions">
                  <button className="btn btn-small btn-ghost" onClick={() => startEdit(h)}>Edit</button>
                  <button className="btn btn-small btn-danger" onClick={() => remove(h)}>Delete</button>
                </td>
              </tr>
            ))}
            {shown.length === 0 && <tr><td colSpan="7" className="muted">No halls found.</td></tr>}
          </tbody>
        </table>
      </div>
    </div>
  )
}
