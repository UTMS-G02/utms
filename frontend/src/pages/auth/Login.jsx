import React, { useState } from 'react';
import { Card, Form, Input, Button, message } from 'antd';
import { useNavigate } from 'react-router-dom';

const Login = () => {
  const [step, setStep] = useState(1);
  const [email, setEmail] = useState('');
  const [form] = Form.useForm();
  const navigate = useNavigate();

  const onStep1 = async (values) => {
    await new Promise((r) => setTimeout(r, 1000));
    setEmail(values.email);
    message.success('E-posta doğrulama kodu gönderildi.');
    setStep(2);
  };

  const onStep2 = async (values) => {
    // Here you would normally verify OTP via API
    if (!values.otp || values.otp.length !== 6) {
      message.error('OTP 6 haneli olmalıdır.');
      return;
    }
    await new Promise((r) => setTimeout(r, 500));
    message.success('Giriş başarılı.');
    navigate('/dashboard');
  };

  const resendCode = () => {
    message.info('Kodu yeniden gönderdik.');
  };

  return (
    <div style={{ display: 'flex', height: '100vh', alignItems: 'center', justifyContent: 'center' }}>
      <Card title="Giriş Yap" style={{ width: 420 }}>
        {step === 1 && (
          <Form form={form} layout="vertical" onFinish={onStep1}>
            <Form.Item name="email" label="E-posta" rules={[{ required: true, message: 'E-posta gerekli.' }]}>
              <Input />
            </Form.Item>
            <Form.Item name="password" label="Şifre" rules={[{ required: true, message: 'Şifre gerekli.' }]}>
              <Input.Password />
            </Form.Item>
            <Form.Item>
              <Button type="primary" htmlType="submit" block>
                İleri
              </Button>
            </Form.Item>
          </Form>
        )}

        {step === 2 && (
          <Form layout="vertical" onFinish={onStep2} initialValues={{ otp: '' }}>
            <Form.Item label="Doğrulama Kodu (OTP)" name="otp" rules={[{ required: true, message: 'Kod gerekli.' }, { len: 6, message: 'Kod 6 haneli olmalıdır.' }]}>
              <Input maxLength={6} />
            </Form.Item>
            <Form.Item>
              <Button type="primary" htmlType="submit" style={{ marginRight: 8 }}>
                Gönder
              </Button>
              <Button onClick={resendCode}>Kodu Yeniden Gönder</Button>
            </Form.Item>
          </Form>
        )}
      </Card>
    </div>
  );
};

export default Login;
