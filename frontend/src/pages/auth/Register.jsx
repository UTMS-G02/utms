import React from 'react';
import { Card, Form, Input, Button, Checkbox, message } from 'antd';
import { useNavigate } from 'react-router-dom';
import { institutionalEmailRule, passwordRule } from '../../utils/validators';

const Register = () => {
  const [form] = Form.useForm();
  const navigate = useNavigate();

  const onFinish = async (values) => {
    // mock delay
    await new Promise((r) => setTimeout(r, 1000));
    message.success('Kayıt başarılı. Giriş sayfasına yönlendiriliyorsunuz.');
    navigate('/login');
  };

  return (
    <div style={{ display: 'flex', minHeight: '100vh', alignItems: 'center', justifyContent: 'center', background: '#fafafa' }}>
      <Card title="Kayıt Ol" style={{ width: 420, boxShadow: '0 4px 16px rgba(0,0,0,0.08)' }}>
        <Form form={form} layout="vertical" onFinish={onFinish}>
          
          <Form.Item
            label="Kurumsal E-posta"
            name="email"
            rules={[institutionalEmailRule()]}
          >
            <Input placeholder="isim@iyte.edu.tr" />
          </Form.Item>

          <Form.Item
            label="Şifre"
            name="password"
            rules={[passwordRule()]}
            hasFeedback
          >
            <Input.Password placeholder="En az 8 karakter" />
          </Form.Item>

          <Form.Item
            label="Şifre Tekrar"
            name="confirm"
            dependencies={["password"]}
            hasFeedback
            rules={[
              { required: true, message: 'Lütfen şifreyi tekrar girin.' },
              ({ getFieldValue }) => ({
                validator(_, value) {
                  if (!value || getFieldValue('password') === value) {
                    return Promise.resolve();
                  }
                  return Promise.reject(new Error('Şifreler eşleşmiyor.'));
                },
              }),
            ]}
          >
            <Input.Password placeholder="Şifrenizi doğrulayın" />
          </Form.Item>

          <Form.Item
            name="kvkk"
            valuePropName="checked"
            rules={[{ validator: (_, value) => (value ? Promise.resolve() : Promise.reject('KVKK onayı gerekli.')) }]}
          >
            <Checkbox>KVKK'yi okudum ve onaylıyorum</Checkbox>
          </Form.Item>

          <Form.Item style={{ marginBottom: 0 }}>
            <Button type="primary" htmlType="submit" block style={{ height: 40, background: '#8B1A2B' }}>
              Kayıt Ol
            </Button>
          </Form.Item>
        </Form>
      </Card>
    </div>
  );
};

export default Register;