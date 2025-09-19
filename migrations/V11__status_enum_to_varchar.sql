-- V11: Convert status from enum type lancamento_status to varchar with CHECK constraint
-- Safer approach: ALTER COLUMN ... TYPE USING to avoid pending trigger events

BEGIN;

-- 1) Remove default so type change is clean
ALTER TABLE public.tb_lancamentos
    ALTER COLUMN status DROP DEFAULT;

-- 2) Change column type in-place by casting enum to text
ALTER TABLE public.tb_lancamentos
    ALTER COLUMN status TYPE VARCHAR(50) USING status::text;

-- 3) Ensure default and NOT NULL as required
ALTER TABLE public.tb_lancamentos
    ALTER COLUMN status SET DEFAULT 'PREVISTO';

UPDATE public.tb_lancamentos
   SET status = 'PREVISTO'
 WHERE status IS NULL;

ALTER TABLE public.tb_lancamentos
    ALTER COLUMN status SET NOT NULL;

-- 4) Add CHECK constraint if it doesn't already exist
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_tb_lancamentos_status_values'
    ) THEN
        ALTER TABLE public.tb_lancamentos
            ADD CONSTRAINT chk_tb_lancamentos_status_values CHECK (status IN ('PREVISTO','EFETIVO','CANCELADO'));
    END IF;
END$$;

COMMIT;
