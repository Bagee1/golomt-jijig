insert into security_controls (control_key, title, description, weight, required, automated, standard_ref)
values
    ('HTTPS_ENABLED', 'HTTPS ашигладаг', 'Системийн endpoint HTTPS ашигладаг эсэх.', 15, true, true, 'OWASP ASVS'),
    ('AUTHENTICATION_ENABLED', 'Нэвтрэлттэй', 'API эсвэл UI хэрэглэгчийг баталгаажуулдаг эсэх.', 15, true, false, 'OWASP ASVS'),
    ('ROLE_BASED_ACCESS', 'Эрхийн түвшинтэй', 'Role/permission ашиглаж хандалт хязгаарладаг эсэх.', 15, true, false, 'OWASP ASVS'),
    ('AUDIT_LOG_ENABLED', 'Audit log хийдэг', 'Чухал үйлдлийг audit log-д бүртгэдэг эсэх.', 10, true, false, 'OWASP ASVS'),
    ('SECRETS_NOT_IN_CODE', 'Нууц мэдээлэл code-д хадгалаагүй', 'Password, token, secret key source code-д ил хадгалаагүй эсэх.', 15, true, false, 'OWASP ASVS'),
    ('SWAGGER_PROTECTED', 'Swagger хамгаалагдсан', 'Swagger/OpenAPI production дээр public биш эсэх.', 10, false, false, 'OWASP API Security'),
    ('CORS_RESTRICTED', 'CORS хязгаарлагдсан', 'CORS тохиргоо allowlist зарчмаар хийгдсэн эсэх.', 10, false, false, 'OWASP API Security'),
    ('INPUT_VALIDATION', 'Input validation хийдэг', 'Request input backend дээр validation хийгддэг эсэх.', 10, true, false, 'OWASP ASVS');

