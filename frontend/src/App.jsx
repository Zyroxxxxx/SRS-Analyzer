import { useEffect, useState } from 'react'
import { login, register, logout, updatePreference } from './api/authApi'
import {
  fetchRequirements,
  createRequirement,
  deleteRequirement,
  analyzeRequirement,
  getAnalysis,
  exportSrs,
  exportSrsExcel
} from './api/requirementApi'
import {
  fetchAdminDashboard,
  fetchAdminUsers,
  fetchAdminUserDetail,
  createAdminUser,
  updateAdminUser,
  deleteAdminUser,
  exportAdminUsageReport,
  fetchAdminRequirements,
  fetchAdminAudit,
  deleteAdminAudit,
  deleteAllAdminAudit
} from './api/adminApi'
import './App.css'



const UI_LANGUAGE_KEY = 'srs_ui_language'
const UI_TRANSLATIONS = {
  'Đăng nhập': 'Login',
  'Đăng ký ngay': 'Register now',
  'Đăng ký': 'Register',
  'Đăng xuất': 'Logout',
  'Đăng nhập an toàn': 'SECURE LOGIN',
  'ĐĂNG NHẬP AN TOÀN': 'SECURE LOGIN',
  'TẠO TÀI KHOẢN MỚI': 'CREATE NEW ACCOUNT',
  'Chào mừng trở lại': 'Welcome back',
  'Đăng nhập để tiếp tục quản lý các yêu cầu phần mềm.': 'Sign in to continue managing software requirements.',
  'Tài khoản': 'Username',
  'Nhập tài khoản': 'Enter username',
  'Mật khẩu': 'Password',
  'Nhập mật khẩu': 'Enter password',
  'Đang đăng nhập…': 'Signing in…',
  'Đang tạo tài khoản…': 'Creating account…',
  'Đang tạo…': 'Creating…',
  'Vui lòng điền đầy đủ tài khoản, email và mật khẩu.': 'Please fill in username, email, and password.',
  'Mật khẩu tối thiểu 6 ký tự.': 'Password must be at least 6 characters.',
  'Mật khẩu nhập lại không khớp.': 'Passwords do not match.',
  'Đăng ký thất bại. Vui lòng thử lại.': 'Registration failed. Please try again.',
  'Sai tài khoản hoặc mật khẩu.': 'Invalid username or password.',
  'Email': 'Email',
  'Họ và tên (không bắt buộc)': 'Full name (optional)',
  'Họ và tên': 'Full name',
  'Nhập lại mật khẩu': 'Confirm password',
  'Đã có tài khoản?': 'Already have an account?',
  'Chưa có tài khoản?': 'Don’t have an account?',
  'Chuyên viên': 'Analyst',
  'Không gian quản lý yêu cầu': 'Requirement Management Workspace',
  'Nền tảng quản lý đặc tả phần mềm': 'Software Specification Management Platform',
  'Quản lý đặc tả phần mềm': 'Software Specification Management',
  'Không gian làm việc': 'Workspace',
  'Quản trị': 'Admin',
  'Nhập một yêu cầu mới': 'Enter a new requirement',
  'Vui lòng nhập tiêu đề và nội dung yêu cầu.': 'Please enter a requirement title and description.',
  'Ví dụ: Đăng nhập tài khoản': 'Example: Account login',
  'Mô tả người dùng, quy tắc nghiệp vụ, kiểm tra dữ liệu, trường hợp đặc biệt và kết quả mong muốn…': 'Describe users, business rules, data validation, edge cases, and expected results…',
  'Đang lưu…': 'Saving…',
  'Thêm yêu cầu': 'Add requirement',
  'Đang phân tích…': 'Analyzing…',
  'Phân tích': 'Analyze',
  'Danh sách yêu cầu': 'Requirement list',
  'Trạng thái': 'Status',
  'Ngày tạo': 'Created',
  'Thao tác': 'Actions',
  'Xem kết quả': 'View result',
  'Xóa': 'Delete',
  'Chờ phân tích': 'Pending analysis',
  'Đang phân tích': 'Analyzing',
  'Phân tích thất bại': 'Analysis failed',
  'Không thể tải danh sách yêu cầu.': 'Unable to load requirements.',
  'Không thể lưu yêu cầu.': 'Unable to save requirement.',
  'Bạn có chắc muốn xóa yêu cầu này?': 'Are you sure you want to delete this requirement?',
  'Không thể xóa yêu cầu.': 'Unable to delete requirement.',
  'Phân tích AI thất bại.': 'AI analysis failed.',
  'Không thể tải lại kết quả phân tích.': 'Unable to reload analysis result.',
  'Xem lại đặc tả': 'Review specification',
  'Yêu cầu chức năng': 'Functional Requirements',
  'Yêu cầu phi chức năng': 'Non-functional Requirements',
  'Điểm chưa rõ cần xác nhận': 'Ambiguities to confirm',
  'Xuất HTML': 'Export HTML',
  'Xuất Excel': 'Export Excel',
  'Nhà cung cấp': 'Provider',
  'Token đầu vào': 'Input tokens',
  'Token đầu ra': 'Output tokens',
  'Tổng token': 'Total tokens',
  'Trung tâm yêu cầu': 'Requirement Center',
  'Chuyển ngôn ngữ nghiệp vụ thành đặc tả để đội nhóm xem xét, kiểm thử và triển khai.': 'Turn business language into specifications for team review, testing, and delivery.',
  'yêu cầu': 'requirements',
  'Tùy chọn AI': 'AI Preferences',
  'Ngôn ngữ kết quả AI': 'AI Output Language',
  'Lưu tùy chọn': 'Save preferences',
  'Không thể lưu tùy chọn phân tích.': 'Unable to save analysis preferences.',
  'Đã tạo User thành công': 'User created successfully',
  'Đã xóa tài khoản thành công': 'Account deleted successfully',
  'Không thể tải chi tiết người dùng.': 'Unable to load user details.',
  'Không thể xóa tài khoản.': 'Unable to delete account.',
  'Cập nhật thất bại.': 'Update failed.',
  'Đang tải…': 'Loading…',
  'Đóng': 'Close',
  'Đang hoạt động': 'Active',
  'Đã bị khóa': 'Locked',
  'Chưa đặt tên': 'No name set',
  'Khóa tài khoản': 'Lock account',
  'Mở khóa tài khoản': 'Unlock account',
  'Hạn mức token (để trống = không giới hạn)': 'Token quota (empty = unlimited)',
  'Không giới hạn': 'Unlimited',
  'Lưu hạn mức': 'Save quota',
  'Xóa tài khoản': 'Delete account',
  'Đang xóa…': 'Deleting…',
  'Không thể tự xóa hoặc tự hạ quyền tài khoản này.': 'You cannot delete or demote this account.',
  'Admin được phân quyền không thể thay đổi hoặc xóa tài khoản Admin gốc.': 'Delegated admins cannot change or delete the root admin account.',
  'Sở thích prompt AI của người dùng này': 'This user’s AI prompt preference',
  '(chưa thiết lập)': '(not set)',
  'Yêu cầu đã tạo': 'Created requirements',
  'Lịch sử prompt & token': 'Prompt & token history',
  'Mật khẩu ban đầu': 'Initial password',
  'Vai trò': 'Role',
  'Trạng thái tài khoản': 'Account status',
  'Tìm tên, username, email…': 'Search name, username, email…',
  'Tìm người dùng': 'Search users',
  'Không tìm thấy người dùng phù hợp.': 'No matching users found.',
  'Chưa có người dùng nào.': 'No users yet.',
  'Admin gốc': 'Root Admin',
  'Hoạt động': 'Active',
  'Đã khóa': 'Locked',
  'Xem chi tiết': 'View details',
  'Danh sách người dùng': 'User list',
  'người dùng': 'users',
  'Tìm yêu cầu': 'Search requirements',
  'Không tìm thấy yêu cầu phù hợp.': 'No matching requirements found.',
  'Chưa có yêu cầu nào.': 'No requirements yet.',
  'AUDIT LOG': 'AUDIT LOG',
  'Tìm nhật ký': 'Search logs',
  'Không tìm thấy bản ghi phù hợp.': 'No matching records found.',
  'Chưa có nhật ký.': 'No logs yet.',
  'Hệ thống': 'System',
  'Không thể tải nhật ký quản trị.': 'Unable to load audit log.',
  'lượt phân tích': 'analyses',
  'Chưa có hoạt động phân tích AI.': 'No AI analysis activity yet.',
  'Không tìm thấy lượt phân tích phù hợp.': 'No matching analyses found.',
  'Dashboard': 'Dashboard',
  'Theo dõi người dùng, yêu cầu, hoạt động phân tích và mức sử dụng AI.': 'Monitor users, requirements, analysis activity, and AI usage.',
  'Tên hiển thị': 'Display name',
  'TỔNG QUAN': 'OVERVIEW',
  'Tổng quan': 'Overview',
  'Danh sách': 'List',
  'Nhật ký': 'Audit',
  'Tên người dùng': 'Username',
  'Token đã dùng / Hạn mức': 'Tokens used / Quota',
  'Không thể xuất báo cáo.': 'Unable to export report.',
  'Không thể xuất file HTML.': 'Unable to export HTML file.',
  'Không thể xuất file Excel.': 'Unable to export Excel file.',
  'KHÔNG GIAN DỰ ÁN': 'PROJECT WORKSPACE',
  'TẠO YÊU CẦU': 'CREATE REQUIREMENT',
  'HỒ SƠ PHÂN TÍCH': 'ANALYSIS PROFILE',
  'YÊU CẦU CỦA TÔI': 'MY REQUIREMENTS',
  'Mô tả nhu cầu nghiệp vụ bằng ngôn ngữ tự nhiên. Hệ thống có thể chuyển nội dung thành đặc tả có cấu trúc và có thể kiểm thử.': 'Describe the business need in natural language. The system can turn the content into structured, testable specifications.',
  'Tiêu đề yêu cầu': 'Requirement title',
  'Nội dung yêu cầu': 'Requirement description',
  'Không gian riêng tư': 'Private workspace',
  'Chọn ngôn ngữ đầu ra và phong cách mà AI sử dụng khi phân tích và cấu trúc yêu cầu.': 'Choose the output language and style used by AI to analyze and structure requirements.',
  'Chưa có yêu cầu. Hãy thêm yêu cầu đầu tiên để bắt đầu.': 'No requirements yet. Add your first requirement to get started.',
  '● Không gian riêng tư': '● Private workspace',
  'Tạo tài khoản trực tiếp trong hệ thống. Tên hiển thị có thể được quản trị viên điều chỉnh sau khi tạo.': 'Create an account directly in the system. The display name can be adjusted by an administrator after creation.',
  'Đang tải danh sách người dùng…': 'Loading user list…',
  'Đang tải danh sách yêu cầu…': 'Loading requirement list…',
  'Đang tải nhật ký quản trị…': 'Loading admin audit log…',
  'Tạo tài khoản': 'Create account',
  'Người dùng này': 'This user',
  'Token': 'Tokens',
  'Root Admin': 'Root Admin',
  'QUẢN TRỊ HỆ THỐNG': 'SYSTEM ADMINISTRATION',
  'TÀI KHOẢN QUẢN TRỊ': 'ADMINISTRATOR ACCOUNT',
  'QUẢN LÝ NGƯỜI DÙNG': 'USER MANAGEMENT',
  'QUẢN LÝ YÊU CẦU': 'REQUIREMENT MANAGEMENT',
  'QUẢN LÝ NHẬT KÝ': 'AUDIT LOG MANAGEMENT',
  'Thông tin hiển thị của quản trị viên': 'Administrator display information',
  'Quản trị viên': 'Administrator',
  'Người dùng': 'User',
  'Yêu cầu': 'Requirement',
  'Đã phân tích': 'Analyzed',
  'Tổng token AI': 'Total AI tokens',
  'NHẬT KÝ PHÂN TÍCH AI': 'AI ANALYSIS LOG',
  'Các yêu cầu phân tích gần đây': 'Recent analyzed requirements',
  'Tất cả người dùng': 'All users',
  'Tất cả nhà cung cấp': 'All providers',
  'Tất cả hành động': 'All actions',
  'Tất cả vai trò': 'All roles',
  'Tất cả trạng thái': 'All statuses',
  'Từ': 'From',
  'Đến': 'To',
  'Reset': 'Reset',
  'Tìm REQ, người dùng, tiêu đề…': 'Search REQ, user, title…',
  'Tìm người dùng, hành động, nội dung…': 'Search user, action, content…',
  'Tìm người dùng, hành động…': 'Search user, action…',
  'Tìm REQ, tiêu đề, người tạo…': 'Search REQ, title, creator…',
  'Lọc người dùng': 'Filter by user',
  'Lọc nhà cung cấp': 'Filter by provider',
  'Lọc hành động': 'Filter by action',
  'Lọc trạng thái': 'Filter by status',
  'Lọc vai trò': 'Filter by role',
  'Tạo tài khoản mới': 'Create new account',
  'Xuất báo cáo Excel': 'Export Excel report',
  'Xóa đã chọn': 'Delete selected',
  'Xóa toàn bộ': 'Delete all',
  'Chọn tất cả bản ghi đang hiển thị': 'Select all visible records',
  'Chỉ Admin gốc được xóa nhật ký': 'Only the Root Admin can delete audit logs',
  'Lịch sử hoạt động quản trị': 'Administrator activity history',
  'Danh sách yêu cầu toàn hệ thống': 'All system requirements',
  'Đang tải dữ liệu quản trị…': 'Loading administrator data…',
  'Không thể tải dữ liệu quản trị': 'Unable to load administrator data',
  'Thử lại': 'Retry',
  'Quay lại Workspace': 'Back to Workspace',
  'Tên hiển thị không được để trống.': 'Display name cannot be empty.',
  'Đã lưu': 'Saved',
  'Lưu tên': 'Save name',
  'Nhập họ và tên': 'Enter full name',
  'Chỉ quản trị viên mới có quyền thay đổi tên hiển thị tài khoản.': 'Only administrators can change the account display name.',
  'User': 'User',
  'Active': 'Active',
  'Locked': 'Locked',
  'Gemini': 'Gemini',
  'token': 'tokens'
}

