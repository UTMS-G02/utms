// Validators used by Auth forms

export function validateInstitutionalEmail(value) {
  if (!value) return { valid: false, message: 'E-posta boş bırakılamaz.' };
  const lower = value.toLowerCase();
  if (!lower.endsWith('.edu.tr')) {
    return { valid: false, message: 'E-posta .edu.tr ile bitmelidir.' };
  }
  // basic email pattern
  const emailRe = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  if (!emailRe.test(value)) return { valid: false, message: 'Geçersiz e-posta formatı.' };
  return { valid: true };
}

export function validateStrongPassword(value) {
  if (!value) return { valid: false, message: 'Şifre boş bırakılamaz.' };
  if (value.length < 8) return { valid: false, message: 'Şifre en az 8 karakter olmalıdır.' };
  const upper = /[A-Z]/.test(value);
  const lower = /[a-z]/.test(value);
  const digit = /[0-9]/.test(value);
  const special = /[!@#$%^&*(),.?"':{}|<>\-_=+\\/\[\];]/.test(value);
  if (!upper) return { valid: false, message: 'Şifre en az bir büyük harf içermelidir.' };
  if (!lower) return { valid: false, message: 'Şifre en az bir küçük harf içermelidir.' };
  if (!digit) return { valid: false, message: 'Şifre en az bir rakam içermelidir.' };
  if (!special) return { valid: false, message: 'Şifre en az bir özel karakter içermelidir.' };
  return { valid: true };
}
