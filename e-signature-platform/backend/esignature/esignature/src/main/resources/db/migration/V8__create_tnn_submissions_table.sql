-- Create tnn_submissions table
CREATE TABLE tnn_submissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    invoice_id UUID NOT NULL,
    submission_payload TEXT NOT NULL,
    response_payload TEXT,
    status VARCHAR(30) NOT NULL,
    error_message TEXT,
    tnn_submission_id VARCHAR(100),
    retry_count INTEGER DEFAULT 0,
    submitted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP,
    CONSTRAINT fk_tnn_submissions_invoice FOREIGN KEY (invoice_id) REFERENCES invoices(id) ON DELETE CASCADE
);

-- Create indexes
CREATE INDEX idx_tnn_submissions_invoice_id ON tnn_submissions(invoice_id);
CREATE INDEX idx_tnn_submissions_status ON tnn_submissions(status);
CREATE INDEX idx_tnn_submissions_tnn_id ON tnn_submissions(tnn_submission_id);