function getUiLanguage() { return localStorage.getItem(UI_LANGUAGE_KEY) || 'VI' }
function setUiLanguage(language) {
  localStorage.setItem(UI_LANGUAGE_KEY, language)
  window.dispatchEvent(new CustomEvent('ui-language-changed', { detail: language }))
}
function translateValue(value, language) {
  if (!value) return value
  if (language === 'EN') {
    if (UI_TRANSLATIONS[value]) return UI_TRANSLATIONS[value]
    const count = value.match(/^(\d+) (yêu cầu|người dùng|bản ghi|lượt phân tích)$/)
    if (count) return `${count[1]} ${({ 'yêu cầu':'requirements', 'người dùng':'users', 'bản ghi':'records', 'lượt phân tích':'analyses' })[count[2]]}`
    const owner = value.match(/^(\s*[·•]\s*)bởi(\s+.+)$/i)
    if (owner) return `${owner[1]}by${owner[2]}`
    return value
  }
  const reverse = UI_TRANSLATIONS[value]
  if (reverse) return value
  const pair = Object.entries(UI_TRANSLATIONS).find(([, en]) => en === value)
  if (pair) return pair[0]
  const count = value.match(/^(\d+) (requirements|users|records|analyses)$/)
  if (count) return `${count[1]} ${({ requirements:'yêu cầu', users:'người dùng', records:'bản ghi', analyses:'lượt phân tích' })[count[2]]}`
  return value
}
function UiLanguageSwitcher({ className = '' }) {
  const [language, setLanguage] = useState(getUiLanguage())
  function change(e) { const next = e.target.value; setLanguage(next); setUiLanguage(next) }
  return <label className={`ui-language-switcher ${className}`}><span>🌐</span><select aria-label="Interface language" value={language} onChange={change}><option value="VI">Tiếng Việt</option><option value="EN">English</option></select></label>
}
function useUiLanguage() {
  const [language, setLanguage] = useState(getUiLanguage())
  useEffect(() => { const fn = e => setLanguage(e.detail || getUiLanguage()); window.addEventListener('ui-language-changed', fn); return () => window.removeEventListener('ui-language-changed', fn) }, [])
  return language
}
function applyUiTranslations(language) {
  const walker = document.createTreeWalker(document.body, NodeFilter.SHOW_TEXT)
  const nodes = []
  while (walker.nextNode()) nodes.push(walker.currentNode)
  nodes.forEach(node => {
    const parent = node.parentElement
    if (!parent || parent.closest('[data-i18n-ignore]')) return
    const raw = node.nodeValue
    const trimmed = raw.trim()
    if (!trimmed) return
    const translated = translateValue(trimmed, language)
    if (translated !== trimmed) node.nodeValue = raw.replace(trimmed, translated)
  })
  document.querySelectorAll('input[placeholder], textarea[placeholder]').forEach(el => {
    const translated = translateValue(el.getAttribute('placeholder'), language); if (translated) el.setAttribute('placeholder', translated)
  })
}

const status = {
  PENDING: 'Chờ phân tích',
  ANALYZING: 'Đang phân tích',
  ANALYZED: 'Đã phân tích',
  FAILED: 'Phân tích thất bại'
}

const statusEn = {
  PENDING: 'Pending analysis',
  ANALYZING: 'Analyzing',
  ANALYZED: 'Analyzed',
  FAILED: 'Analysis failed'
}

function getStoredUser() {
  try {
    return JSON.parse(localStorage.getItem('srs_user') || 'null')
  } catch {
    return null
  }
}

/* ============================== ĐĂNG NHẬP / ĐĂNG KÝ ============================== */

