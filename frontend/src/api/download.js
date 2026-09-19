import apiClient from './client'

// Tai file blob tra ve tu backend xuong may nguoi dung, dung chung cho moi loai xuat file (SRS, Excel, bao cao...)
export async function downloadFile(url, filename) {
  try {
    const response = await apiClient.get(url, { responseType: 'blob' })
    const blob = response.data instanceof Blob
      ? response.data
      : new Blob([response.data])

    const objectUrl = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = objectUrl
    link.download = filename
    link.style.display = 'none'
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.setTimeout(() => window.URL.revokeObjectURL(objectUrl), 1000)
  } catch (e) {
    // Axios tra loi duoi dang Blob khi responseType='blob', nen doc message tu Blob truoc khi nem loi ra ngoai.
    if (e.response?.data instanceof Blob) {
      try {
        const text = await e.response.data.text()
        e.response.data = JSON.parse(text)
      } catch (_) {
        // Giu nguyen loi goc neu server khong tra JSON.
      }
    }
    throw e
  }
}
