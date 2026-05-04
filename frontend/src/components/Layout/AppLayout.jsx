import { useState } from 'react'
import { Outlet, useNavigate, useLocation, Link } from 'react-router-dom'
import { Layout, Menu, Avatar, Dropdown, Typography, Space } from 'antd'
import {
  HomeOutlined,
  FileTextOutlined,
  FolderOpenOutlined,
  UserOutlined,
  LogoutOutlined,
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  BellOutlined,
} from '@ant-design/icons'
import { useAuth, ROLES } from '../../contexts/AuthContext'

const { Sider, Content, Header } = Layout
const { Text } = Typography

// ─── Per-role menu definitions ────────────────────────────────────────────────
const MENU_CONFIG = {
  [ROLES.STUDENT]: [
    { key: '/student/dashboard', icon: <HomeOutlined />, label: 'Ana Sayfa' },
    { key: '/student/applications/new', icon: <FileTextOutlined />, label: 'Başvuru Yap' },    { key: '/student/applications', icon: <FolderOpenOutlined />, label: 'Başvurularım' },
    { key: '/student/profile', icon: <UserOutlined />, label: 'Profil' },
  ],
  [ROLES.OIDB]: [
    { key: '/oidb/dashboard', icon: <HomeOutlined />, label: 'Ana Sayfa' },
    { key: '/oidb/pending', icon: <FileTextOutlined />, label: 'Bekleyen Başvurular' },
  ],
  [ROLES.YDYO]: [
    { key: '/ydyo/dashboard', icon: <HomeOutlined />, label: 'Ana Sayfa' },
    { key: '/ydyo/evaluations', icon: <FileTextOutlined />, label: 'Değerlendirmeler' },
    { key: '/ydyo/bulk-upload', icon: <FolderOpenOutlined />, label: 'Toplu Yükleme' },
  ],
  [ROLES.YGK]: [
    { key: '/ygk/dashboard', icon: <HomeOutlined />, label: 'Ana Sayfa' },
    { key: '/ygk/intibak', icon: <FileTextOutlined />, label: 'İntibak Tablosu' },
  ],
  [ROLES.DEAN_OFFICE]: [
    { key: '/dean/dashboard', icon: <HomeOutlined />, label: 'Ana Sayfa' },
  ],
  [ROLES.FACULTY_BOARD]: [
    { key: '/faculty-board/dashboard', icon: <HomeOutlined />, label: 'Ana Sayfa' },
  ],
}

function IyteLogo({ collapsed }) {
  return (
    <Link to="/" style={{ display: 'flex', alignItems: 'center', gap: 10, textDecoration: 'none' }}>
      <div
        style={{
          width: 36,
          height: 36,
          borderRadius: '50%',
          background: 'var(--color-primary)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          flexShrink: 0,
        }}
      >
        <span style={{ color: '#fff', fontWeight: 700, fontSize: 14 }}>İ</span>
      </div>
      {!collapsed && (
        <div style={{ lineHeight: 1.2 }}>
          <div style={{ fontWeight: 700, fontSize: 13, color: 'var(--color-primary)' }}>
            İYTE
          </div>
          <div style={{ fontSize: 10, color: 'var(--color-text-secondary)', letterSpacing: '0.02em' }}>
            UTMS
          </div>
        </div>
      )}
    </Link>
  )
}