function Login({ onLogin, onSwitchToRegister }) {
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  async function submit(e) {
    e.preventDefault()
    setBusy(true)
    setError('')

    try {
      const u = await login({ username, password })
      localStorage.setItem('srs_token', u.token)
      localStorage.setItem('srs_user', JSON.stringify(u))
      onLogin(u)
    } catch (e) {
      setError(e.response?.data?.message || 'Sai tài khoản hoặc mật khẩu.')
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="login-page">
      <UiLanguageSwitcher className="login-language" />
      <div className="login-brand">
        <div className="brand-mark">SR</div>
        <div>
          <strong>Không gian quản lý yêu cầu</strong>
          <span>Nền tảng quản lý đặc tả phần mềm</span>
        </div>
      </div>

      <div className="login-card">
        <div className="login-heading">
          <span>ĐĂNG NHẬP AN TOÀN</span>
          <h1>Chào mừng trở lại</h1>
          <p>Đăng nhập để tiếp tục quản lý các yêu cầu phần mềm.</p>
        </div>

        <form onSubmit={submit}>
          <label>
            Tài khoản
            <input
              autoFocus
              autoComplete="username"
              value={username}
              onChange={e => setUsername(e.target.value)}
              placeholder="Nhập tài khoản"
            />
          </label>

          <label>
            Mật khẩu
            <input
              type="password"
              autoComplete="current-password"
              value={password}
              onChange={e => setPassword(e.target.value)}
              placeholder="Nhập mật khẩu"
            />
          </label>

          {error && <div className="alert">{error}</div>}

          <button className="primary" disabled={busy}>
            {busy ? 'Đang đăng nhập…' : 'Đăng nhập'}
          </button>
        </form>

        <div className="switch-auth">
          Chưa có tài khoản?{' '}
          <button type="button" className="link-btn" onClick={onSwitchToRegister}>
            Đăng ký ngay
          </button>
        </div>
      </div>
    </div>
  )
}

function Register({ onRegistered, onSwitchToLogin }) {
  const [form, setForm] = useState({ username: '', email: '', fullName: '', password: '', confirm: '' })
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  function set(field) {
    return e => setForm(f => ({ ...f, [field]: e.target.value }))
  }

  async function submit(e) {
    e.preventDefault()
    setError('')

    if (!form.username.trim() || !form.email.trim() || !form.password) {
      setError('Vui lòng điền đầy đủ tài khoản, email và mật khẩu.')
      return
    }
    if (form.password.length < 6) {
      setError('Mật khẩu tối thiểu 6 ký tự.')
      return
    }
    if (form.password !== form.confirm) {
      setError('Mật khẩu nhập lại không khớp.')
      return
    }

    setBusy(true)
    try {
      const u = await register({
        username: form.username.trim(),
        email: form.email.trim(),
        password: form.password,
        fullName: form.fullName.trim()
      })
      localStorage.setItem('srs_token', u.token)
      localStorage.setItem('srs_user', JSON.stringify(u))
      onRegistered(u)
    } catch (e) {
      setError(e.response?.data?.message || 'Đăng ký thất bại. Vui lòng thử lại.')
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="login-page">
      <UiLanguageSwitcher className="login-language" />
      <div className="login-brand">
        <div className="brand-mark">SR</div>
        <div>
          <strong>Không gian quản lý yêu cầu</strong>
          <span>Nền tảng quản lý đặc tả phần mềm</span>
        </div>
      </div>

      <div className="login-card">
        <div className="login-heading">
          <span>TẠO TÀI KHOẢN MỚI</span>
          <h1>Đăng ký tài khoản</h1>
          <p>Tạo tài khoản cá nhân để bắt đầu quản lý yêu cầu phần mềm của riêng bạn.</p>
        </div>

        <form onSubmit={submit}>
          <label>
            Tài khoản
            <input autoFocus autoComplete="username" value={form.username} onChange={set('username')} placeholder="Ví dụ: nguyenvana" />
          </label>

          <label>
            Email
            <input type="email" autoComplete="email" value={form.email} onChange={set('email')} placeholder="ban@vidu.com" />
          </label>

          <label>
            Họ và tên (không bắt buộc)
            <input value={form.fullName} onChange={set('fullName')} placeholder="Nguyễn Văn A" />
          </label>

          <label>
            Mật khẩu
            <input type="password" autoComplete="new-password" value={form.password} onChange={set('password')} placeholder={uiLanguage === 'EN' ? 'At least 6 characters' : 'Tối thiểu 6 ký tự'} />
          </label>

          <label>
            Nhập lại mật khẩu
            <input type="password" value={form.confirm} onChange={set('confirm')} placeholder="Nhập lại mật khẩu" />
          </label>

          {error && <div className="alert">{error}</div>}

          <button className="primary" disabled={busy}>
            {busy ? 'Đang tạo tài khoản…' : 'Đăng ký'}
          </button>
        </form>

        <div className="switch-auth">
          Đã có tài khoản?{' '}
          <button type="button" className="link-btn" onClick={onSwitchToLogin}>
            Đăng nhập
          </button>
        </div>
      </div>
    </div>
  )
}

/* ============================== BỐ CỤC CHUNG ============================== */

function Header({ user, onLogout, onAdmin }) {
  return (
    <header className="topbar">
      <UiLanguageSwitcher className="topbar-language" />
      <div className="brand">
        <div className="brand-mark">SR</div>
        <div>
          <strong>Không gian quản lý yêu cầu</strong>
          <small>Quản lý đặc tả phần mềm</small>
        </div>
      </div>

      <nav>
        <button className="nav-active">Không gian làm việc</button>
        {user.role === 'ADMIN' && onAdmin && (
          <button onClick={onAdmin}>Quản trị</button>
        )}
      </nav>

      <div className="account">
        <div className="avatar">{(user.fullName || user.username)[0]}</div>
        <div>
          <strong>{user.fullName || user.username}</strong>
          <span>{user.role === 'ADMIN' ? 'Quản trị viên' : 'Chuyên viên'}</span>
        </div>
        <button className="logout" onClick={onLogout}>Đăng xuất</button>
      </div>
    </header>
  )
}

/* ============================== KHÔNG GIAN LÀM VIỆC (USER) ============================== */

function RequirementComposer({ onSubmit, busy }) {
  const [title, setTitle] = useState('')
  const [desc, setDesc] = useState('')
  const [err, setErr] = useState('')

  function submit(e) {
    e.preventDefault()
    if (!title.trim() || !desc.trim()) {
      setErr('Vui lòng nhập tiêu đề và nội dung yêu cầu.')
      return
    }
    setErr('')
    onSubmit({ title: title.trim(), rawDescription: desc.trim() }).then(() => {
      setTitle('')
      setDesc('')
    })
  }

  return (
    <section className="card composer">
      <div className="section-head">
        <div>
          <span className="eyebrow">TẠO YÊU CẦU</span>
          <h2>Nhập một yêu cầu mới</h2>
        </div>
        <span className="step">01</span>
      </div>

      <p className="muted">
        Mô tả nhu cầu nghiệp vụ bằng ngôn ngữ tự nhiên. Hệ thống có thể chuyển nội dung thành đặc tả có cấu trúc và có thể kiểm thử.
      </p>

      <form onSubmit={submit}>
        <label>
          Tiêu đề yêu cầu
          <input
            value={title}
            onChange={e => setTitle(e.target.value)}
            placeholder="Ví dụ: Đăng nhập tài khoản"
          />
        </label>

        <label>
          Nội dung yêu cầu
          <textarea
            value={desc}
            onChange={e => setDesc(e.target.value)}
            rows="8"
            placeholder="Mô tả người dùng, quy tắc nghiệp vụ, kiểm tra dữ liệu, trường hợp đặc biệt và kết quả mong muốn…"
          />
        </label>

        {err && <div className="field-error">{err}</div>}

        <button className="primary" disabled={busy}>
          {busy ? 'Đang lưu…' : 'Thêm yêu cầu'}
        </button>
      </form>
    </section>
  )
}

function RequirementList({ items, loading, onDelete, onAnalyze, onView, analyzing }) {
  return (
    <section className="card list-card">
      <div className="section-head">
        <div>
          <span className="eyebrow">YÊU CẦU CỦA TÔI</span>
          <h2>Danh sách yêu cầu <span className="count">{items.length}</span></h2>
        </div>
        <span className="live-dot">Đang hoạt động</span>
      </div>

      {loading ? (
        <div className="empty">Đang tải danh sách yêu cầu…</div>
      ) : items.length === 0 ? (
        <div className="empty">Chưa có yêu cầu. Hãy thêm yêu cầu đầu tiên để bắt đầu.</div>
      ) : (
        <div className="req-table">
          <div className="table-head">
            <span>Yêu cầu</span>
            <span>Trạng thái</span>
            <span>Ngày tạo</span>
            <span>Thao tác</span>
          </div>

          {items.map(r => (
            <article className="req-item" key={r.id}>
              <div>
                <div className="req-id">
                  REQ-{String(r.id).padStart(4, '0')}
                  {r.ownerUsername && <span className="req-owner"> · bởi {r.ownerUsername}</span>}
                </div>
                <h3>{r.title}</h3>
                <p>{r.rawDescription}</p>
              </div>

              <div>
                <span className={`badge badge-${r.status.toLowerCase()}`}>
                  {status[r.status] || r.status}
                </span>
              </div>

              <time>
                {r.createdAt ? new Date(r.createdAt).toLocaleDateString('vi-VN') : ''}
              </time>

              <div className="actions">
                {r.status === 'ANALYZED' && (
                  <button onClick={() => onView(r.id)}>Xem kết quả</button>
                )}
                <button
                  onClick={() => onAnalyze(r.id)}
                  disabled={analyzing === r.id}
                >
                  {analyzing === r.id ? 'Đang phân tích…' : 'Phân tích'}
                </button>
                <button className="danger-link" onClick={() => onDelete(r.id)}>
                  Xóa
                </button>
              </div>
            </article>
          ))}
        </div>
      )}
    </section>
  )
}

function AnalysisDrawer({ data, onClose, onExportHtml, onExportExcel }) {
  if (!data) return null

  const render = x => {
    try {
      const a = JSON.parse(x)
      if (Array.isArray(a)) {
        return (
          <ul>
            {a.map((v, i) => (
              <li key={i}>{typeof v === 'string' ? v : JSON.stringify(v)}</li>
            ))}
          </ul>
        )
      }
      return <pre>{x}</pre>
    } catch {
      return <p>{x}</p>
    }
  }

  return (
    <div className="drawer-backdrop" onClick={onClose}>
      <aside className="drawer" onClick={e => e.stopPropagation()}>
        <div className="drawer-top">
          <div>
            <span className="eyebrow">KẾT QUẢ PHÂN TÍCH</span>
            <h2>Xem lại đặc tả</h2>
          </div>
          <button onClick={onClose}>×</button>
        </div>

        <p className="summary">{data.summary}</p>

        <div className="analysis-grid">
          <div>
            <h4>Yêu cầu chức năng</h4>
            {render(data.functionalRequirements)}
          </div>

          <div>
            <h4>Yêu cầu phi chức năng</h4>
            {render(data.nonFunctionalRequirements)}
          </div>

          <div>
            <h4>UserStory</h4>
            {render(data.userStories)}
          </div>

          <div>
            <h4>Acceptance Criteria</h4>
            {render(data.acceptanceCriteria)}
          </div>

          <div className="wide">
            <h4>Điểm chưa rõ cần xác nhận</h4>
            {render(data.ambiguousNotes)}
          </div>
        </div>

        <div className="drawer-actions">
          <button className="secondary compact" onClick={() => onExportHtml(data.requirementId)}>
            Xuất PDF (HTML)
          </button>
          <button className="secondary compact" onClick={() => onExportExcel(data.requirementId)}>
            Xuất Excel (kèm Ma trận truy vết)
          </button>
        </div>

        <div className="usage">
          <span>Nhà cung cấp <b>{data.provider}</b></span>
          <span>Token đầu vào <b>{data.promptTokens}</b></span>
          <span>Token đầu ra <b>{data.outputTokens}</b></span>
          <span>Tổng token <b>{data.totalTokens}</b></span>
        </div>
      </aside>
    </div>
  )
}

function UserApp({ user, onLogout, onAdmin, onUserUpdate }) {
  const [items, setItems] = useState([])
  const [loading, setLoading] = useState(true)
  const [busy, setBusy] = useState(false)
  const [analyzing, setAnalyzing] = useState(null)
  const [analysis, setAnalysis] = useState(null)
  const [pref, setPref] = useState(user.aiPreference || '')
  const [analysisLanguage, setAnalysisLanguage] = useState(user.analysisLanguage || 'EN')
  const [saved, setSaved] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    fetchRequirements()
      .then(setItems)
      .catch(e => setError(e.response?.data?.message || 'Không thể tải danh sách yêu cầu.'))
      .finally(() => setLoading(false))
  }, [])

  async function add(payload) {
    setBusy(true)
    try {
      const x = await createRequirement(payload)
      setItems(v => [x, ...v])
      setError('')
    } catch (e) {
      setError(e.response?.data?.message || 'Không thể lưu yêu cầu.')
    } finally {
      setBusy(false)
    }
  }

  async function remove(id) {
    if (!window.confirm('Bạn có chắc muốn xóa yêu cầu này?')) return
    try {
      await deleteRequirement(id)
      setItems(v => v.filter(x => x.id !== id))
      if (analysis?.requirementId === id) setAnalysis(null)
    } catch (e) {
      setError(e.response?.data?.message || 'Không thể xóa yêu cầu.')
    }
  }

  async function analyze(id) {
    setAnalyzing(id)
    setError('')
    try {
      const x = await analyzeRequirement(id)
      setAnalysis(x)
      setItems(v => v.map(r => r.id === id ? { ...r, status: 'ANALYZED' } : r))
    } catch (e) {
      setError(e.response?.data?.message || 'Phân tích AI thất bại.')
      // Neu that bai, tai lai danh sach de thay dung trang thai FAILED tu backend
      fetchRequirements().then(setItems).catch(() => {})
    } finally {
      setAnalyzing(null)
    }
  }

  async function viewAnalysis(id) {
    setError('')
    try {
      const x = await getAnalysis(id)
      setAnalysis(x)
    } catch (e) {
      setError(e.response?.data?.message || 'Không thể tải lại kết quả phân tích.')
    }
  }

  async function savePref() {
    try {
      const u = await updatePreference(pref, analysisLanguage)
      onUserUpdate(u)
      setSaved(true)
      setTimeout(() => setSaved(false), 1800)
    } catch (e) {
      setError(e.response?.data?.message || 'Không thể lưu tùy chọn phân tích.')
    }
  }


  async function handleExportHtml(id) {
    try {
      await exportSrs(id)
    } catch (e) {
      setError(e.response?.data?.message || 'Không thể xuất file HTML.')
    }
  }

  async function handleExportExcel(id) {
    try {
      await exportSrsExcel(id)
    } catch (e) {
      setError(e.response?.data?.message || 'Không thể xuất file Excel.')
    }
  }

  return (
    <>
      <Header user={user} onLogout={onLogout} onAdmin={onAdmin} />

      <main className="page">
        <div className="page-title">
          <div>
            <span className="eyebrow">KHÔNG GIAN DỰ ÁN</span>
            <h1>Trung tâm yêu cầu</h1>
            <p>Chuyển ngôn ngữ nghiệp vụ thành đặc tả để đội nhóm xem xét, kiểm thử và triển khai.</p>
          </div>
          <div className="title-meta">
            <span>{items.length} yêu cầu</span>
            <span className="secure">● Không gian riêng tư</span>
          </div>
        </div>

        {error && <div className="alert page-alert">{error}</div>}

        <div className="workspace-grid">
          <RequirementComposer onSubmit={add} busy={busy} />

          <div className="side-stack">
            <section className="card preference">
              <div className="section-head">
                <div>
                  <span className="eyebrow">HỒ SƠ PHÂN TÍCH</span>
                  <h2>Tùy chọn AI</h2>
                </div>
              </div>
              <p className="muted">Chọn ngôn ngữ đầu ra và phong cách mà AI sử dụng khi phân tích và cấu trúc yêu cầu.</p>
              <label className="language-select-label">Ngôn ngữ kết quả AI
                <select
                  value={analysisLanguage}
                  onChange={e => setAnalysisLanguage(e.target.value)}
                >
                  <option value="EN">English</option>
                  <option value="VI">Tiếng Việt</option>
                </select>
              </label>
              <textarea
                value={pref}
                onChange={e => setPref(e.target.value)}
                rows="5"
                placeholder="Ví dụ: Formal, concise, prioritize security and testability."
              />
              <button className="secondary" onClick={savePref}>
                {saved ? 'Đã lưu' : 'Lưu tùy chọn'}
              </button>
            </section>
          </div>
        </div>

        <RequirementList
          items={items}
          loading={loading}
          onDelete={remove}
          onAnalyze={analyze}
          onView={viewAnalysis}
          analyzing={analyzing}
        />
      </main>

      <AnalysisDrawer
        data={analysis}
        onClose={() => setAnalysis(null)}
        onExportHtml={handleExportHtml}
        onExportExcel={handleExportExcel}
      />
    </>
  )
}

