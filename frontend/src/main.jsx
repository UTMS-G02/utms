import React from 'react'
import ReactDOM from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import { ConfigProvider, App as AntdApp } from 'antd'
import App from './App'
import AuthProvider from './contexts/AuthContext'
import './index.css'

const theme = {
  token: {
    colorPrimary: '#8B1A2B',
    colorPrimaryHover: '#a61f34',
    colorPrimaryActive: '#6e1522',
    borderRadius: 6,
    fontFamily: "'DM Sans', 'Segoe UI', sans-serif",
  },
  components: {
    Button: {
      primaryColor: '#ffffff',
    },
    Menu: {
      itemSelectedBg: '#f5e6e8',
      itemSelectedColor: '#8B1A2B',
      itemHoverColor: '#8B1A2B',
    },
  },
}

ReactDOM.createRoot(document.getElementById('root')).render(
  <BrowserRouter>
    <ConfigProvider theme={theme}>
      {/* AntdApp eklendi, artık Login'deki message.success çökmeyecek! */}
      <AntdApp> 
        <AuthProvider>
          <App />
        </AuthProvider>
      </AntdApp>
    </ConfigProvider>
  </BrowserRouter>
)