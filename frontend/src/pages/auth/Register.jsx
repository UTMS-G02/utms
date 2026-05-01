import React from 'react';
import { Card, Form, Input, Button, Checkbox, message } from 'antd';
import { useNavigate } from 'react-router-dom';
import { validateInstitutionalEmail, validateStrongPassword } from '../../utils/validators';

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
    <div style={{ display: 'flex', height: '100vh', alignItems: 'center', justifyContent: 'center' }}>
      <Card title="Kayıt Ol" style={{ width: 420 }}>
        <Form form={form} layout="vertical" onFinish={onFinish}>
          <Form.Item
            label="E-posta"
            name="email"
            rules={[
              { required: true, message: 'E-posta gerekli.' },
              {
                validator: (_, value) => {
                  const res = validateInstitutionalEmail(value);
                  return res.valid ? Promise.resolve() : Promise.reject(res.message);
                },
              },
            ]}
          >
            <Input />
          </Form.Item>

          <Form.Item
            label="Şifre"
            name="password"
            rules={[
              { required: true, message: 'Şifre gerekli.' },
              {
                validator: (_, value) => {
                  const res = validateStrongPassword(value);
                  return res.valid ? Promise.resolve() : Promise.reject(res.message);
                },
              },
            ]}
            hasFeedback
          >
            <Input.Password />
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
            <Input.Password />
          </Form.Item>

          <Form.Item
            name="kvkk"
            valuePropName="checked"
            rules={[{ validator: (_, value) => (value ? Promise.resolve() : Promise.reject('KVKK onayı gerekli.')) }]}
          >
            <Checkbox>KVKK'yi okudum ve onaylıyorum</Checkbox>
          </Form.Item>

          <Form.Item>
            <Button type="primary" htmlType="submit" block>
              Kayıt Ol
            </Button>
          </Form.Item>
        </Form>
      </Card>
    </div>
  );
};

export default Register;