/* ============================== TRANG QUẢN TRỊ (ADMIN) ============================== */

const ROLE_LABEL = { ADMIN: 'Quản trị viên', USER: 'Người dùng' }

function UserDetailDrawer({ userId, currentUserId, onClose, onChanged, onDeleted }) {
  const [detail, setDetail] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [nameInput, setNameInput] = useState('')
  const [quotaInput, setQuotaInput] = useState('')
  const [savingField, setSavingField] = useState('')

  useEffect(() => {
    if (!userId) return
    setLoading(true)
    setDetail(null)
    setError('')
    fetchAdminUserDetail(userId)
      .then(d => {
        setDetail(d)
        setNameInput(d.fullName || '')
        setQuotaInput(d.tokenQuota ?? '')
      })
      .catch(e => setError(e.response?.data?.message || (uiLanguage === 'EN' ? 'Unable to load user details.' : 'Không thể tải chi tiết người dùng.')))
      .finally(() => setLoading(false))
  }, [userId])

  async function removeUser() {
    if (!detail || Number(detail.id) === Number(currentUserId)) return
    const name = detail.fullName || detail.username
    if (!window.confirm(`Xóa tài khoản ${name}? Toàn bộ Requirement và dữ liệu AI của tài khoản này sẽ bị xóa.`)) return
    setSavingField('delete')
    setError('')
    try {
      await deleteAdminUser(userId)
      onDeleted()
    } catch (e) {
      setError(e.response?.data?.message || (uiLanguage === 'EN' ? 'Unable to delete account.' : 'Không thể xóa tài khoản.'))
    } finally {
      setSavingField('')
    }
  }

  async function applyUpdate(payload, field) {
    setSavingField(field)
    setError('')
    try {
      const refreshed = await updateAdminUser(userId, payload)
      setDetail(prev => ({ ...prev, ...refreshed }))
      if (payload.fullName !== undefined) setNameInput(refreshed.fullName || '')
      if (payload.tokenQuota !== undefined) setQuotaInput(refreshed.tokenQuota ?? '')
      onChanged(refreshed)
    } catch (e) {
      setError(e.response?.data?.message || (uiLanguage === 'EN' ? 'Update failed.' : 'Cập nhật thất bại.'))
    } finally {
      setSavingField('')
    }
  }

  const uiLanguage = useUiLanguage()

  if (!userId) return null

  const isSelf = Number(userId) === Number(currentUserId)
  const isRootAdmin = Boolean(detail?.rootAdmin)

  return (
    <div className="drawer-backdrop" onClick={onClose}>
      <aside className="drawer" onClick={e => e.stopPropagation()}>
        <div className="drawer-top">
          <div>
            <span className="eyebrow">{uiLanguage === 'EN' ? 'USER DETAILS' : 'CHI TIẾT NGƯỜI DÙNG'}</span>
            <h2>{detail ? (detail.fullName || detail.username) : (uiLanguage === 'EN' ? 'Loading…' : 'Đang tải…')}</h2>
          </div>
          <button aria-label={uiLanguage === 'EN' ? 'Close' : 'Đóng'} onClick={onClose}>×</button>
        </div>

        {error && <div className="alert page-alert">{error}</div>}
        {loading && <div className="empty">{uiLanguage === 'EN' ? 'Loading…' : 'Đang tải…'}</div>}

        {!loading && detail && (
          <>
            <div className="summary user-summary">
              <span><b>{uiLanguage === 'EN' ? 'Account:' : 'Tài khoản:'}</b> {detail.username} ({detail.email})</span>
              <span><b>{uiLanguage === 'EN' ? 'Role:' : 'Vai trò:'}</b> {ROLE_LABEL[detail.role] || detail.role}</span>
              <span><b>{uiLanguage === 'EN' ? 'Status:' : 'Trạng thái:'}</b> {detail.active ? (uiLanguage === 'EN' ? 'Active' : 'Đang hoạt động') : (uiLanguage === 'EN' ? 'Locked' : 'Đã bị khóa')}</span>
              <span><b>{uiLanguage === 'EN' ? 'Total Requirements:' : 'Tổng Requirement:'}</b> {detail.requirementCount || 0}</span>
              <span><b>{uiLanguage === 'EN' ? 'AI analyses:' : 'Lượt phân tích AI:'}</b> {detail.analysisCount || 0}</span>
              <span><b>{uiLanguage === 'EN' ? 'Total tokens used:' : 'Tổng token đã dùng:'}</b> {Number(detail.totalTokens || 0).toLocaleString()}</span>
            </div>

            <div className="admin-controls">
              <div className="admin-control-row">
                <label>{uiLanguage === 'EN' ? 'Display name' : 'Họ tên hiển thị'}</label>
                <div className="text-edit">
                  <input value={nameInput} onChange={e => setNameInput(e.target.value)} placeholder={uiLanguage === 'EN' ? 'No name set' : 'Chưa đặt tên'} />
                  <button className="secondary compact" disabled={savingField === 'fullName' || !nameInput.trim()} onClick={() => applyUpdate({ fullName: nameInput.trim() }, 'fullName')}>
                    {savingField === 'fullName' ? (uiLanguage === 'EN' ? 'Saving…' : 'Đang lưu…') : (uiLanguage === 'EN' ? 'Save name' : 'Lưu tên')}
                  </button>
                </div>
              </div>

              <div className="admin-control-row">
                <label>{uiLanguage === 'EN' ? 'Role' : 'Vai trò'}</label>
                <select value={detail.role} disabled={savingField === 'role' || isSelf || isRootAdmin} onChange={e => applyUpdate({ role: e.target.value }, 'role')}>
                  <option value="USER">{uiLanguage === 'EN' ? 'User' : 'Người dùng'}</option>
                  <option value="ADMIN">{uiLanguage === 'EN' ? 'Administrator' : 'Quản trị viên'}</option>
                </select>
              </div>

              <div className="admin-control-row">
                <label>{uiLanguage === 'EN' ? 'Account status' : 'Trạng thái tài khoản'}</label>
                <button className={`account-lock-button ${detail.active ? 'danger-button' : 'secondary'}`} disabled={savingField === 'active' || isSelf || isRootAdmin} onClick={() => applyUpdate({ active: !detail.active }, 'active')}>
                  {savingField === 'active' ? (uiLanguage === 'EN' ? 'Saving…' : 'Đang lưu…') : detail.active ? (uiLanguage === 'EN' ? 'Lock account' : 'Khóa tài khoản') : (uiLanguage === 'EN' ? 'Unlock account' : 'Mở khóa tài khoản')}
                </button>
              </div>

              <div className="admin-control-row">
                <label>{uiLanguage === 'EN' ? 'Token quota (empty = unlimited)' : 'Hạn mức token (để trống = không giới hạn)'}</label>
                <div className="quota-edit">
                  <input type="number" min="0" value={quotaInput} onChange={e => setQuotaInput(e.target.value)} placeholder={uiLanguage === 'EN' ? 'Unlimited' : 'Không giới hạn'} />
                  <button className="secondary compact" disabled={savingField === 'tokenQuota'} onClick={() => applyUpdate({ tokenQuota: quotaInput === '' ? 0 : Number(quotaInput) }, 'tokenQuota')}>
                    {savingField === 'tokenQuota' ? (uiLanguage === 'EN' ? 'Saving…' : 'Đang lưu…') : (uiLanguage === 'EN' ? 'Save quota' : 'Lưu hạn mức')}
                  </button>
                </div>
              </div>
            </div>

            {isSelf ? (
              <div className="admin-self-protection">
                <strong>{uiLanguage === 'EN' ? 'Current administrator account' : 'Tài khoản quản trị hiện tại'}</strong>
                <span>{uiLanguage === 'EN' ? 'You cannot delete or demote this account.' : 'Không thể tự xóa hoặc tự hạ quyền tài khoản này.'}</span>
              </div>
            ) : isRootAdmin ? (
              <div className="admin-self-protection">
                <strong>{uiLanguage === 'EN' ? 'System Root Admin' : 'Admin gốc của hệ thống'}</strong>
                <span>{uiLanguage === 'EN' ? 'Delegated admins cannot change or delete the Root Admin account.' : 'Admin được phân quyền không thể thay đổi hoặc xóa tài khoản Admin gốc.'}</span>
              </div>
            ) : (
              <div className="drawer-danger-zone">
                <div><strong>{uiLanguage === 'EN' ? 'Delete account' : 'Xóa tài khoản'}</strong><span>{uiLanguage === 'EN' ? 'This action permanently deletes the account, Requirements, and related AI data.' : 'Thao tác này xóa vĩnh viễn tài khoản, Requirement và dữ liệu AI liên quan.'}</span></div>
                <button className="danger-button" disabled={savingField === 'delete'} onClick={removeUser}>{savingField === 'delete' ? (uiLanguage === 'EN' ? 'Deleting…' : 'Đang xóa…') : (uiLanguage === 'EN' ? 'Delete account' : 'Xóa tài khoản')}</button>
              </div>
            )}

            <div className="analysis-grid">
              <div className="wide">
                <h4>{uiLanguage === 'EN' ? 'This user’s AI prompt preference' : 'Sở thích prompt AI của người dùng này'}</h4>
                <p>{detail.aiPreference || (uiLanguage === 'EN' ? '(not set)' : '(chưa thiết lập)')}</p>
              </div>
            </div>

            <section className="usage-table user-detail-section">
              <h4>{uiLanguage === 'EN' ? `Created requirements (${(detail.requirements || []).length})` : `Yêu cầu đã tạo (${(detail.requirements || []).length})`}</h4>
              {(detail.requirements || []).length === 0 ? <div className="empty">{uiLanguage === 'EN' ? 'This user has not created any requirements.' : 'Người dùng này chưa tạo yêu cầu nào.'}</div> : (detail.requirements || []).map(r => (
                <article key={r.id}>
                  <div><div className="req-id">REQ-{String(r.id).padStart(4, '0')}</div><h3>{r.title}</h3><p>{r.rawDescription}</p></div>
                  <span className={`badge badge-${String(r.status || '').toLowerCase()}`}>{uiLanguage === 'EN' ? (statusEn[r.status] || r.status) : (status[r.status] || r.status)}</span>
                </article>
              ))}
            </section>

            <section className="usage-table user-detail-section">
              <h4>{uiLanguage === 'EN' ? `Prompt & token history (${(detail.usageHistory || []).length})` : `Lịch sử prompt & token (${(detail.usageHistory || []).length})`}</h4>
              {(detail.usageHistory || []).length === 0 ? <div className="empty">{uiLanguage === 'EN' ? 'No AI analysis activity yet.' : 'Chưa có lượt phân tích AI nào.'}</div> : (detail.usageHistory || []).map(x => (
                <article key={x.id}>
                  <div><div className="req-id">REQ-{String(x.requirementId).padStart(4, '0')}</div><h3>{x.requirementTitle}</h3><p>{x.prompt}</p></div>
                  <div className="token-box"><span>{x.provider || 'AI'}</span><b>{Number(x.totalTokens || 0).toLocaleString()}</b><small>tokens</small></div>
                </article>
              ))}
            </section>
          </>
        )}
      </aside>
    </div>
  )
}

