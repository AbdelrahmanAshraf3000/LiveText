-- Phase 3: CRDT collaboration state persistence

CREATE TABLE collab_state (
    document_id UUID PRIMARY KEY REFERENCES documents(id) ON DELETE CASCADE,
    snapshot BYTEA,
    state_vector BYTEA,
    snapshot_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE collab_updates (
    id BIGSERIAL PRIMARY KEY,
    document_id UUID NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    update BYTEA NOT NULL,
    hash VARCHAR(64) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE(document_id, hash)
);
CREATE INDEX idx_collab_updates_doc ON collab_updates(document_id, id);