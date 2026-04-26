-- Create invoice_templates table
CREATE TABLE invoice_templates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    template_data JSONB,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_templates_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Create indexes
CREATE INDEX idx_templates_user_id ON invoice_templates(user_id);
CREATE INDEX idx_templates_name ON invoice_templates(name);
CREATE INDEX idx_templates_is_default ON invoice_templates(is_default);

-- Insert default template
INSERT INTO invoice_templates (
    id,
    user_id,
    name,
    description,
    template_data,
    is_default,
    created_at,
    updated_at
) 
SELECT 
    gen_random_uuid(),
    id,
    'Standard Invoice Template',
    'Default template for standard invoices',
    '{"currency": "USD", "defaultTaxRate": 19.00, "defaultTerms": "Payment due within 30 days", "defaultNotes": "Thank you for your business!"}'::jsonb,
    TRUE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM users 
WHERE email = 'admin@esignature.com';