function CreateUserDrawer({ open, onClose, onCreated }) {
  const uiLanguage = useUiLanguage()
  const [form, setForm] = useState({ username: '', email: '', fullName: '', password: '', role: 'USER' })
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  function set(field) { return e => setForm(f => ({ ...f, [field]: e.target.value })) }

  async function submit(e) {
    e.preventDefault(); setError('')
    if (!form.username.trim() || !form.email.trim() || !form.password) { setError('Vui lòng điền tài khoản, email và mật khẩu.'); return }
    if (form.password.length < 6) { setError('Mật khẩu tối thiểu 6 ký tự.'); return }
    setBusy(true)
    try {
      await createAdminUser({ username: form.username.trim(), email: form.email.trim(), fullName: form.fullName.trim(), password: form.password, role: form.role })
      setForm({ username: '', email: '', fullName: '', password: '', role: 'USER' }); onCreated()
    } catch (e) { setError(e.response?.data?.message || 'Tạo tài khoản thất bại.') }
    finally { setBusy(false) }
  }

  if (!open) return null
  return <div className="drawer-backdrop" onClick={onClose}><aside className="drawer" onClick={e => e.stopPropagation()}>
    <div className="drawer-top"><div><span className="eyebrow">{uiLanguage === 'EN' ? 'USER MANAGEMENT' : 'QUẢN LÝ NGƯỜI DÙNG'}</span><h2>{uiLanguage === 'EN' ? 'Create new account' : 'Tạo tài khoản mới'}</h2></div><button aria-label="Đóng" onClick={onClose}>×</button></div>
    <p className="muted">{uiLanguage === 'EN' ? 'Create an account directly in the system. The display name can be adjusted by an administrator after creation.' : 'Tạo tài khoản trực tiếp trong hệ thống. Tên hiển thị có thể được quản trị viên điều chỉnh sau khi tạo.'}</p>
    <form onSubmit={submit} className="create-user-form">
      <label>{uiLanguage === 'EN' ? 'Username' : 'Tài khoản'}<input value={form.username} onChange={set('username')} autoComplete="username" placeholder="Ví dụ: nguyenvana" /></label>
      <label>Email<input type="email" value={form.email} onChange={set('email')} autoComplete="email" placeholder="ban@vidu.com" /></label>
      <label>{uiLanguage === 'EN' ? 'Full name' : 'Họ và tên'}<input value={form.fullName} onChange={set('fullName')} placeholder="Nguyễn Văn A" /></label>
      <label>{uiLanguage === 'EN' ? 'Initial password' : 'Mật khẩu ban đầu'}<input type="password" value={form.password} onChange={set('password')} autoComplete="new-password" placeholder={uiLanguage === 'EN' ? 'At least 6 characters' : 'Tối thiểu 6 ký tự'} /></label>
      <label>{uiLanguage === 'EN' ? 'Role' : 'Vai trò'}<select value={form.role} onChange={set('role')}><option value="USER">{uiLanguage === 'EN' ? 'User' : 'Người dùng'}</option><option value="ADMIN">{uiLanguage === 'EN' ? 'Administrator' : 'Quản trị viên'}</option></select></label>
      {error && <div className="alert">{error}</div>}
      <button className="primary" disabled={busy}>{busy ? (uiLanguage === 'EN' ? 'Creating…' : 'Đang tạo…') : (uiLanguage === 'EN' ? 'Create account' : 'Tạo tài khoản')}</button>
    </form>
  </aside></div>
}

