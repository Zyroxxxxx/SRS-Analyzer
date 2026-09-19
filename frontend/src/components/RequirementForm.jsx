import { useState } from 'react'

// Form nhap yeu cau phan mem bang ngon ngu tu nhien
// Component "ngu" - khong tu goi API, chi bao ve cho App qua onSubmit
export default function RequirementForm({ onSubmit, submitting }) {
  const [title, setTitle] = useState('')
  const [rawDescription, setRawDescription] = useState('')
  const [error, setError] = useState('')

  function handleSubmit(e) {
    e.preventDefault()

    if (!title.trim() || !rawDescription.trim()) {
      setError('Vui long nhap day du tieu de va mo ta yeu cau')
      return
    }

    setError('')
    onSubmit({ title: title.trim(), rawDescription: rawDescription.trim() })
    setTitle('')
    setRawDescription('')
  }

  return (
    <form className="req-form" onSubmit={handleSubmit}>
      <div className="req-form__field">
        <label htmlFor="title">Tieu de</label>
        <input
          id="title"
          type="text"
          placeholder="Vi du: Dang nhap he thong"
          value={title}
          onChange={(e) => setTitle(e.target.value)}
        />
      </div>

      <div className="req-form__field">
        <label htmlFor="rawDescription">Mo ta yeu cau (ngon ngu tu nhien)</label>
        <textarea
          id="rawDescription"
          rows={6}
          placeholder="Vi du: Nguoi dung can dang nhap he thong bang email va mat khau. Neu sai qua 5 lan thi khoa tai khoan 15 phut."
          value={rawDescription}
          onChange={(e) => setRawDescription(e.target.value)}
        />
      </div>

      {error && <p className="req-form__error">{error}</p>}

      <button type="submit" className="btn-primary" disabled={submitting}>
        {submitting ? 'Dang luu...' : 'Them yeu cau'}
      </button>
    </form>
  )
}
