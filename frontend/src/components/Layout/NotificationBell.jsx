import { useState, useEffect, useCallback } from 'react'
import { Badge, Popover, Empty, Spin, Typography, Tooltip, Button, Modal, App } from 'antd'
import { BellOutlined, DeleteOutlined, CheckOutlined } from '@ant-design/icons'
import { notificationsApi } from '../../api/notifications'

const { Text, Paragraph } = Typography

// Kısa, yerel zaman etiketi (örn. "5 dk önce", "2 saat önce", tarih).
function formatTime(value) {
  if (!value) return ''
  const date = new Date(value)
  const diffMin = Math.round((Date.now() - date.getTime()) / 60000)
  if (diffMin < 1) return 'az önce'
  if (diffMin < 60) return `${diffMin} dk önce`
  const diffHour = Math.round(diffMin / 60)
  if (diffHour < 24) return `${diffHour} saat önce`
  const diffDay = Math.round(diffHour / 24)
  if (diffDay < 7) return `${diffDay} gün önce`
  return date.toLocaleDateString('tr-TR')
}

// Tek bildirim satırı: başlık + zaman + aksiyon butonları (Okundu / Sil).
// Satıra tıklamak detay modalını açar — okundu YAPMAZ (okundu yalnız buton ile).
function NotificationItem({ item, onOpen, onMarkRead, onDelete }) {
  const unread = !item.isRead
  return (
    <div
      onClick={() => onOpen(item)}
      style={{
        padding: '10px 12px',
        cursor: 'pointer',
        borderLeft: unread ? '3px solid var(--color-primary)' : '3px solid transparent',
        background: unread ? '#f9f0f1' : 'transparent',
        transition: 'background 0.2s',
      }}
      onMouseEnter={(e) => (e.currentTarget.style.background = unread ? '#f5e6e8' : '#fafafa')}
      onMouseLeave={(e) => (e.currentTarget.style.background = unread ? '#f9f0f1' : 'transparent')}
    >
      <Text
        style={{ fontSize: 13, fontWeight: unread ? 600 : 500, display: 'block' }}
        type={unread ? undefined : 'secondary'}
        ellipsis
      >
        {item.title}
      </Text>
      <Text type="secondary" style={{ fontSize: 12, display: 'block', marginTop: 2 }} ellipsis>
        {item.message}
      </Text>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginTop: 6 }}>
        <Text type="secondary" style={{ fontSize: 11 }}>{formatTime(item.createdAt)}</Text>
        <div onClick={(e) => e.stopPropagation()} style={{ display: 'flex', gap: 4 }}>
          {unread && (
            <Tooltip title="Okundu olarak işaretle">
              <Button
                type="text"
                size="small"
                icon={<CheckOutlined />}
                onClick={() => onMarkRead(item)}
              >
                Okundu
              </Button>
            </Tooltip>
          )}
          <Tooltip title="Sil">
            <Button
              type="text"
              size="small"
              danger
              icon={<DeleteOutlined />}
              onClick={() => onDelete(item)}
            />
          </Tooltip>
        </div>
      </div>
    </div>
  )
}

function GroupLabel({ children }) {
  return (
    <div style={{ padding: '6px 12px', background: '#fafafa', borderBottom: '1px solid var(--color-border)' }}>
      <Text type="secondary" style={{ fontSize: 11, fontWeight: 600, letterSpacing: '0.04em' }}>
        {children}
      </Text>
    </div>
  )
}