function AdminUsersTab({ onOpenUser, onCreateUser }) {
  const uiLanguage = useUiLanguage()
  const [users, setUsers] = useState([]), [search, setSearch] = useState('')
  const [roleFilter, setRoleFilter] = useState('ALL'), [statusFilter, setStatusFilter] = useState('ALL'), [dateFrom, setDateFrom] = useState(''), [dateTo, setDateTo] = useState('')
  const [loading, setLoading] = useState(true), [error, setError] = useState('')
  useEffect(() => { fetchAdminUsers().then(setUsers).catch(e => setError(e.response?.data?.message || 'Không thể tải danh sách người dùng.')).finally(() => setLoading(false)) }, [])
  const filtered = users.filter(u => {
    const q = search.trim().toLowerCase()
    const textOk = !q || [u.fullName, u.username, u.email, u.role].filter(Boolean).some(v => String(v).toLowerCase().includes(q))
    const roleOk = roleFilter === 'ALL' || u.role === roleFilter
    const statusOk = statusFilter === 'ALL' || (statusFilter === 'ACTIVE' ? u.active : !u.active)
    const created = u.createdAt ? new Date(u.createdAt) : null
    const fromOk = !dateFrom || (created && created >= new Date(dateFrom + 'T00:00:00'))
    const toOk = !dateTo || (created && created <= new Date(dateTo + 'T23:59:59.999'))
    return textOk && roleOk && statusOk && fromOk && toOk
  })
  function resetFilters() { setSearch(''); setRoleFilter('ALL'); setStatusFilter('ALL'); setDateFrom(''); setDateTo('') }
  return <section className="card admin-log"><div className="section-head admin-section-head-responsive"><div><span className="eyebrow">QUẢN LÝ NGƯỜI DÙNG</span><h2>Danh sách người dùng</h2></div><div className="admin-toolbar-actions"><span className="count">{filtered.length}/{users.length} người dùng</span><button className="secondary compact" onClick={onCreateUser}>+ {uiLanguage === 'EN' ? 'Create new account' : 'Tạo tài khoản mới'}</button></div></div>
    <div className="admin-filters">
      <div className="search-box"><span aria-hidden="true">⌕</span><input value={search} onChange={e => setSearch(e.target.value)} placeholder="Tìm tên, username, email…" aria-label="Tìm người dùng" /></div>
      <select value={roleFilter} onChange={e => setRoleFilter(e.target.value)} aria-label="Lọc vai trò"><option value="ALL">Tất cả vai trò</option><option value="USER">User</option><option value="ADMIN">Admin</option></select>
      <select value={statusFilter} onChange={e => setStatusFilter(e.target.value)} aria-label="Lọc trạng thái"><option value="ALL">Tất cả trạng thái</option><option value="ACTIVE">Đang hoạt động</option><option value="LOCKED">Đã khóa</option></select>
      <label className="date-filter">Từ <input type="date" value={dateFrom} onChange={e => setDateFrom(e.target.value)} /></label><label className="date-filter">Đến <input type="date" value={dateTo} onChange={e => setDateTo(e.target.value)} /></label>
      <button className="filter-reset" onClick={resetFilters}>Reset</button>
    </div>
    {error && <div className="alert page-alert">{error}</div>}{loading ? <div className="empty">Đang tải…</div> : filtered.length === 0 ? <div className="empty">{users.length ? 'Không tìm thấy người dùng phù hợp.' : 'Chưa có người dùng nào.'}</div> : <div className="user-table">
      <div className="user-table-head"><span>Người dùng</span><span>Vai trò</span><span>Trạng thái</span><span>Token</span><span>Yêu cầu</span><span>Thao tác</span></div>
      {filtered.map(u => <div className="user-table-row" key={u.id}>
        <div><strong>{u.fullName || 'Chưa đặt tên'}</strong><small>@{u.username}</small></div><span>{u.rootAdmin ? 'Admin gốc' : u.role === 'ADMIN' ? 'Admin' : 'User'}</span><span>{u.active ? 'Đang hoạt động' : 'Đã bị khóa'}</span><span>{Number(u.totalTokensUsed || 0).toLocaleString()} / {u.tokenQuota == null ? '∞' : Number(u.tokenQuota).toLocaleString()}</span><span>{u.requirementCount || 0}</span><button className="secondary compact" onClick={() => onOpenUser(u.id)}>Xem chi tiết</button>
      </div>)}
    </div>}
  </section>
}

function AdminRequirementsTab() {
  const uiLanguage = useUiLanguage()
  const [items, setItems] = useState([]), [search, setSearch] = useState(''), [statusFilter, setStatusFilter] = useState('ALL'), [dateFrom, setDateFrom] = useState(''), [dateTo, setDateTo] = useState('')
  const [loading, setLoading] = useState(true), [error, setError] = useState('')
  useEffect(() => { fetchAdminRequirements().then(setItems).catch(e => setError(e.response?.data?.message || 'Không thể tải danh sách yêu cầu.')).finally(() => setLoading(false)) }, [])
  const filtered = items.filter(r => {
    const q = search.trim().toLowerCase()
    const textOk = !q || [r.id, r.title, r.username, r.fullName, r.status].filter(v => v !== null && v !== undefined).some(v => String(v).toLowerCase().includes(q))
    const statusOk = statusFilter === 'ALL' || r.status === statusFilter
    const created = r.createdAt ? new Date(r.createdAt) : null
    const fromOk = !dateFrom || (created && created >= new Date(dateFrom + 'T00:00:00'))
    const toOk = !dateTo || (created && created <= new Date(dateTo + 'T23:59:59.999'))
    return textOk && statusOk && fromOk && toOk
  })
  function resetFilters() { setSearch(''); setStatusFilter('ALL'); setDateFrom(''); setDateTo('') }
  return <section className="card admin-log"><div className="section-head admin-section-head-responsive"><div><span className="eyebrow">QUẢN LÝ YÊU CẦU</span><h2>Danh sách yêu cầu toàn hệ thống</h2></div><div className="admin-toolbar-actions"><span className="count">{filtered.length}/{items.length} yêu cầu</span></div></div>
    <div className="admin-filters"><div className="search-box"><span aria-hidden="true">⌕</span><input value={search} onChange={e => setSearch(e.target.value)} placeholder="Tìm REQ, tiêu đề, người tạo…" aria-label="Tìm yêu cầu" /></div><select value={statusFilter} onChange={e => setStatusFilter(e.target.value)} aria-label="Lọc trạng thái"><option value="ALL">Tất cả trạng thái</option><option value="PENDING">Chờ phân tích</option><option value="ANALYZING">Đang phân tích</option><option value="ANALYZED">Đã phân tích</option><option value="FAILED">Phân tích thất bại</option></select><label className="date-filter">Từ <input type="date" value={dateFrom} onChange={e => setDateFrom(e.target.value)} /></label><label className="date-filter">Đến <input type="date" value={dateTo} onChange={e => setDateTo(e.target.value)} /></label><button className="filter-reset" onClick={resetFilters}>Reset</button></div>
    {error && <div className="alert page-alert">{error}</div>}{loading ? <div className="empty">Đang tải…</div> : filtered.length === 0 ? <div className="empty">{items.length ? 'Không tìm thấy yêu cầu phù hợp.' : 'Chưa có yêu cầu nào.'}</div> : <div className="admin-requirement-list">{filtered.map(r => <article className="admin-requirement-row" key={r.id}><div className="admin-requirement-main"><div className="req-id">REQ-{String(r.id).padStart(4, '0')}</div><h3>{r.title}</h3><p>{r.fullName || 'Chưa có tên'} <span>·</span> {r.username || 'Không xác định'}</p></div><span className={`badge badge-${String(r.status || '').toLowerCase()}`}>{uiLanguage === 'EN' ? (statusEn[r.status] || r.status) : (status[r.status] || r.status)}</span><time>{r.createdAt ? new Date(r.createdAt).toLocaleString('vi-VN') : '—'}</time></article>)}</div>}
  </section>
}