export default function AppLayout() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [collapsed, setCollapsed] = useState(false)

  const menuItems = MENU_CONFIG[user?.role] ?? []

  const userDropdownItems = [
    { key: 'profile', icon: <UserOutlined />, label: 'Profil' },
    { type: 'divider' },
    { key: 'logout', icon: <LogoutOutlined />, label: 'Çıkış Yap', danger: true },
  ]

  const handleUserMenuClick = ({ key }) => {
    if (key === 'logout') logout()
    if (key === 'profile') navigate(`/${user?.role?.toLowerCase()}/profile`)
  }

  return (
    <Layout style={{ minHeight: '100vh' }}>
      {/* ── Sidebar ── */}
      <Sider
        collapsed={collapsed}
        onCollapse={setCollapsed}
        width={220}
        collapsedWidth={64}
        style={{
          background: 'var(--color-surface)',
          borderRight: '1px solid var(--color-border)',
          position: 'fixed',
          left: 0,
          top: 0,
          bottom: 0,
          zIndex: 100,
          overflow: 'auto',
        }}
        trigger={null}
      >
        {/* Logo */}
        <div
          style={{
            height: 64,
            display: 'flex',
            alignItems: 'center',
            padding: '0 16px',
            borderBottom: '1px solid var(--color-border)',
          }}
        >
          <IyteLogo collapsed={collapsed} />
        </div>

        {/* Navigation */}
        <Menu
          mode="inline"
          selectedKeys={[location.pathname]}
          items={menuItems}
          onClick={({ key }) => navigate(key)}
          style={{ border: 'none', marginTop: 8 }}
        />

        {/* Logout at bottom */}
        <div
          style={{
            position: 'absolute',
            bottom: 0,
            width: '100%',
            padding: '16px',
            borderTop: '1px solid var(--color-border)',
          }}
        >
          <div
            onClick={logout}
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: 10,
              cursor: 'pointer',
              color: 'var(--color-text-secondary)',
              fontSize: 13,
              padding: '6px 8px',
              borderRadius: 6,
              transition: 'all 0.2s',
            }}
            onMouseEnter={(e) => {
              e.currentTarget.style.background = '#fff1f0'
              e.currentTarget.style.color = '#ff4d4f'
            }}
            onMouseLeave={(e) => {
              e.currentTarget.style.background = 'transparent'
              e.currentTarget.style.color = 'var(--color-text-secondary)'
            }}
          >
            <LogoutOutlined />
            {!collapsed && <span>Çıkış Yap</span>}
          </div>
        </div>
      </Sider>

      {/* ── Main content area ── */}
      <Layout style={{ marginLeft: collapsed ? 64 : 220, transition: 'margin 0.2s' }}>
        {/* Header */}
        <Header
          style={{
            background: 'var(--color-surface)',
            borderBottom: '1px solid var(--color-border)',
            padding: '0 24px',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            position: 'sticky',
            top: 0,
            zIndex: 99,
            height: 64,
          }}
        >
          {/* Collapse toggle */}
          <div
            onClick={() => setCollapsed(!collapsed)}
            style={{ cursor: 'pointer', color: 'var(--color-text-secondary)', fontSize: 18 }}
          >
            {collapsed ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />}
          </div>

          {/* Right side: bell + user */}
          <Space size={16}>
            <BellOutlined
              style={{ fontSize: 18, color: 'var(--color-text-secondary)', cursor: 'pointer' }}
            />
            <Dropdown
              menu={{ items: userDropdownItems, onClick: handleUserMenuClick }}
              placement="bottomRight"
              arrow
            >
              <Space style={{ cursor: 'pointer' }}>
                <Avatar
                  size={32}
                  style={{ background: 'var(--color-primary)', fontWeight: 600 }}
                >
                  {user?.firstName?.[0]?.toUpperCase() ?? 'U'}
                </Avatar>
                <div style={{ lineHeight: 1.3 }}>
                  <Text style={{ fontSize: 13, fontWeight: 600, display: 'block' }}>
                    {`${user?.firstName ?? 'Kullanıcı'} ${user?.lastName ?? ''}`}
                  </Text>
                  <Text
                    type="secondary"
                    style={{ fontSize: 11 }}
                  >
                    {user?.role}
                  </Text>
                </div>
              </Space>
            </Dropdown>
          </Space>
        </Header>

        {/* Page content */}
        <Content style={{ padding: '24px', minHeight: 'calc(100vh - 64px)' }}>
          <Outlet />
        </Content>
      </Layout>
    </Layout>
  )
}