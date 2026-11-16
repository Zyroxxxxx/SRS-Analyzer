import { useEffect, useMemo, useRef, useState } from 'react'
import './chat.css'
import { fetchRequirements, createRequirement, deleteRequirement, getAnalysis, exportSrs, exportSrsExcel } from './api/requirementApi'
import { updatePreference } from './api/authApi'
import * as W from './api/workspaceApi'

const errText = (e, fallback) => e?.response?.data?.message || e?.message || fallback
const parseList = s => {
  try { const v = JSON.parse(s || '[]'); return Array.isArray(v) ? v.map(x => (typeof x === 'string' ? x : JSON.stringify(x))) : [] } catch { return [] }
}
const wordCount = t => (t.trim().match(/\S+/g) || []).length
const STATUS = { PENDING: 'Chờ phân tích', ANALYZED: 'Đã phân tích', FAILED: 'Lỗi phân tích' }
const SOURCE = { ORIGINAL: 'Bản gốc', UPLOAD: 'Từ file Word', A: 'Bản A', B: 'Bản B', MANUAL: 'Sửa tay', RESTORE: 'Khôi phục' }
const MAX_FILE = 30 * 1024 * 1024
const SAMPLES = [
  'Hệ thống đặt lịch khám bệnh trực tuyến cho phòng khám',
  'Ứng dụng quản lý chi tiêu cá nhân có báo cáo theo tháng',
  'Website bán hàng có giỏ hàng, thanh toán và theo dõi đơn hàng',
]
const FEATURES = [
  ['📝', 'Nhập yêu cầu', 'Mô tả bằng lời hoặc tải file Word'],
  ['🤖', 'AI phân tích', 'User Story, tiêu chí chấp nhận, bản A/B'],
  ['📤', 'Xuất kết quả', 'SRS, Excel, SwimLane PNG, gửi Email'],
]

function groupLabel(date) {
  const days = (Date.now() - new Date(date).getTime()) / 86400000
  return days < 1 ? 'Hôm nay' : days < 7 ? '7 ngày qua' : days < 30 ? '30 ngày qua' : 'Cũ hơn'
}

function Modal({ title, onClose, children }) {
  useEffect(() => {
    const h = e => e.key === 'Escape' && onClose()
    window.addEventListener('keydown', h)
    return () => window.removeEventListener('keydown', h)
  }, [onClose])
  return (
    <div className="cw-modal-bg" onMouseDown={e => e.target === e.currentTarget && onClose()}>
      <div className="cw-modal" role="dialog" aria-label={title}>
        <h3>{title}</h3>
        {children}
      </div>
    </div>
  )
}

/* ===================== Composer (ô nhập phía dưới, giống ChatGPT) ===================== */
function Composer({ busy, onText, onFile, preset }) {
  const [text, setText] = useState('')
  const fileRef = useRef(null)
  const taRef = useRef(null)

  useEffect(() => {
    if (!preset) return
    setText(preset.text)
    requestAnimationFrame(() => { resize(); taRef.current?.focus() })
  }, [preset]) // eslint-disable-line react-hooks/exhaustive-deps

  function resize() {
    const el = taRef.current
    if (!el) return
    el.style.height = 'auto'
    el.style.height = Math.min(el.scrollHeight, 220) + 'px'
  }
  async function submit() {
    if (!text.trim() || busy) return
    const t = text
    setText('')
    requestAnimationFrame(resize)
    if (!(await onText(t))) setText(t) // loi -> giu lai noi dung de nguoi dung khong mat
  }
  return (
    <div className="cw-composer-wrap">
      <div className="cw-composer">
        <button className="cw-icon" title="Tải lên file Word (.docx, tối đa 100.000 từ)" disabled={busy} onClick={() => fileRef.current?.click()}>📎</button>
        <input ref={fileRef} type="file" hidden accept=".docx,application/vnd.openxmlformats-officedocument.wordprocessingml.document"
          onChange={e => { const f = e.target.files?.[0]; e.target.value = ''; if (f) onFile(f) }} />
        <textarea ref={taRef} rows={1} value={text} placeholder="Mô tả yêu cầu phần mềm mới…"
          onChange={e => { setText(e.target.value); resize() }}
          onKeyDown={e => { if (e.key === 'Enter' && !e.shiftKey && !e.nativeEvent.isComposing) { e.preventDefault(); submit() } }} />
        <button className="cw-send" disabled={busy || !text.trim()} onClick={submit} title="Gửi (Enter)">↑</button>
      </div>
      <p className="cw-hint">Enter để gửi · Shift+Enter xuống dòng · 📎 Word .docx tối đa 100.000 từ</p>
    </div>
  )
}

