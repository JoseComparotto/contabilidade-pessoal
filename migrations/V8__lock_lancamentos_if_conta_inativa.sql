-- V8: Imutabilidade de lançamentos associados a contas inativas
-- Regra: Se um lançamento estiver associado a alguma conta inativa (crédito ou débito),
--        esse lançamento é IMUTÁVEL (não pode sofrer UPDATE nem DELETE) enquanto a associação
--        envolver conta(s) inativa(s).
-- Observações:
--  - Implementado com CONSTRAINT TRIGGER DEFERRABLE para avaliar por transação.
--  - Verificação considera o estado das contas do registro OLD (pré-alteração/exclusão),
--    bloqueando qualquer tentativa de modificação quando houver associação inativa.

CREATE OR REPLACE FUNCTION public.fn_tg_ct_lock_tb_lancamentos_if_conta_inativa()
RETURNS trigger
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = pg_catalog, public
AS $BODY$
DECLARE
    v_cre_ativa boolean;
    v_deb_ativa boolean;
BEGIN
    IF TG_OP IN ('UPDATE', 'DELETE') THEN
        -- Verifica status das contas associadas ao lançamento original (OLD)
        SELECT c.ativa INTO v_cre_ativa FROM public.tb_contas c WHERE c.id = OLD.id_conta_credito;
        SELECT c.ativa INTO v_deb_ativa FROM public.tb_contas c WHERE c.id = OLD.id_conta_debito;

        -- Se qualquer uma for inativa, o lançamento é imutável
        IF COALESCE(v_cre_ativa, false) = false OR COALESCE(v_deb_ativa, false) = false THEN
            RAISE EXCEPTION 'Lançamento (%) é imutável enquanto associado a conta inativa', OLD.id
                USING ERRCODE = '45000';
        END IF;
    END IF;

    RETURN NULL; -- AFTER/CONSTRAINT triggers ignoram o retorno
END
$BODY$;

ALTER FUNCTION public.fn_tg_ct_lock_tb_lancamentos_if_conta_inativa() OWNER TO CURRENT_USER;

DROP TRIGGER IF EXISTS ct_lock_tb_lancamentos_if_conta_inativa ON public.tb_lancamentos;
CREATE CONSTRAINT TRIGGER ct_lock_tb_lancamentos_if_conta_inativa
    AFTER UPDATE OR DELETE ON public.tb_lancamentos
    DEFERRABLE INITIALLY DEFERRED
    FOR EACH ROW
    EXECUTE FUNCTION public.fn_tg_ct_lock_tb_lancamentos_if_conta_inativa();
