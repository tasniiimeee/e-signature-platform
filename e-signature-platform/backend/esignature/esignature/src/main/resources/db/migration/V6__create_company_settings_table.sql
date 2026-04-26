-- Create company_settings table
CREATE TABLE company_settings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_name VARCHAR(255) NOT NULL,
    tax_id VARCHAR(50) NOT NULL,
    address TEXT,
    phone VARCHAR(50),
    email VARCHAR(255),
    logo_url VARCHAR(500),
    smtp_host VARCHAR(255),
    smtp_port INTEGER,
    smtp_username VARCHAR(255),
    smtp_password_encrypted TEXT,
    tnn_api_key_encrypted TEXT,
    tnn_endpoint_url VARCHAR(500),
    tnn_company_id VARCHAR(100),
    invoice_prefix VARCHAR(10) DEFAULT 'INV',
    invoice_number_digits INTEGER DEFAULT 5,
    default_currency VARCHAR(3) DEFAULT 'USD',
    default_vat_rate DECIMAL(5,2) DEFAULT 19.00,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Insert default company settings
INSERT INTO company_settings (
    id,
    company_name,
    tax_id,
    address,
    phone,
    email,
    invoice_prefix,
    invoice_number_digits,
    default_currency,
    default_vat_rate,
    created_at,
    updated_at
) VALUES (
    gen_random_uuid(),
    'E-Signature Platform',
    '1234567890',
    '123 Business Street, City, Country',
    '+1-555-0100',
    'info@esignature.com',
    'INV',
    5,
    'USD',
    19.00,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);