/* ===================== Acceptance Criteria ===================== */
function CriteriaRow({ c, onSave, onDelete }) {
  const [edit, setEdit] = useState(false)
  const [v, setV] = useState(c.content)
  if (edit) {
    return (
      <div className="cw-ac edit">
        <textarea rows={2} value={v} onChange={e => setV(e.target.value)} />
        <span className="cw-mini">
          <button onClick={async () => { if (await onSave(c.id, v)) setEdit(false) }}>Lưu</button>
          <button onClick={() => { setV(c.content); setEdit(false) }}>Hủy</button>
        </span>
      </div>
    )
  }
  return (
    <div className="cw-ac">
      <span>{c.content}</span>
      <span className="cw-mini"><button onClick={() => setEdit(true)}>Sửa</button><button onClick={() => onDelete(c.id)}>Xóa</button></span>
    </div>
  )
}

/* ===================== User Story ===================== */
function StoryCard({ s, onChange, onRemove, flash }) {
  const [edit, setEdit] = useState(false)
  const [text, setText] = useState(s.content)
  const [prio, setPrio] = useState(s.priority || 'MEDIUM')
  const [ai, setAi] = useState(false)
  const [instr, setInstr] = useState('')
  const [suggest, setSuggest] = useState('')
  const [acText, setAcText] = useState('')
  const [busy, setBusy] = useState(false)

  async function run(fn, fail) {
    setBusy(true)
    try { return await fn() } catch (e) { flash(errText(e, fail)); return undefined } finally { setBusy(false) }
  }
  const save = async () => {
    const r = await run(() => W.updateStory(s.id, text, prio), 'Không thể lưu User Story.')
    if (r) { onChange(r); setEdit(false) }
  }
  const rewrite = async () => {
    const r = await run(() => W.rewriteStory(s.id, instr), 'AI không viết lại được User Story.')
    if (r) setSuggest(r.content)
  }
  const apply = async () => {
    const r = await run(() => W.updateStory(s.id, suggest, s.priority), 'Không thể lưu User Story.')
    if (r) { onChange(r); setText(r.content); setSuggest(''); setAi(false); setInstr('') }
  }
  const toggleConfirm = async () => {
    const r = await run(() => W.confirmStory(s.id, !s.confirmed), 'Không thể xác nhận User Story.')
    if (r) onChange(r)
  }
  const acAction = fn => async (...args) => {
    const r = await run(() => fn(...args), 'Không thể cập nhật Acceptance Criteria.')
    if (r) onChange(r)
    return !!r
  }

  return (
    <article className={`cw-story p-${s.priority || 'MEDIUM'} ${s.confirmed ? 'confirmed' : ''}`}>
      <div className="cw-story-head">
        <span className={`cw-badge p-${s.priority || 'MEDIUM'}`}>{s.priority || 'MEDIUM'}</span>
        {s.confirmed && <span className="cw-badge ok">✓ Đã xác nhận</span>}
      </div>

      {edit ? (
        <>
          <textarea rows={3} value={text} onChange={e => setText(e.target.value)} />
          <div className="cw-row">
            <select value={prio} onChange={e => setPrio(e.target.value)}>
              <option value="HIGH">HIGH</option><option value="MEDIUM">MEDIUM</option><option value="LOW">LOW</option>
            </select>
            <button className="cw-btn primary" disabled={busy} onClick={save}>Lưu</button>
            <button className="cw-btn" onClick={() => { setText(s.content); setPrio(s.priority || 'MEDIUM'); setEdit(false) }}>Hủy</button>
          </div>
        </>
      ) : (
        <p className="cw-story-text">{s.content}</p>
      )}

      {ai && (
        <div className="cw-ai-box">
          <input value={instr} onChange={e => setInstr(e.target.value)} placeholder="Yêu cầu cho AI (tuỳ chọn): ngắn gọn hơn, thêm vai trò admin…" />
          <button className="cw-btn primary" disabled={busy} onClick={rewrite}>{busy ? 'Đang viết…' : 'Viết lại'}</button>
          {suggest && (
            <div className="cw-suggest">
              <p>{suggest}</p>
              <button className="cw-btn primary" disabled={busy} onClick={apply}>Áp dụng</button>
              <button className="cw-btn" onClick={() => setSuggest('')}>Bỏ</button>
            </div>
          )}
        </div>
      )}

      <div className="cw-ac-title">Acceptance Criteria ({s.criteria.length})</div>
      {s.criteria.length === 0 && <p className="cw-muted">Chưa có tiêu chí. Thêm ít nhất 1 tiêu chí (Given-When-Then) để xác nhận.</p>}
      {s.criteria.map(c => (
        <CriteriaRow key={c.id} c={c} onSave={acAction(W.updateCriteria)} onDelete={acAction(W.deleteCriteria)} />
      ))}
      <div className="cw-row">
        <input value={acText} onChange={e => setAcText(e.target.value)} placeholder="Given … When … Then …"
          onKeyDown={async e => { if (e.key === 'Enter' && acText.trim()) { if (await acAction(W.addCriteria)(s.id, acText)) setAcText('') } }} />
        <button className="cw-btn" disabled={busy || !acText.trim()} onClick={async () => { if (await acAction(W.addCriteria)(s.id, acText)) setAcText('') }}>+ Tiêu chí</button>
      </div>

      <div className="cw-row cw-story-actions">
        <button className="cw-btn" onClick={() => setEdit(e => !e)}>✎ Sửa</button>
        <button className="cw-btn" onClick={() => setAi(a => !a)}>✨ Viết lại bằng AI</button>
        <button className="cw-btn" disabled={busy} onClick={() => onRemove(s.id)}>🗑 Xóa</button>
        <button className={`cw-btn ${s.confirmed ? '' : 'primary'}`} disabled={busy} onClick={toggleConfirm}>
          {s.confirmed ? 'Bỏ xác nhận' : '✓ Xác nhận'}
        </button>
      </div>
    </article>
  )
}

