-- V9: Remover a regra que impede conta redutora de aceitar movimento oposto
-- Objetivo: atualizar a função do gatilho para não barrar contas redutoras,
-- mantendo as regras de hierarquia e de descendentes.

-- Atualiza a função de enforcement removendo a "Regra 1" (redutora não pode aceitar movimento oposto)
CREATE OR REPLACE FUNCTION public.fn_tg_ct_enforce_tb_contas_aceita_movimento_oposto()
RETURNS trigger
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = pg_catalog, public
AS $BODY$
DECLARE
    v_parent_accepts boolean;
    v_descendant_accepts boolean;
BEGIN
    IF TG_OP = 'INSERT' OR TG_OP = 'UPDATE' THEN
        -- Regra de hierarquia: só pode aceitar se a superior também aceitar
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

        -- Complemento da regra de hierarquia: não permitir definir FALSE
        -- se existir QUALQUER descendente que aceita (em qualquer nível)
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
                   )
              INTO v_descendant_accepts;

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

-- Recria o constraint trigger (idempotente)
DROP TRIGGER IF EXISTS ct_enforce_tb_contas_aceita_movimento_oposto ON public.tb_contas;
CREATE CONSTRAINT TRIGGER ct_enforce_tb_contas_aceita_movimento_oposto
    AFTER INSERT OR UPDATE OF credora, id_superior, aceita_movimento_oposto ON public.tb_contas
    DEFERRABLE INITIALLY DEFERRED
    FOR EACH ROW
    EXECUTE FUNCTION public.fn_tg_ct_enforce_tb_contas_aceita_movimento_oposto();
