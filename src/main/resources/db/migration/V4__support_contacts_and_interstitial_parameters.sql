ALTER TABLE system_parameters
    ALTER COLUMN parameter_value TYPE TEXT;

INSERT INTO system_parameters (parameter_key, parameter_value, description, value_type)
VALUES
    ('ADMIN_WHATSAPP_NUMBER', '573163353115', 'Numero de WhatsApp del administrador familiar en formato internacional sin simbolos.', 'TEXT'),
    ('FORGOT_PASSWORD_WHATSAPP_MESSAGE', 'Hola, ayudame a recuperar mi contrasena de FamiCup.', 'Mensaje prellenado de WhatsApp para recuperacion de contrasena.', 'TEXT'),
    ('REQUEST_ACCESS_WHATSAPP_MESSAGE', 'Hola, quiero solicitar acceso a FamiCup para participar en la polla familiar.', 'Mensaje prellenado de WhatsApp para solicitud de acceso.', 'TEXT'),
    ('FORGOT_PASSWORD_MODAL_TEXT', 'Contacta al administrador familiar de FamiCup para restablecer tu contrasena escribiendole al WhatsApp 3163353115.', 'Texto visible del modal de recuperacion de contrasena.', 'TEXT'),
    ('REQUEST_ACCESS_MODAL_TEXT', 'Pidele al organizador de tu familia que te registre agregandote en la tabla de miembros. Escribile al WhatsApp 3163353115.', 'Texto visible del modal de solicitud de acceso.', 'TEXT'),
    ('INTERSTITIAL_BANNER_ENABLED', 'false', 'Activa o desactiva el banner interstitial inicial.', 'BOOLEAN'),
    ('INTERSTITIAL_BANNER_IMAGE_URL', '', 'URL publica de la imagen del banner interstitial.', 'TEXT'),
    ('INTERSTITIAL_BANNER_TARGET_URL', '', 'URL opcional de destino al tocar el banner interstitial.', 'TEXT'),
    ('INTERSTITIAL_BANNER_ALT_TEXT', 'Anuncio familiar FamiCup', 'Texto alternativo del banner interstitial.', 'TEXT'),
    ('INTERSTITIAL_BANNER_DISMISS_HOURS', '12', 'Horas durante las que se recuerda el cierre del banner en este navegador.', 'INTEGER')
ON CONFLICT (parameter_key) DO NOTHING;
