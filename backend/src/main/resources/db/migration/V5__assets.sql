-- Phase 5: assets table for uploaded images

CREATE TABLE assets (
    id           UUID PRIMARY KEY,
    document_id UUID NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    uploader_id  BIGINT NOT NULL REFERENCES users(id),
    file_name    TEXT NOT NULL,
    mime_type    TEXT NOT NULL,
    byte_size    BIGINT NOT NULL DEFAULT 0,
    object_key   TEXT NOT NULL,
    url          TEXT NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_assets_doc ON assets(document_id);