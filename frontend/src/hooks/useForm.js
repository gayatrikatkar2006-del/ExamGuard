import { useState } from 'react'

// Custom hook: values, validation errors, change handler and submit handler in one place.
export default function useForm(initial, validate) {
  const [values, setValues] = useState(initial)
  const [errors, setErrors] = useState({})

  const handleChange = (e) => {
    const { name, value } = e.target
    setValues((v) => ({ ...v, [name]: value }))
    if (errors[name]) setErrors((er) => ({ ...er, [name]: undefined }))
  }

  const handleSubmit = (onSubmit) => async (e) => {
    e.preventDefault()
    const found = validate(values)
    setErrors(found)
    if (Object.keys(found).length === 0) await onSubmit(values)
  }

  const reset = (next = initial) => { setValues(next); setErrors({}) }

  return { values, errors, setValues, setErrors, handleChange, handleSubmit, reset }
}