/* ===================== Email ===================== */
function EmailModal({ req, hasStories, onClose, flash }) {
  const [to, setTo] = useState('')
  const [subject, setSubject] = useState(`SRS: ${req.title}`)
  const [message, setMessage] = useState('')
  const [srs, setSrs] = useState(true)
  const [swim, setSwim] = useState(hasStories)
  const [busy, setBusy] = useState(false)

  async function send() {
    const list = to.split(/[,;\s]+/).map(x => x.trim()).filter(Boolean)
    if (!list.length) return flash('Vui lòng nhập ít nhất 1 địa chỉ email.')
    setBusy(true)
    try {
      await W.sendEmail(req.id, { to: list, subject, message, attachSrs: srs, attachSwimlane: swim })
      flash('Đã gửi email thành công.', 'ok')
      onClose()
    } catch (e) {
      flash(errText(e, 'Không gửi được email.'))
    } finally {
      setBusy(false)
    }
  }
  return (
    <Modal title="Gửi kết quả qua Email" onClose={onClose}>
      <label>Người nhận (tối đa 5, cách nhau bằng dấu phẩy)<input value={to} onChange={e => setTo(e.target.value)} placeholder="ten@congty.com, ..." autoFocus /></label>
      <label>Tiêu đề<input value={subject} onChange={e => setSubject(e.target.value)} /></label>
      <label>Lời nhắn<textarea rows={3} value={message} onChange={e => setMessage(e.target.value)} /></label>
      <label className="cw-check"><input type="checkbox" checked={srs} onChange={e => setSrs(e.target.checked)} /> Đính kèm SRS (HTML)</label>
      <label className="cw-check"><input type="checkbox" checked={swim} disabled={!hasStories} onChange={e => setSwim(e.target.checked)} /> Đính kèm SwimLane (PNG){!hasStories && ' — cần có User Story'}</label>
      <div className="cw-row end">
        <button className="cw-btn" onClick={onClose}>Hủy</button>
        <button className="cw-btn primary" disabled={busy} onClick={send}>{busy ? 'Đang gửi…' : 'Gửi'}</button>
      </div>
    </Modal>
  )
}

