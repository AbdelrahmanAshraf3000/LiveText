-- Phase 2: documents and sharing permissions

CREATE TABLE documents (
    id          UUID PRIMARY KEY,
    title       TEXT NOT NULL,
    owner_id    BIGINT NOT NULL REFERENCES users(id),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_documents_owner ON documents(owner_id);

CREATE TABLE document_permissions (
    id            BIGSERIAL PRIMARY KEY,
    document_id   UUID NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    user_id       BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role          TEXT NOT NULL CHECK(role IN ('OWNER','EDITOR','VIEWER')),
    granted_by    BIGINT NOT NULL REFERENCES users(id),
    granted_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE(document_id, user_id)
);
CREATE INDEX idx_permissions_user ON document_permissions(user_id);
CREATE INDEX idx_permissions_doc ON document_permissions(document_id);