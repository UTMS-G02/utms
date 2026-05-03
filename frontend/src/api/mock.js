export const mockApplications = [
  {
    applicationId: 50,
    targetDepartment: 'Elektrik-Elektronik Mühendisliği',
    targetFaculty: 'Mühendislik Fakültesi',
    status: 'OIDB_REVIEW',
    createdAt: '2026-04-15T10:30:00Z',
    submittedAt: '2026-04-16T09:15:00Z',
  },
  {
    applicationId: 38,
    targetDepartment: 'Bilgisayar Mühendisliği',
    targetFaculty: 'Mühendislik Fakültesi',
    status: 'REJECTED',
    createdAt: '2025-10-12T08:00:00Z',
    submittedAt: '2025-10-15T14:30:00Z',
  },
  {
    applicationId: 22,
    targetDepartment: 'Makine Mühendisliği',
    targetFaculty: 'Mühendislik Fakültesi',
    status: 'ACCEPTED',
    createdAt: '2024-09-05T11:20:00Z',
    submittedAt: '2024-09-10T16:00:00Z',
  },
]

export const mockApplicationDetail = {
  applicationId: 42,
  studentId: 1,
  studentName: 'Ali Veli',
  targetDepartment: 'Elektrik-Elektronik Mühendisliği',
  targetFaculty: 'Mühendislik Fakültesi',
  status: 'OIDB_REVIEW',
  createdAt: '2026-04-15T10:30:00Z',
  submittedAt: '2026-04-16T09:15:00Z',
  documents: [
    {
      documentId: 101,
      docType: 'TRANSCRIPT',
      fileName: 'transkript.pdf',
      uploadedAt: '2026-04-15T11:00:00Z',
    },
    {
      documentId: 102,
      docType: 'ID_CARD',
      fileName: 'kimlik.pdf',
      uploadedAt: '2026-04-15T11:05:00Z',
    },
  ],
  statusHistory: [
    { status: 'DRAFT',       changedAt: '2026-04-15T10:30:00Z' },
    { status: 'SUBMITTED',   changedAt: '2026-04-16T09:15:00Z' },
    { status: 'OIDB_REVIEW', changedAt: '2026-04-16T14:00:00Z' },
  ],
}

export const mockResult = {
  applicationId: 42,
  status: 'ACCEPTED',
  compositeScore: 405.32,
  primaryListRank: 3,
  publishedAt: '2026-05-20T15:00:00Z',
}

export const delay = (ms = 500) =>
  new Promise((resolve) => setTimeout(resolve, ms))