/* ===================== Lịch sử phiên bản ===================== */
function VersionsPanel({ req, onPatch, flash, reloadKey }) {
  const [list, setList] = useState(null)
  const [editing, setEditing] = useState(false)
  const [text, setText] = useState(req.rawDescription)
  const load = () => W.fetchVersions(req.id).then(setList).catch(e => flash(errText(e, 'Không tải được lịch sử phiên bản.')))
  useEffect(() => { load() }, [req.id, reloadKey, req.rawDescription]) // eslint-disable-line react-hooks/exhaustive-deps

  async function restore(v) {
    try { onPatch(await W.restoreVersion(req.id, v.id)); flash(`Đã khôi phục nội dung phiên bản v${v.versionNo}.`, 'ok') } catch (e) { flash(errText(e, 'Không khôi phục được.')) }
  }
  async function saveManual() {
    if (text.trim() === req.rawDescription.trim()) return setEditing(false)
    try { onPatch(await W.applyVersion(req.id, 'MANUAL', text)); setEditing(false) } catch (e) { flash(errText(e, 'Không lưu được phiên bản.')) }
  }
  return (
    <section className="cw-card">
      <div className="cw-card-head">
        <h4>Lịch sử phiên bản</h4>
        <button className="cw-btn" onClick={() => { setText(req.rawDescription); setEditing(e => !e) }}>✎ Sửa tay → phiên bản mới</button>
      </div>
      {editing && (
        <div className="cw-edit">
          <textarea rows={8} value={text} onChange={e => setText(e.target.value)} />
          <div className="cw-row end"><button className="cw-btn" onClick={() => setEditing(false)}>Hủy</button><button className="cw-btn primary" onClick={saveManual}>Lưu phiên bản</button></div>
        </div>
      )}
      {!list ? <p className="cw-muted">Đang tải…</p> : list.map(v => (
        <div key={v.id} className="cw-ver">
          <div>
            <strong>v{v.versionNo}</strong> <span className="cw-badge">{SOURCE[v.source] || v.source}</span>
            <span className="cw-muted"> · {new Date(v.createdAt).toLocaleString('vi-VN')} · {wordCount(v.content).toLocaleString('vi-VN')} từ</span>
            <p className="cw-ver-prev">{v.content.slice(0, 160)}{v.content.length > 160 ? '…' : ''}</p>
          </div>
          {v.content === req.rawDescription ? <span className="cw-badge ok">Hiện tại</span> : <button className="cw-btn" onClick={() => restore(v)}>Khôi phục</button>}
        </div>
      ))}
    </section>
  )
}