function AdminAuditTab({ user }) {
  const [items, setItems] = useState([]), [search, setSearch] = useState(''), [actionFilter, setActionFilter] = useState('ALL'), [userFilter, setUserFilter] = useState('ALL'), [dateFrom, setDateFrom] = useState(''), [dateTo, setDateTo] = useState('')
  const [selected, setSelected] = useState([]), [loading, setLoading] = useState(true), [error, setError] = useState('')
  const canDelete = Boolean(user?.rootAdmin)
  async function load() { setLoading(true); try { setItems(await fetchAdminAudit()); setSelected([]); setError('') } catch (e) { setError(e.response?.data?.message || 'Không thể tải nhật ký quản trị.') } finally { setLoading(false) } }
  useEffect(() => { load() }, [])
  const actions = [...new Set(items.map(a => a.action).filter(Boolean))].sort()
  const usernames = [...new Set(items.map(a => a.username).filter(Boolean))].sort()
  const filtered = items.filter(a => {
    const q = search.trim().toLowerCase()
    const textOk = !q || [a.username, a.action, a.details, a.requirementId, a.requirementTitle].filter(v => v !== null && v !== undefined).some(v => String(v).toLowerCase().includes(q))
    const actionOk = actionFilter === 'ALL' || a.action === actionFilter
    const userOk = userFilter === 'ALL' || a.username === userFilter
    const created = a.createdAt ? new Date(a.createdAt) : null
    const fromOk = !dateFrom || (created && created >= new Date(dateFrom + 'T00:00:00'))
    const toOk = !dateTo || (created && created <= new Date(dateTo + 'T23:59:59.999'))
    return textOk && actionOk && userOk && fromOk && toOk
  })
  const allVisibleSelected = filtered.length > 0 && filtered.every(a => selected.includes(a.id))
  function toggle(id) { setSelected(v => v.includes(id) ? v.filter(x => x !== id) : [...v, id]) }
  function toggleAll() { setSelected(allVisibleSelected ? [] : filtered.map(a => a.id)) }
  function resetFilters() { setSearch(''); setActionFilter('ALL'); setUserFilter('ALL'); setDateFrom(''); setDateTo(''); setSelected([]) }
  async function removeOne(id) { if (!canDelete || !window.confirm('Bạn có chắc muốn xóa nhật ký này?')) return; try { await deleteAdminAudit(id); await load() } catch (e) { setError(e.response?.data?.message || 'Không thể xóa nhật ký.') } }
  async function removeSelected() { if (!canDelete || !selected.length || !window.confirm(`Bạn có chắc muốn xóa ${selected.length} nhật ký đã chọn?`)) return; try { await Promise.all(selected.map(id => deleteAdminAudit(id))); await load() } catch (e) { setError(e.response?.data?.message || 'Không thể xóa nhật ký đã chọn.') } }
  async function removeAll() { if (!canDelete || !items.length || !window.confirm('Bạn có chắc muốn xóa toàn bộ nhật ký quản trị? Hành động này không thể hoàn tác.')) return; try { await deleteAllAdminAudit(); await load() } catch (e) { setError(e.response?.data?.message || 'Không thể xóa toàn bộ nhật ký.') } }
  return <section className="card admin-log"><div className="section-head admin-section-head-responsive"><div><span className="eyebrow">AUDIT LOG</span><h2>Lịch sử hoạt động quản trị</h2></div><div className="admin-toolbar-actions"><span className="count">{filtered.length}/{items.length} bản ghi</span>{canDelete && <><button className="danger compact" disabled={!selected.length} onClick={removeSelected}>Xóa đã chọn{selected.length ? ` (${selected.length})` : ''}</button><button className="danger-outline compact" disabled={!items.length} onClick={removeAll}>Xóa toàn bộ</button></>}</div></div>
    <div className="admin-filters"><div className="search-box"><span aria-hidden="true">⌕</span><input value={search} onChange={e => setSearch(e.target.value)} placeholder="Tìm người dùng, hành động, nội dung…" aria-label="Tìm nhật ký" /></div><select value={userFilter} onChange={e => setUserFilter(e.target.value)} aria-label="Lọc người dùng"><option value="ALL">Tất cả người dùng</option>{usernames.map(x => <option key={x} value={x}>{x}</option>)}</select><select value={actionFilter} onChange={e => setActionFilter(e.target.value)} aria-label="Lọc hành động"><option value="ALL">Tất cả hành động</option>{actions.map(x => <option key={x} value={x}>{x}</option>)}</select><label className="date-filter">Từ <input type="date" value={dateFrom} onChange={e => setDateFrom(e.target.value)} /></label><label className="date-filter">Đến <input type="date" value={dateTo} onChange={e => setDateTo(e.target.value)} /></label><button className="filter-reset" onClick={resetFilters}>Reset</button></div>
    {canDelete && filtered.length > 0 && <div className="audit-select-all"><label><input type="checkbox" checked={allVisibleSelected} onChange={toggleAll} /> Chọn tất cả bản ghi đang hiển thị</label><span>Chỉ Admin gốc được xóa nhật ký</span></div>}
    {error && <div className="alert page-alert">{error}</div>}{loading ? <div className="empty">Đang tải…</div> : filtered.length === 0 ? <div className="empty">{items.length ? 'Không tìm thấy bản ghi phù hợp.' : 'Chưa có nhật ký.'}</div> : <div className="audit-list">{filtered.map(a => <article key={a.id} className="audit-row"><div className="audit-row-check">{canDelete && <input type="checkbox" checked={selected.includes(a.id)} onChange={() => toggle(a.id)} aria-label={`Chọn nhật ký ${a.id}`} />}<div><div className="req-id">{a.username || 'Hệ thống'} · {a.action}</div><strong>{a.requirementId ? `REQ-${String(a.requirementId).padStart(4, '0')} · ${a.requirementTitle || ''}` : a.action}</strong><p>{a.details || '—'}</p></div></div><div className="audit-row-end"><time>{a.createdAt ? new Date(a.createdAt).toLocaleString('vi-VN') : '—'}</time>{canDelete && <button className="danger-link" onClick={() => removeOne(a.id)}>Xóa</button>}</div></article>)}</div>}
  </section>
}

