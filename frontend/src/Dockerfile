# Hafif bir Node.js sürümü kullanıyoruz
FROM node:20-alpine

# Container içinde çalışacağımız klasör
WORKDIR /app

# Sadece kütüphane listesini kopyala ve indir
COPY package*.json ./
RUN npm install

# Geri kalan tüm kodları kopyala
COPY . .

# Hem standart React (3000) hem de Vite (5173) portlarını dışa açalım
EXPOSE 3000 5173

# Uygulamayı başlat
CMD ["npm", "run", "dev"]