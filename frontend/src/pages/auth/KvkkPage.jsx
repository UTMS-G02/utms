import { Typography } from 'antd'

const { Title, Paragraph, Text } = Typography

// KVKK / Kişisel Verilerin Korunması Aydınlatma Metni.
// Login ve başvuru formundaki "Aydınlatma Metni" linki bu sayfaya (/kvkk) açılır.
export default function KvkkPage() {
  return (
    <div
      style={{
        minHeight: '100vh',
        background: '#f2f2f7',
        padding: '48px 16px',
        fontFamily: "'DM Sans', sans-serif",
      }}
    >
      <div
        style={{
          maxWidth: 820,
          margin: '0 auto',
          background: '#ffffff',
          borderRadius: 10,
          padding: 40,
          boxShadow: '0 4px 16px rgba(0,0,0,0.06)',
          border: '1px solid #f0f0f0',
        }}
      >
        <Title level={2} style={{ color: '#8B1A2B', marginTop: 0 }}>
          Kişisel Verilerin Korunması Aydınlatma Metni
        </Title>
        <Text type="secondary">
          Yatay Geçiş Başvuru Portalı — İzmir Yüksek Teknoloji Enstitüsü
        </Text>

        <Title level={4}>1. Veri Sorumlusu</Title>
        <Paragraph>
          6698 sayılı Kişisel Verilerin Korunması Kanunu (“KVKK”) kapsamında kişisel
          verileriniz, veri sorumlusu sıfatıyla İzmir Yüksek Teknoloji Enstitüsü
          tarafından aşağıda açıklanan amaçlarla işlenebilecektir.
        </Paragraph>

        <Title level={4}>2. İşlenen Kişisel Veriler</Title>
        <Paragraph>
          Yatay geçiş başvurunuz kapsamında ad-soyad, T.C. kimlik numarası, doğum
          tarihi, iletişim bilgileri (e-posta, telefon), öğrencilik ve akademik
          bilgileriniz (mevcut üniversite/bölüm, not ortalaması, YKS puan ve
          sıralaması) ile başvuruya eklenen belgeler işlenmektedir.
        </Paragraph>

        <Title level={4}>3. İşleme Amaçları</Title>
        <Paragraph>
          Kişisel verileriniz; yatay geçiş başvurunuzun alınması, değerlendirilmesi,
          sonuçlandırılması ve ilgili mevzuat kapsamındaki yükümlülüklerin yerine
          getirilmesi amacıyla işlenmektedir.
        </Paragraph>

        <Title level={4}>4. Haklarınız</Title>
        <Paragraph>
          KVKK’nın 11. maddesi uyarınca; verilerinizin işlenip işlenmediğini öğrenme,
          düzeltilmesini veya silinmesini isteme ve kanunda sayılan diğer haklarınızı
          kullanma hakkına sahipsiniz.
        </Paragraph>

        <Paragraph type="secondary" style={{ marginTop: 24, fontSize: 13 }}>
          Bu metin demo/prototip amaçlı hazırlanmıştır.
        </Paragraph>
      </div>
    </div>
  )
}