export default function NotificationBell() {
  const { message } = App.useApp()
  const [open, setOpen] = useState(false)
  const [loading, setLoading] = useState(false)
  const [items, setItems] = useState([])
  const [detailItem, setDetailItem] = useState(null) // açık detay modalı

  const load = useCallback(async () => {
    setLoading(true)
    try {
      const data = await notificationsApi.getMyNotifications()
      setItems(data)
    } catch {
      message.error('Bildirimler yüklenirken bir hata oluştu.')
    } finally {
      setLoading(false)
    }
  }, [message])

  // İlk yüklemede sayacın görünmesi için bir kez çek (sessiz — spinner yok).
  useEffect(() => {
    notificationsApi.getMyNotifications()
      .then((data) => setItems(data))
      .catch(() => {})
  }, [])

  const handleOpenChange = (next) => {
    setOpen(next)
    if (next) load()
  }

  // Okundu: yalnız buton ile. Local state güncellenir, sayfa yenilenmez.
  const markAsRead = async (item) => {
    if (item.isRead) return
    try {
      await notificationsApi.markAsRead(item.id)
      setItems((prev) => prev.map((n) => (n.id === item.id ? { ...n, isRead: true } : n)))
      setDetailItem((prev) => (prev && prev.id === item.id ? { ...prev, isRead: true } : prev))
    } catch {
      message.error('Bildirim güncellenemedi.')
    }
  }

  const handleDelete = async (item) => {
    try {
      await notificationsApi.deleteNotification(item.id)
      setItems((prev) => prev.filter((n) => n.id !== item.id))
      setDetailItem((prev) => (prev && prev.id === item.id ? null : prev))
      message.success('Bildirim silindi.')
    } catch {
      message.error('Bildirim silinirken bir hata oluştu.')
    }
  }

  const unread = items.filter((n) => !n.isRead)
  const read = items.filter((n) => n.isRead)

  const content = (
    <div style={{ width: 340, maxHeight: 440, overflowY: 'auto', margin: '-12px -16px' }}>
      <div style={{ padding: '10px 12px', borderBottom: '1px solid var(--color-border)' }}>
        <Text strong style={{ fontSize: 14 }}>Bildirimler</Text>
      </div>

      {loading ? (
        <div style={{ padding: 32, textAlign: 'center' }}>
          <Spin />
        </div>
      ) : items.length === 0 ? (
        <div style={{ padding: '24px 12px' }}>
          <Empty
            image={Empty.PRESENTED_IMAGE_SIMPLE}
            description="Yeni bildiriminiz bulunmamaktadır."
          />
        </div>
      ) : (
        <>
          {unread.length > 0 && (
            <>
              <GroupLabel>OKUNMAMIŞ</GroupLabel>
              {unread.map((item) => (
                <NotificationItem
                  key={item.id}
                  item={item}
                  onOpen={setDetailItem}
                  onMarkRead={markAsRead}
                  onDelete={handleDelete}
                />
              ))}
            </>
          )}
          {read.length > 0 && (
            <>
              <GroupLabel>OKUNDU</GroupLabel>
              {read.map((item) => (
                <NotificationItem
                  key={item.id}
                  item={item}
                  onOpen={setDetailItem}
                  onMarkRead={markAsRead}
                  onDelete={handleDelete}
                />
              ))}
            </>
          )}
        </>
      )}
    </div>
  )

  return (
    <>
      <Popover
        content={content}
        trigger="click"
        placement="bottomRight"
        open={open}
        onOpenChange={handleOpenChange}
        arrow
      >
        <Badge count={unread.length} size="small" offset={[-2, 2]}>
          <BellOutlined
            style={{ fontSize: 18, color: 'var(--color-text-secondary)', cursor: 'pointer' }}
          />
        </Badge>
      </Popover>

      {/* Detay modalı — tam başlık + mesaj + zaman. Okundu yalnız butonla. */}
      <Modal
        open={!!detailItem}
        onCancel={() => setDetailItem(null)}
        title={detailItem?.title}
        footer={[
          detailItem && !detailItem.isRead && (
            <Button key="read" icon={<CheckOutlined />} onClick={() => markAsRead(detailItem)}>
              Okundu İşaretle
            </Button>
          ),
          <Button key="close" type="primary" onClick={() => setDetailItem(null)}>
            Kapat
          </Button>,
        ]}
      >
        {detailItem && (
          <>
            <Paragraph style={{ marginBottom: 8 }}>{detailItem.message}</Paragraph>
            <Text type="secondary" style={{ fontSize: 12 }}>{formatTime(detailItem.createdAt)}</Text>
          </>
        )}
      </Modal>
    </>
  )
}
