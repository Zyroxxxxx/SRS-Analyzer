const STATUS_LABEL = {
  PENDING: 'Cho phan tich',
  ANALYZING: 'Dang phan tich',
  ANALYZED: 'Da phan tich',
  FAILED: 'Loi phan tich',
}

function formatReqId(id) {
  return `REQ-${String(id).padStart(4, '0')}`
}

function formatDate(isoString) {
  if (!isoString) return ''
  const d = new Date(isoString)
  return d.toLocaleString('vi-VN', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}

export default function RequirementList({ requirements, loading, onDelete }) {
  if (loading) {
    return <p className="req-list__empty">Dang tai danh sach...</p>
  }

  if (requirements.length === 0) {
    return (
      <p className="req-list__empty">
        Chua co yeu cau nao. Nhap yeu cau dau tien o form ben trai.
      </p>
    )
  }

  return (
    <ul className="req-list">
      {requirements.map((req) => (
        <li key={req.id} className="req-row">
          <div className="req-row__meta">
            <span className="req-row__id">{formatReqId(req.id)}</span>
            <span className={`req-row__status req-row__status--${req.status.toLowerCase()}`}>
              {STATUS_LABEL[req.status] ?? req.status}
            </span>
          </div>

          <h3 className="req-row__title">{req.title}</h3>
          <p className="req-row__desc">{req.rawDescription}</p>

          <div className="req-row__footer">
            <span className="req-row__date">Tao luc {formatDate(req.createdAt)}</span>
            <button
              type="button"
              className="btn-text"
              onClick={() => onDelete(req.id)}
            >
              Xoa
            </button>
          </div>
        </li>
      ))}
    </ul>
  )
}