/* ===================== Luồng hội thoại của 1 requirement ===================== */
function Thread({ req, onPatch, flash }) {
  const [analysis, setAnalysis] = useState(null)
  const [stories, setStories] = useState([])
  const [ab, setAb] = useState(null)
  const [busy, setBusy] = useState('')
  const [showVer, setShowVer] = useState(false)
  const [verKey, setVerKey] = useState(0)
  const [full, setFull] = useState(false)
  const [reAsk, setReAsk] = useState(false)
  const [swimUrl, setSwimUrl] = useState(null)
  const [mail, setMail] = useState(false)
  const [newStory, setNewStory] = useState('')
  const swimRef = useRef(null)

  useEffect(() => {
    let alive = true
    W.fetchStories(req.id).then(s => alive && setStories(s)).catch(() => {})
    if (req.status === 'ANALYZED') getAnalysis(req.id).then(a => alive && setAnalysis(a)).catch(() => {})
    return () => { alive = false; if (swimRef.current) URL.revokeObjectURL(swimRef.current) }
  }, [req.id]) // eslint-disable-line react-hooks/exhaustive-deps

  const text = req.rawDescription
  const words = useMemo(() => wordCount(text), [text])
  const confirmed = stories.filter(s => s.confirmed).length
  const patchStory = s => setStories(list => list.map(x => (x.id === s.id ? s : x)))
  const guard = (key, fail, fn) => async () => {
    setBusy(key)
    try { await fn() } catch (e) { flash(errText(e, fail)) } finally { setBusy('') }
  }

  const analyze = async () => {
    if (stories.length && !reAsk) return setReAsk(true)
    setReAsk(false)
    await guard('analyze', 'Phân tích thất bại.', async () => {
      try {
        setAnalysis(await W.analyze(req.id))
      } catch (e) { onPatch({ ...req, status: 'FAILED' }); throw e }
      setStories(await W.fetchStories(req.id))
      onPatch({ ...req, status: 'ANALYZED' })
      if (swimRef.current) { URL.revokeObjectURL(swimRef.current); swimRef.current = null; setSwimUrl(null) }
    })()
  }
  const genAB = guard('ab', 'AI không tạo được bản A/B.', async () => setAb(await W.generateAB(req.id)))
  const pickVariant = async key => {
    try { onPatch(await W.applyVersion(req.id, key, ab[key])); setAb(null); setVerKey(k => k + 1); flash(`Đã áp dụng bản ${key} thành phiên bản mới.`, 'ok') } catch (e) { flash(errText(e, 'Không áp dụng được.')) }
  }
  const makeSwim = guard('swim', 'Không tạo được SwimLane.', async () => {
    const url = await W.swimlaneUrl(req.id)
    if (swimRef.current) URL.revokeObjectURL(swimRef.current)
    swimRef.current = url
    setSwimUrl(url)
  })
  const makeSrs = guard('srs', 'Không tạo được SRS.', () => exportSrs(req.id))
  const makeXlsx = guard('xlsx', 'Không xuất được Excel.', () => exportSrsExcel(req.id))
  const removeStory = async id => {
    try { await W.deleteStory(id); setStories(l => l.filter(x => x.id !== id)) } catch (e) { flash(errText(e, 'Không xóa được User Story.')) }
  }
  const addStory = async () => {
    try { const s = await W.addStory(req.id, newStory, 'MEDIUM'); setStories(l => [...l, s]); setNewStory('') } catch (e) { flash(errText(e, 'Không thêm được User Story.')) }
  }

  return (
    <div className="cw-thread">
      {/* Yêu cầu của người dùng */}
      <div className="cw-msg user">
        <div className="cw-bubble">
          <p className="cw-pre">{full || text.length <= 1500 ? text : text.slice(0, 1500) + '…'}</p>
          {text.length > 1500 && <button className="cw-link" onClick={() => setFull(f => !f)}>{full ? 'Thu gọn' : 'Xem toàn bộ'}</button>}
          <small>{words.toLocaleString('vi-VN')} từ</small>
        </div>
      </div>

      {/* Phản hồi của trợ lý */}
      <div className="cw-msg bot">
        <div className="cw-avatar">AI</div>
        <div className="cw-bot-body">
          <div className="cw-status">
            <span className={`cw-dot ${req.status}`} /> {STATUS[req.status] || req.status}
            {req.status !== 'ANALYZED' && analysis && <span className="cw-muted"> — kết quả bên dưới là của lần phân tích trước</span>}
          </div>

          <div className="cw-actions">
            <button className="cw-btn primary" disabled={!!busy} onClick={analyze}>{busy === 'analyze' ? 'Đang phân tích…' : stories.length ? '↻ Phân tích lại' : '▶ Phân tích'}</button>
            <button className="cw-btn" disabled={!!busy} onClick={genAB}>{busy === 'ab' ? 'Đang tạo A/B…' : '⇄ Generation A/B'}</button>
            <button className="cw-btn" onClick={() => setShowVer(v => !v)}>🕘 Phiên bản</button>
            <button className="cw-btn" disabled={!!busy} onClick={makeSrs}>📄 Tạo SRS</button>
            <button className="cw-btn" disabled={!!busy || !stories.length} title={stories.length ? '' : 'Cần có User Story'} onClick={makeSwim}>{busy === 'swim' ? 'Đang vẽ…' : '🏊 SwimLane PNG'}</button>
            <button className="cw-btn" disabled={!!busy} onClick={makeXlsx}>📊 Excel</button>
            <button className="cw-btn" onClick={() => setMail(true)}>✉ Gửi Email</button>
          </div>

          {reAsk && (
            <div className="cw-warn">
              Phân tích lại sẽ <strong>thay thế toàn bộ User Story và Acceptance Criteria hiện có</strong> (kể cả phần bạn đã sửa/xác nhận).
              <div className="cw-row"><button className="cw-btn primary" onClick={analyze}>Vẫn phân tích lại</button><button className="cw-btn" onClick={() => setReAsk(false)}>Hủy</button></div>
            </div>
          )}

          {busy === 'analyze' && <div className="cw-typing"><i /><i /><i /></div>}

          {showVer && <VersionsPanel req={req} onPatch={onPatch} flash={flash} reloadKey={verKey} />}

          {ab && (
            <div className="cw-ab">
              {['A', 'B'].map(k => (
                <section key={k} className="cw-card">
                  <div className="cw-card-head"><h4>{k === 'A' ? 'Bản A · Chính thức' : 'Bản B · Ngắn gọn (agile)'}</h4></div>
                  <p className="cw-pre cw-ab-text">{ab[k]}</p>
                  <button className="cw-btn primary" onClick={() => pickVariant(k)}>Dùng bản {k}</button>
                </section>
              ))}
              <button className="cw-link" onClick={() => setAb(null)}>Đóng so sánh A/B</button>
            </div>
          )}

          {analysis && (
            <section className="cw-card">
              <div className="cw-card-head"><h4>Kết quả phân tích</h4><small className="cw-muted">{analysis.provider} · {analysis.totalTokens} token</small></div>
              <p>{analysis.summary}</p>
              {[['Functional Requirements', analysis.functionalRequirements], ['Non-functional Requirements', analysis.nonFunctionalRequirements], ['Điểm chưa rõ', analysis.ambiguousNotes]].map(([t, json]) => {
                const items = parseList(json)
                return items.length ? (
                  <details key={t} open={t.startsWith('Functional')}>
                    <summary>{t} ({items.length})</summary>
                    <ul>{items.map((x, i) => <li key={i}>{x}</li>)}</ul>
                  </details>
                ) : null
              })}
            </section>
          )}

          <section className="cw-card">
            <div className="cw-card-head">
              <h4>User Stories ({stories.length})</h4>
              {stories.length > 0 && <small className="cw-muted">Đã xác nhận {confirmed}/{stories.length}</small>}
            </div>
            {stories.length === 0 && <p className="cw-muted">Chưa có User Story. Bấm “Phân tích” để AI tạo, hoặc tự thêm bên dưới.</p>}
            {stories.map(s => <StoryCard key={s.id} s={s} onChange={patchStory} onRemove={removeStory} flash={flash} />)}
            <div className="cw-row">
              <input value={newStory} onChange={e => setNewStory(e.target.value)} placeholder="As a …, I want to …, so that …"
                onKeyDown={e => e.key === 'Enter' && newStory.trim() && addStory()} />
              <button className="cw-btn" disabled={!newStory.trim()} onClick={addStory}>+ Thêm User Story</button>
            </div>
          </section>

          {swimUrl && (
            <section className="cw-card">
              <div className="cw-card-head">
                <h4>SwimLane</h4>
                <a className="cw-btn" href={swimUrl} download={`REQ-${req.id}-swimlane.png`}>⬇ Tải PNG</a>
              </div>
              <div className="cw-swim"><img src={swimUrl} alt="SwimLane diagram" /></div>
            </section>
          )}
        </div>
      </div>

      {mail && <EmailModal req={req} hasStories={stories.length > 0} onClose={() => setMail(false)} flash={flash} />}
    </div>
  )
}

