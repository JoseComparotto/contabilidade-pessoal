-- V4: Add column aceita_movimento_oposto, enforce business rules, and populate initial values
-- Regras:
-- 1) Para contas redutoras (natureza diferente da raiz), aceita_movimento_oposto deve ser FALSE.
-- 2) Uma conta só pode aceitar movimento oposto se for raiz ou se a superior também aceitar.
-- 3) Popular: raízes com sequencia 1,2,3 e todas as suas inferiores (não redutoras) devem aceitar movimento oposto.

-- 1) Add column (idempotent)
ALTER TABLE public.tb_contas
    ADD COLUMN IF NOT EXISTS aceita_movimento_oposto boolean NOT NULL DEFAULT false;

-- 2) Function: enforce aceita_movimento_oposto (redutora=false and hierarchy)
CREATE OR REPLACE FUNCTION public.fn_tg_ct_enforce_tb_contas_aceita_movimento_oposto()
RETURNS trigger
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = pg_catalog, public
AS $BODY$
DECLARE
    v_root_credora boolean;
    v_is_redutora  boolean;
    v_parent_accepts boolean;
    v_descendant_accepts boolean;
BEGIN
    IF TG_OP = 'INSERT' OR TG_OP = 'UPDATE' THEN
        -- Determina a natureza da raiz
        WITH RECURSIVE chain AS (
            SELECT id, id_superior, credora
            FROM public.tb_contas
            WHERE id = NEW.id
            UNION ALL
            SELECT c.id, c.id_superior, c.credora
            FROM public.tb_contas c
            JOIN chain ch ON c.id = ch.id_superior
        )
        SELECT ch.credora
          INTO v_root_credora
          FROM chain ch
         WHERE ch.id_superior IS NULL
         LIMIT 1;

        IF v_root_credora IS NULL THEN
            v_root_credora := NEW.credora;
        END IF;

        v_is_redutora := (NEW.credora IS DISTINCT FROM v_root_credora);

        -- Regra 1: redutora não pode aceitar movimento oposto
        IF v_is_redutora AND NEW.aceita_movimento_oposto = true THEN
            RAISE EXCEPTION 'Para contas redutoras, aceita_movimento_oposto deve ser FALSE'
                USING ERRCODE = '45000';
        END IF;

        -- Regra 2: hierarquia
        IF NEW.aceita_movimento_oposto = true AND NEW.id_superior IS NOT NULL THEN
            SELECT c.aceita_movimento_oposto
              INTO v_parent_accepts
              FROM public.tb_contas c
             WHERE c.id = NEW.id_superior;

            IF v_parent_accepts IS DISTINCT FROM TRUE THEN
                RAISE EXCEPTION 'Uma conta só pode aceitar movimento oposto se a superior também aceitar'
                    USING ERRCODE = '45000';
            END IF;
        END IF;

                -- Regra 2 (complemento): não permitir definir a conta para NÃO aceitar
                -- enquanto existir QUALQUER descendente (em qualquer nível) que aceita
                IF NEW.aceita_movimento_oposto = false THEN
                        WITH RECURSIVE descendentes AS (
                                SELECT id
                                    FROM public.tb_contas
                                 WHERE id_superior = NEW.id
                                UNION ALL
                                SELECT c.id
                                    FROM public.tb_contas c
                                    JOIN descendentes d ON c.id_superior = d.id
                        )
                        SELECT EXISTS (
                                SELECT 1
                                    FROM public.tb_contas t
                                    JOIN descendentes d ON t.id = d.id
                                 WHERE t.aceita_movimento_oposto = true
                        ) INTO v_descendant_accepts;

                        IF v_descendant_accepts THEN
                                RAISE EXCEPTION 'Não é possível definir aceita_movimento_oposto=FALSE enquanto existir descendente que aceita'
                                        USING ERRCODE = '45000';
                        END IF;
                END IF;
    END IF;

    RETURN NULL; -- ignored for AFTER/CONSTRAINT triggers
END
$BODY$;

ALTER FUNCTION public.fn_tg_ct_enforce_tb_contas_aceita_movimento_oposto() OWNER TO CURRENT_USER;

-- 3) Constraint trigger (deferrable per-transaction)
DROP TRIGGER IF EXISTS ct_enforce_tb_contas_aceita_movimento_oposto ON public.tb_contas;
CREATE CONSTRAINT TRIGGER ct_enforce_tb_contas_aceita_movimento_oposto
    AFTER INSERT OR UPDATE OF credora, id_superior, aceita_movimento_oposto ON public.tb_contas
    DEFERRABLE INITIALLY DEFERRED
    FOR EACH ROW
    EXECUTE FUNCTION public.fn_tg_ct_enforce_tb_contas_aceita_movimento_oposto();

-- 4) Populate initial values for roots 1,2,3 and their non-redutora descendants
WITH RECURSIVE roots AS (
    SELECT id, id_superior, sequencia, credora, id AS root_id, credora AS root_credora
    FROM public.tb_contas
    WHERE id_superior IS NULL AND sequencia IN (1, 2, 3)
    UNION ALL
    SELECT c.id, c.id_superior, c.sequencia, c.credora, r.root_id, r.root_credora
    FROM public.tb_contas c
    JOIN roots r ON c.id_superior = r.id
)
UPDATE public.tb_contas t
SET aceita_movimento_oposto = TRUE
FROM roots r
WHERE t.id = r.id
  AND (t.credora IS NOT DISTINCT FROM r.root_credora);
