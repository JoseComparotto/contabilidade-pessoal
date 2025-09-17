-- V10: Status, datas prevista/efetiva em tb_lancamentos
-- Requisitos:
-- - status: enum (PREVISTO, EFETIVO, CANCELADO), NOT NULL, DEFAULT PREVISTO
-- - backfill: todos os lançamentos existentes devem ter status = EFETIVO

-- 1) Cria o tipo ENUM (idempotente)
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'lancamento_status') THEN
        CREATE TYPE public.lancamento_status AS ENUM ('PREVISTO', 'EFETIVO', 'CANCELADO');
    END IF;
END $$;

-- 2) Adiciona a coluna status com default PREVISTO (idempotente)
ALTER TABLE public.tb_lancamentos
    ADD COLUMN IF NOT EXISTS status public.lancamento_status NOT NULL DEFAULT 'PREVISTO';

-- 3) Backfill de status = EFETIVO para registros existentes
--    Nota: não altera datas; apenas garante o valor de status conforme solicitado
UPDATE public.tb_lancamentos
   SET status = 'EFETIVO'
 WHERE status IS DISTINCT FROM 'EFETIVO';
