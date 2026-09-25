-- Phase 4: version history for documents

CREATE TABLE document_versions (
    id          BIGSERIAL PRIMARY KEY,
    document_id UUID NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    version_no  INT NOT NULL,
    yjs_state   BYTEA NOT NULL,
    byte_size   INT NOT NULL DEFAULT 0,
    created_by  BIGINT NOT NULL REFERENCES users(id),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE(document_id, version_no)
);
CREATE INDEX idx_versions_doc ON document_versions(document_id, version_no);