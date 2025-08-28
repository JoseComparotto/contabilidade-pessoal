-- V5: Constraint trigger dedicada para regra de redutora vs. inferiores
-- Regra: Uma conta redutora não pode ter inferiores não-redutoras.
-- (redutora = natureza diferente da natureza da raiz)

CREATE OR REPLACE FUNCTION public.fn_tg_ct_validate_tb_contas_redutora_inferiores()
RETURNS trigger
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = pg_catalog, public
AS $BODY$
DECLARE
    v_root_credora boolean;
    v_is_redutora  boolean;
    v_parent_credora boolean;
    v_parent_is_redutora boolean;
    v_has_non_redutora_child boolean;
BEGIN
    IF TG_OP = 'INSERT' OR TG_OP = 'UPDATE' THEN
        -- Descobre a natureza (credora) da raiz da conta NEW
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

        -- Validação lado filho: se superior for redutora, filho não pode ser não-redutora
        IF NEW.id_superior IS NOT NULL THEN
            SELECT c.credora INTO v_parent_credora FROM public.tb_contas c WHERE c.id = NEW.id_superior;
            v_parent_is_redutora := (v_parent_credora IS DISTINCT FROM v_root_credora);
            IF v_parent_is_redutora AND NOT v_is_redutora THEN
                RAISE EXCEPTION 'Conta superior redutora não pode ter inferior não-redutora'
                    USING ERRCODE = '45000';
            END IF;
        END IF;

        -- Validação lado pai: conta redutora não pode ter filhos imediatos não-redutoras
        IF v_is_redutora THEN
            SELECT EXISTS (
                SELECT 1
                  FROM public.tb_contas f
                 WHERE f.id_superior = NEW.id
                   AND (f.credora IS NOT DISTINCT FROM v_root_credora)
            ) INTO v_has_non_redutora_child;

            IF v_has_non_redutora_child THEN
                RAISE EXCEPTION 'Conta redutora não pode ter inferiores não-redutoras'
                    USING ERRCODE = '45000';
            END IF;
        END IF;
    END IF;

    RETURN NULL; -- AFTER/CONSTRAINT triggers ignore return value
END
$BODY$;

ALTER FUNCTION public.fn_tg_ct_validate_tb_contas_redutora_inferiores() OWNER TO CURRENT_USER;

DROP TRIGGER IF EXISTS ct_validate_tb_contas_redutora_inferiores ON public.tb_contas;
CREATE CONSTRAINT TRIGGER ct_validate_tb_contas_redutora_inferiores
    AFTER INSERT OR UPDATE OF credora, id_superior ON public.tb_contas
    DEFERRABLE INITIALLY DEFERRED
    FOR EACH ROW
    EXECUTE FUNCTION public.fn_tg_ct_validate_tb_contas_redutora_inferiores();
