CREATE INDEX IF NOT EXISTS inspections_status_updated_id_idx
    ON inspections (status, updated_at DESC, id DESC);