/* ===================== Khung chính ===================== */
export default function ChatWorkspace({ user, onLogout, onAdmin, onUserUpdate }) {
  const [items, setItems] = useState([])
  const [loading, setLoading] = useState(true)
  const [selId, setSelId] = useState(null)
  const [q, setQ] = useState('')
  const [navOpen, setNavOpen] = useState(false)
  const [toast, setToast] = useState(null)
  const [busy, setBusy] = useState(false)
  const [del, setDel] = useState(null)
  const [settings, setSettings] = useState(false)
  const [pref, setPref] = useState(user.aiPreference || '')
  const [lang, setLang] = useState(user.analysisLanguage || 'EN')
  const [preset, setPreset] = useState(null)
  const scrollRef = useRef(null)

  const flash = (text, type = 'error') => setToast({ text, type })
  useEffect(() => { if (toast) { const t = setTimeout(() => setToast(null), 5000); return () => clearTimeout(t) } }, [toast])
  useEffect(() => {
    fetchRequirements()
      .then(r => setItems([...r].sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))))
      .catch(e => flash(errText(e, 'Không thể tải danh sách yêu cầu.')))
      .finally(() => setLoading(false))
  }, [])
  useEffect(() => { scrollRef.current?.scrollTo({ top: 0 }) }, [selId])

  const sel = items.find(x => x.id === selId) || null
  const patch = x => setItems(list => list.map(i => (i.id === x.id ? { ...i, ...x } : i)))
  const add = x => { setItems(l => [x, ...l]); setSelId(x.id); setNavOpen(false) }

  async function onText(raw) {
    const t = raw.trim()
    const first = t.split('\n')[0].trim()
    setBusy(true)
    try { add(await createRequirement({ title: first.length > 80 ? first.slice(0, 77) + '…' : first, rawDescription: t })); return true }
    catch (e) { flash(errText(e, 'Không thể lưu yêu cầu.')); return false }
    finally { setBusy(false) }
  }
  async function onFile(f) {
    if (!/\.docx$/i.test(f.name)) return flash('Chỉ hỗ trợ file Word .docx (hãy lưu file .doc cũ thành .docx).')
    if (f.size > MAX_FILE) return flash('File quá lớn (tối đa 30 MB).')
    setBusy(true)
    flash(`Đang đọc “${f.name}”…`, 'ok')
    try { add(await W.importDocx(f)); flash('Đã nhập nội dung từ file Word.', 'ok') }
    catch (e) { flash(errText(e, 'Không đọc được file Word.')) }
    finally { setBusy(false) }
  }
  async function confirmDelete() {
    const x = del
    setDel(null)
    try { await deleteRequirement(x.id); setItems(l => l.filter(i => i.id !== x.id)); if (selId === x.id) setSelId(null) }
    catch (e) { flash(errText(e, 'Không thể xóa yêu cầu.')) }
  }
  async function savePref() {
    try { onUserUpdate(await updatePreference(pref, lang)); setSettings(false); flash('Đã lưu tùy chọn AI.', 'ok') }
    catch (e) { flash(errText(e, 'Không thể lưu tùy chọn phân tích.')) }
  }

  const groups = useMemo(() => {
    const k = q.trim().toLowerCase()
    const out = []
    items.filter(x => !k || x.title.toLowerCase().includes(k)).forEach(x => {
      const label = groupLabel(x.createdAt)
      const g = out.find(o => o.label === label)
      g ? g.items.push(x) : out.push({ label, items: [x] })
    })
    return out
  }, [items, q])

  return (
    <div className="cw">
      <aside className={`cw-side ${navOpen ? 'open' : ''}`}>
        <div className="cw-side-top">
          <div className="cw-brand"><div className="cw-logo">SR</div><div><strong>SRS Analyzer</strong><small>Requirement workspace</small></div></div>
          <button className="cw-new" onClick={() => { setSelId(null); setNavOpen(false) }}>＋ Yêu cầu mới</button>
          <input className="cw-search" value={q} onChange={e => setQ(e.target.value)} placeholder="Tìm yêu cầu…" />
        </div>
        <nav className="cw-list" aria-label="Danh sách Requirements">
          {loading && <p className="cw-muted pad">Đang tải…</p>}
          {!loading && !groups.length && <p className="cw-muted pad">{q ? 'Không có kết quả.' : 'Chưa có yêu cầu nào.'}</p>}
          {groups.map(g => (
            <div key={g.label}>
              <div className="cw-group">{g.label}</div>
              {g.items.map(x => (
                <div key={x.id} className={`cw-item ${x.id === selId ? 'active' : ''}`} onClick={() => { setSelId(x.id); setNavOpen(false) }} title={x.title}>
                  <span className={`cw-dot ${x.status}`} />
                  <span className="cw-item-title">{x.title}</span>
                  <button className="cw-x" title="Xóa" onClick={e => { e.stopPropagation(); setDel(x) }}>✕</button>
                </div>
              ))}
            </div>
          ))}
        </nav>
        <div className="cw-side-foot">
          <div className="cw-user"><div className="cw-avatar sm">{(user.fullName || user.username)[0]}</div><div><strong>{user.fullName || user.username}</strong><small>{user.role === 'ADMIN' ? 'Quản trị viên' : 'Chuyên viên'}</small></div></div>
          <div className="cw-row">
            <button className="cw-btn" onClick={() => { setPref(user.aiPreference || ''); setLang(user.analysisLanguage || 'EN'); setSettings(true) }}>⚙ Tùy chọn AI</button>
            {user.role === 'ADMIN' && onAdmin && <button className="cw-btn" onClick={onAdmin}>Quản trị</button>}
            <button className="cw-btn" onClick={onLogout}>Đăng xuất</button>
          </div>
        </div>
      </aside>
      {navOpen && <div className="cw-scrim" onClick={() => setNavOpen(false)} />}

      <main className="cw-main">
        <header className="cw-top">
          <button className="cw-icon cw-burger" onClick={() => setNavOpen(o => !o)} aria-label="Mở danh sách">☰</button>
          <div className="cw-top-title">{sel ? sel.title : 'SRS Analyzer'}</div>
        </header>
        <div className="cw-scroll" ref={scrollRef}>
          {sel ? <Thread key={sel.id} req={sel} onPatch={patch} flash={flash} /> : (
            <div className="cw-welcome">
              <div className="cw-hero-icon">✨</div>
              <h1>Bạn muốn phân tích <span className="cw-grad-text">yêu cầu</span> nào?</h1>
              <p>Nhập mô tả bên dưới hoặc tải lên file Word (.docx, tối đa 100.000 từ). AI sẽ tạo User Story, Acceptance Criteria, SRS và SwimLane.</p>
              <div className="cw-features">
                {FEATURES.map(([icon, t, d]) => (
                  <div key={t} className="cw-feature"><span>{icon}</span><strong>{t}</strong><small>{d}</small></div>
                ))}
              </div>
              <div className="cw-samples">
                <span className="cw-muted">Thử nhanh:</span>
                {SAMPLES.map(t => <button key={t} className="cw-chip" onClick={() => setPreset({ text: t, n: Date.now() })}>{t}</button>)}
              </div>
            </div>
          )}
        </div>
        <Composer busy={busy} onText={onText} onFile={onFile} preset={preset} />
      </main>

      {toast && <div className={`cw-toast ${toast.type}`} role="status" onClick={() => setToast(null)}>{toast.text}</div>}

      {del && (
        <Modal title="Xóa yêu cầu" onClose={() => setDel(null)}>
          <p>Xóa “{del.title}” cùng toàn bộ phân tích, User Story và lịch sử phiên bản? Không thể hoàn tác.</p>
          <div className="cw-row end"><button className="cw-btn" onClick={() => setDel(null)}>Hủy</button><button className="cw-btn danger" onClick={confirmDelete}>Xóa</button></div>
        </Modal>
      )}
      {settings && (
        <Modal title="Tùy chọn AI" onClose={() => setSettings(false)}>
          <label>Ngôn ngữ kết quả AI
            <select value={lang} onChange={e => setLang(e.target.value)}><option value="EN">English</option><option value="VI">Tiếng Việt</option></select>
          </label>
          <label>Phong cách / ưu tiên<textarea rows={4} value={pref} onChange={e => setPref(e.target.value)} placeholder="Ví dụ: Formal, concise, prioritize security and testability." /></label>
          <div className="cw-row end"><button className="cw-btn" onClick={() => setSettings(false)}>Hủy</button><button className="cw-btn primary" onClick={savePref}>Lưu</button></div>
        </Modal>
      )}
    </div>
  )
}