function AdminOverviewTab() {
  const [data, setData] = useState(null), [loading, setLoading] = useState(true), [error, setError] = useState('')
  const [search, setSearch] = useState(''), [providerFilter, setProviderFilter] = useState('ALL'), [userFilter, setUserFilter] = useState('ALL'), [dateFrom, setDateFrom] = useState(''), [dateTo, setDateTo] = useState('')
  useEffect(() => { fetchAdminDashboard().then(setData).catch(e => setError(e.response?.data?.message || `Không thể tải trang quản trị (${e.response?.status || 'không xác định'}).`)).finally(() => setLoading(false)) }, [])
  async function handleExportUsage() { try { await exportAdminUsageReport() } catch (e) { setError(e.response?.data?.message || 'Không thể xuất báo cáo.') } }
  function resetFilters() { setSearch(''); setProviderFilter('ALL'); setUserFilter('ALL'); setDateFrom(''); setDateTo('') }
  if (loading) return <div className="card empty">Đang tải dữ liệu quản trị…</div>
  if (error) return <div className="card admin-error-card"><div><strong>Không thể tải dữ liệu quản trị</strong><p>{error}</p></div><button className="secondary compact" onClick={() => window.location.reload()}>Thử lại</button></div>
  if (!data) return null
  const usage = data.recentUsage || []
  const providers = [...new Set(usage.map(x => x.provider).filter(Boolean))].sort()
  const usernames = [...new Set(usage.map(x => x.username).filter(Boolean))].sort()
  const rows = usage.filter(x => {
    const q = search.trim().toLowerCase()
    const textOk = !q || [x.username, x.requirementTitle, x.requirementId, x.provider, x.prompt].filter(v => v !== null && v !== undefined).some(v => String(v).toLowerCase().includes(q))
    const providerOk = providerFilter === 'ALL' || x.provider === providerFilter
    const userOk = userFilter === 'ALL' || x.username === userFilter
    const created = x.createdAt ? new Date(x.createdAt) : null
    const fromOk = !dateFrom || (created && created >= new Date(dateFrom + 'T00:00:00'))
    const toOk = !dateTo || (created && created <= new Date(dateTo + 'T23:59:59.999'))
    return textOk && providerOk && userOk && fromOk && toOk
  })
  return <><div className="stats"><div className="stat"><span>Người dùng</span><b>{data.totalUsers}</b></div><div className="stat"><span>Yêu cầu</span><b>{data.totalRequirements}</b></div><div className="stat"><span>Đã phân tích</span><b>{data.analyzedRequirements}</b></div><div className="stat"><span>Tổng token AI</span><b>{Number(data.totalTokens || 0).toLocaleString()}</b></div></div><section className="card admin-log"><div className="section-head admin-section-head-responsive"><div><span className="eyebrow">NHẬT KÝ PHÂN TÍCH AI</span><h2>Các yêu cầu phân tích gần đây</h2></div><div className="admin-toolbar-actions"><span className="count">{rows.length}/{usage.length} lượt phân tích</span><button className="secondary compact" onClick={handleExportUsage}>Xuất báo cáo Excel</button></div></div>
    <div className="admin-filters"><div className="search-box"><span aria-hidden="true">⌕</span><input value={search} onChange={e => setSearch(e.target.value)} placeholder="Tìm REQ, người dùng, tiêu đề…" aria-label="Tìm nhật ký phân tích AI" /></div><select value={userFilter} onChange={e => setUserFilter(e.target.value)} aria-label="Lọc người dùng"><option value="ALL">Tất cả người dùng</option>{usernames.map(x => <option key={x} value={x}>{x}</option>)}</select><select value={providerFilter} onChange={e => setProviderFilter(e.target.value)} aria-label="Lọc nhà cung cấp"><option value="ALL">Tất cả nhà cung cấp</option>{providers.map(x => <option key={x} value={x}>{x}</option>)}</select><label className="date-filter">Từ <input type="date" value={dateFrom} onChange={e => setDateFrom(e.target.value)} /></label><label className="date-filter">Đến <input type="date" value={dateTo} onChange={e => setDateTo(e.target.value)} /></label><button className="filter-reset" onClick={resetFilters}>Reset</button></div>
    {rows.length === 0 ? <div className="empty">{usage.length ? 'Không tìm thấy lượt phân tích phù hợp.' : 'Chưa có hoạt động phân tích AI.'}</div> : <div className="usage-table">{rows.map(x => <article key={x.id}><div><div className="req-id">REQ-{String(x.requirementId).padStart(4, '0')} · {x.username}</div><h3>{x.requirementTitle}</h3><p>{x.prompt}</p><time>{x.createdAt ? new Date(x.createdAt).toLocaleString('vi-VN') : '—'}</time></div><div className="token-box"><span>{x.provider || 'AI'}</span><b>{Number(x.totalTokens || 0).toLocaleString()}</b><small>token</small></div></article>)}</div>}
  </section></>
}

function AdminApp({ user, onBack, onLogout, onUserUpdate }) {
  const [tab, setTab] = useState('overview')
  const [openUserId, setOpenUserId] = useState(null)
  const [usersRefreshKey, setUsersRefreshKey] = useState(0)
  const [creatingUser, setCreatingUser] = useState(false)
  const [nameInput, setNameInput] = useState(user.fullName || '')
  const [nameSaved, setNameSaved] = useState(false)
  const [nameError, setNameError] = useState('')
  const [toast, setToast] = useState('')

  async function saveMyName() {
    const value = nameInput.trim()
    if (!value) {
      setNameError('Tên hiển thị không được để trống.')
      return
    }
    try {
      const updated = await updateAdminUser(user.id, { fullName: value })
      onUserUpdate(updated)
      setNameInput(updated.fullName || value)
      setNameSaved(true)
      setNameError('')
      window.setTimeout(() => setNameSaved(false), 1800)
    } catch (e) {
      setNameError(e.response?.data?.message || 'Không thể lưu tên hiển thị.')
    }
  }

  return (
    <>
      <Header user={user} onLogout={onLogout} />

      <main className="page admin-page">
        <div className="page-title">
          <div>
            <span className="eyebrow">QUẢN TRỊ HỆ THỐNG</span>
            <h1>Dashboard</h1>
            <p>Theo dõi người dùng, yêu cầu, hoạt động phân tích và mức sử dụng AI.</p>
          </div>
          <button className="secondary compact" onClick={onBack}>Quay lại Workspace</button>
        </div>

        <div className="card admin-self-profile">
          <div className="admin-self-profile-head">
            <div>
              <span className="eyebrow">TÀI KHOẢN QUẢN TRỊ</span>
              <strong>Thông tin hiển thị của quản trị viên</strong>
              <p className="muted">Chỉ quản trị viên mới có quyền thay đổi tên hiển thị tài khoản.</p>
            </div>
            <span className="role-pill role-admin">Quản trị viên</span>
          </div>
          <div className="text-edit admin-self-name-edit">
            <label className="inline-label">Tên hiển thị</label>
            <input value={nameInput} onChange={e => setNameInput(e.target.value)} placeholder="Nhập họ và tên" />
            <button className="secondary compact" onClick={saveMyName} disabled={nameSaved}>
              {nameSaved ? 'Đã lưu' : 'Lưu tên'}
            </button>
          </div>
          {nameError && <div className="alert" style={{ marginTop: 10 }}>{nameError}</div>}
        </div>

        <div className="admin-tabs">
          <button className={tab === 'overview' ? 'admin-tab admin-tab-active' : 'admin-tab'} onClick={() => setTab('overview')}>
            Tổng quan
          </button>
          <button className={tab === 'users' ? 'admin-tab admin-tab-active' : 'admin-tab'} onClick={() => setTab('users')}>
            Người dùng
          </button>
          <button className={tab === 'requirements' ? 'admin-tab admin-tab-active' : 'admin-tab'} onClick={() => setTab('requirements')}>
            Yêu cầu
          </button>
          <button className={tab === 'audit' ? 'admin-tab admin-tab-active' : 'admin-tab'} onClick={() => setTab('audit')}>
            Nhật ký
          </button>
        </div>

        {tab === 'overview' && <AdminOverviewTab />}
        {tab === 'users' && (
          <AdminUsersTab
            key={usersRefreshKey}
            onOpenUser={setOpenUserId}
            onCreateUser={() => setCreatingUser(true)}
          />
        )}
        {tab === 'requirements' && <AdminRequirementsTab />}
        {tab === 'audit' && <AdminAuditTab user={user} />}
      </main>

      <UserDetailDrawer
        userId={openUserId}
        currentUserId={user.id}
        onClose={() => setOpenUserId(null)}
        onChanged={updated => {
          setUsersRefreshKey(k => k + 1)
          if (updated?.fullName) onUserUpdate(updated)
        }}
        onDeleted={() => {
          setOpenUserId(null)
          setUsersRefreshKey(k => k + 1)
          setToast('Đã xóa tài khoản thành công')
          setTimeout(() => setToast(''), 2200)
        }}
      />

      <CreateUserDrawer
        open={creatingUser}
        onClose={() => setCreatingUser(false)}
        onCreated={() => {
          setCreatingUser(false)
          setUsersRefreshKey(k => k + 1)
          setToast('Đã tạo User thành công')
          setTimeout(() => setToast(''), 2200)
        }}
      />

      {toast && <div className="success-toast" role="status">✓ {toast}</div>}
    </>
  )
}

/* ============================== ROOT ============================== */

export default function App() {
  const [user, setUser] = useState(getStoredUser())
  const uiLanguage = useUiLanguage()
  const [admin, setAdmin] = useState(false)
  const [authView, setAuthView] = useState('login')

  useEffect(() => {
    document.documentElement.lang = uiLanguage === 'EN' ? 'en' : 'vi'

    let scheduled = false
    const apply = () => {
      if (scheduled) return
      scheduled = true
      window.setTimeout(() => {
        scheduled = false
        applyUiTranslations(uiLanguage)
      }, 0)
    }

    apply()

    // Observe React-rendered text changes so dynamic labels such as
    // "Signing in…" are translated, but do NOT observe attributes.
    // Observing placeholder attributes while changing placeholders inside
    // applyUiTranslations would create a DOM mutation loop.
    const observer = new MutationObserver(() => apply())
    observer.observe(document.body, {
      childList: true,
      subtree: true,
      characterData: true
    })

    return () => observer.disconnect()
  }, [uiLanguage, user, authView, admin])

  useEffect(() => {
    function handleExpired() {
      setUser(null)
      setAdmin(false)
      setAuthView('login')
    }
    window.addEventListener('auth-expired', handleExpired)
    return () => window.removeEventListener('auth-expired', handleExpired)
  }, [])

  function handleLogout() {
    logout().catch(() => {})
    localStorage.removeItem('srs_token')
    localStorage.removeItem('srs_user')
    setUser(null)
    setAdmin(false)
    setAuthView('login')
  }

  // Dung de dong bo thong tin user hien tai sau khi Admin cap nhat ten hoac User luu Prompt Preference
  function handleUserUpdate(patch) {
    setUser(prev => {
      const merged = { ...prev, ...patch }
      localStorage.setItem('srs_user', JSON.stringify(merged))
      return merged
    })
  }

  if (!user) {
    return authView === 'register'
      ? <Register onRegistered={setUser} onSwitchToLogin={() => setAuthView('login')} />
      : <Login onLogin={setUser} onSwitchToRegister={() => setAuthView('register')} />
  }

  if (user.role === 'ADMIN' && admin) {
    return <AdminApp user={user} onBack={() => setAdmin(false)} onLogout={handleLogout} onUserUpdate={handleUserUpdate} />
  }

  return <UserApp user={user} onLogout={handleLogout} onAdmin={() => setAdmin(true)} onUserUpdate={handleUserUpdate} />
}
