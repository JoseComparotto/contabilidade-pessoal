-- V3: Constraint triggers and security adjustments for tb_contas
-- - Add constraint triggers (deferrable per-transaction)
-- - Block changes to immutable columns
-- - Ensure only non-analytic parents (analitica = false) in id_superior
-- - Make audit trigger function SECURITY DEFINER and set safe search_path

-- 1) Update audit trigger function to run with definer privileges and safe search_path
--    Also set a GUC flag to allow the immutable-check trigger to ignore the managed update of updated_at.
CREATE OR REPLACE FUNCTION public.fn_tg_update_audit()
RETURNS trigger
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = pg_catalog, public
AS $BODY$
BEGIN
    -- Flag to indicate updated_at was changed by the audit trigger (checked by constraint trigger)
    PERFORM set_config('contas.bypass_immutable_updated_at', '1', true);

    NEW.updated_at := now();
    RETURN NEW;
END
$BODY$;

-- Ensure the function owner is the migration executor (commonly a superuser)
ALTER FUNCTION public.fn_tg_update_audit() OWNER TO CURRENT_USER;

-- 2) Function: block immutable columns on tb_contas
--    Columns: id, created_by_system, id_superior, sequencia, created_at, updated_at (the latter only when not set by audit trigger)
CREATE OR REPLACE FUNCTION public.fn_tg_ct_block_tb_contas_immutable_columns()
RETURNS trigger
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = pg_catalog, public
AS $BODY$
BEGIN
    IF TG_OP = 'UPDATE' THEN
        IF NEW.id IS DISTINCT FROM OLD.id THEN
            RAISE EXCEPTION 'Campo id é imutável em public.tb_contas' USING ERRCODE = '45000';
        END IF;

        IF NEW.created_by_system IS DISTINCT FROM OLD.created_by_system THEN
            RAISE EXCEPTION 'Campo created_by_system é imutável em public.tb_contas' USING ERRCODE = '45000';
        END IF;

        IF NEW.id_superior IS DISTINCT FROM OLD.id_superior THEN
            RAISE EXCEPTION 'Campo id_superior é imutável em public.tb_contas' USING ERRCODE = '45000';
        END IF;

        IF NEW.sequencia IS DISTINCT FROM OLD.sequencia THEN
            RAISE EXCEPTION 'Campo sequencia é imutável em public.tb_contas' USING ERRCODE = '45000';
        END IF;

        IF NEW.created_at IS DISTINCT FROM OLD.created_at THEN
            RAISE EXCEPTION 'Campo created_at é imutável em public.tb_contas' USING ERRCODE = '45000';
        END IF;

        -- Allow updated_at only when set by the audit trigger (guarded by GUC flag)
        IF NEW.updated_at IS DISTINCT FROM OLD.updated_at
           AND COALESCE(current_setting('contas.bypass_immutable_updated_at', true), '0') <> '1' THEN
            RAISE EXCEPTION 'Campo updated_at é imutável em public.tb_contas' USING ERRCODE = '45000';
        END IF;
    END IF;

    RETURN NULL; -- ignored for AFTER/CONSTRAINT triggers
END
$BODY$;

-- Drop and recreate the constraint trigger (deferrable per-transaction)
DROP TRIGGER IF EXISTS ct_block_tb_contas_immutable_columns ON public.tb_contas;
CREATE CONSTRAINT TRIGGER ct_block_tb_contas_immutable_columns
    AFTER UPDATE ON public.tb_contas
    DEFERRABLE INITIALLY DEFERRED
    FOR EACH ROW
    EXECUTE FUNCTION public.fn_tg_ct_block_tb_contas_immutable_columns();

-- 3) Function: validate that id_superior points to a non-analytic (analitica = false) parent
CREATE OR REPLACE FUNCTION public.fn_tg_ct_validate_tb_contas_id_superior_parent()
RETURNS trigger
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = pg_catalog, public
AS $BODY$
DECLARE
    v_parent_is_analytic boolean;
BEGIN
    IF (TG_OP = 'INSERT' OR TG_OP = 'UPDATE') AND NEW.id_superior IS NOT NULL THEN
        SELECT c.analitica INTO v_parent_is_analytic
        FROM public.tb_contas c
        WHERE c.id = NEW.id_superior;

        -- If FK exists, parent should always exist; still handle unexpected NULL defensively
        IF v_parent_is_analytic IS NULL THEN
            RAISE EXCEPTION 'Conta superior (%) inexistente para public.tb_contas.id_superior', NEW.id_superior USING ERRCODE = '23503';
        END IF;

        IF v_parent_is_analytic THEN
            RAISE EXCEPTION 'A conta superior (%) deve ser sintética (analitica = false) em public.tb_contas', NEW.id_superior USING ERRCODE = '45000';
        END IF;
    END IF;

    RETURN NULL; -- ignored for AFTER/CONSTRAINT triggers
END
$BODY$;

-- Drop and recreate the constraint trigger (deferrable per-transaction)
DROP TRIGGER IF EXISTS ct_validate_tb_contas_id_superior_parent ON public.tb_contas;
CREATE CONSTRAINT TRIGGER ct_validate_tb_contas_id_superior_parent
    AFTER INSERT OR UPDATE OF id_superior ON public.tb_contas
    DEFERRABLE INITIALLY DEFERRED
    FOR EACH ROW
    EXECUTE FUNCTION public.fn_tg_ct_validate_tb_contas_id_superior_parent();

-- 4) Function: prevent setting analitica = true for accounts that already have children
CREATE OR REPLACE FUNCTION public.fn_tg_ct_validate_tb_contas_analitica_change()
RETURNS trigger
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = pg_catalog, public
AS $BODY$
DECLARE
    v_has_children boolean;
BEGIN
    IF TG_OP = 'UPDATE' THEN
        IF NEW.analitica IS DISTINCT FROM OLD.analitica AND NEW.analitica = true THEN
            SELECT EXISTS (
                SELECT 1 FROM public.tb_contas c WHERE c.id_superior = NEW.id
            ) INTO v_has_children;

            IF v_has_children THEN
                RAISE EXCEPTION 'Não é permitido definir analitica=true para conta (%) que possui inferiores', NEW.id USING ERRCODE = '45000';
            END IF;
        END IF;
    END IF;

    RETURN NULL; -- ignored for AFTER/CONSTRAINT triggers
END
$BODY$;

-- Drop and recreate the constraint trigger (deferrable per-transaction)
DROP TRIGGER IF EXISTS ct_validate_tb_contas_analitica_change ON public.tb_contas;
CREATE CONSTRAINT TRIGGER ct_validate_tb_contas_analitica_change
    AFTER UPDATE OF analitica ON public.tb_contas
    DEFERRABLE INITIALLY DEFERRED
    FOR EACH ROW
    EXECUTE FUNCTION public.fn_tg_ct_validate_tb_contas_analitica_change();
