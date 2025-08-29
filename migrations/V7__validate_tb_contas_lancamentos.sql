-- V7: Validações complementares relacionadas a tb_contas x tb_lancamentos
-- Regras (aplicadas após UPDATE em tb_contas):
-- 1) Se a conta for sintética (analitica = false), não pode existir nenhum lançamento associado a ela
--    (nem como crédito nem como débito).
-- 2) Se a conta não aceitar movimento oposto (aceita_movimento_oposto = false),
--    então não pode existir lançamento em sentido contrário à sua natureza (credora):
--       - crédito em conta não-credora
--       - débito em conta credora
-- 3) Se a conta estiver inativa (ativa = false), a soma dos débitos dela deve ser igual à soma dos créditos
--    (saldo líquido zero) para permitir a atualização.
-- Observações:
--  - Triggers do tipo CONSTRAINT, DEFERRABLE INITIALLY DEFERRED, para avaliar por transação.
--  - Índices em tb_lancamentos (id_conta_credito/id_conta_debito) já previstos em V6.

CREATE OR REPLACE FUNCTION public.fn_tg_ct_validate_tb_contas_lancamentos_consistency()
RETURNS trigger
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = pg_catalog, public
AS $BODY$
DECLARE
    v_has_lancamentos boolean;
    v_has_opposite_credit boolean;
    v_has_opposite_debit boolean;
    v_sum_debitos numeric(14,2);
    v_sum_creditos numeric(14,2);
BEGIN
    IF TG_OP = 'UPDATE' THEN
        -- Regra 1: conta sintética não pode ter lançamentos associados
        IF NEW.analitica = false THEN
            SELECT EXISTS (
                SELECT 1 FROM public.tb_lancamentos l
                 WHERE l.id_conta_credito = NEW.id
                    OR l.id_conta_debito  = NEW.id
            ) INTO v_has_lancamentos;

            IF v_has_lancamentos THEN
                RAISE EXCEPTION 'Conta (%) é sintética (analitica=false) e não pode ter lançamentos associados', NEW.id
                    USING ERRCODE = '45000';
            END IF;
        END IF;

        -- Regra 2: sem aceitação de movimento oposto, não pode haver lançamentos contrários
        IF NEW.aceita_movimento_oposto = false THEN
            -- Crédito contrário: quando a conta NÃO é credora
            IF NEW.credora IS DISTINCT FROM TRUE THEN
                SELECT EXISTS (
                    SELECT 1 FROM public.tb_lancamentos l
                     WHERE l.id_conta_credito = NEW.id
                ) INTO v_has_opposite_credit;

                IF v_has_opposite_credit THEN
                    RAISE EXCEPTION 'Conta (%) não aceita movimento oposto e possui créditos contrários à natureza', NEW.id
                        USING ERRCODE = '45000';
                END IF;
            END IF;

            -- Débito contrário: quando a conta É credora
            IF NEW.credora IS DISTINCT FROM FALSE THEN
                SELECT EXISTS (
                    SELECT 1 FROM public.tb_lancamentos l
                     WHERE l.id_conta_debito = NEW.id
                ) INTO v_has_opposite_debit;

                IF v_has_opposite_debit THEN
                    RAISE EXCEPTION 'Conta (%) não aceita movimento oposto e possui débitos contrários à natureza', NEW.id
                        USING ERRCODE = '45000';
                END IF;
            END IF;
        END IF;

                -- Regra 3: conta inativa deve ter saldo líquido zero (débitos = créditos)
                IF NEW.ativa = false THEN
                        SELECT COALESCE(SUM(l.valor), 0)
                            INTO v_sum_debitos
                            FROM public.tb_lancamentos l
                         WHERE l.id_conta_debito = NEW.id;

                        SELECT COALESCE(SUM(l.valor), 0)
                            INTO v_sum_creditos
                            FROM public.tb_lancamentos l
                         WHERE l.id_conta_credito = NEW.id;

                        IF v_sum_debitos <> v_sum_creditos THEN
                                RAISE EXCEPTION 'Conta (%) está inativa (ativa=false) e não possui saldo zero: debitos(%) <> creditos(%)', NEW.id, v_sum_debitos, v_sum_creditos
                                        USING ERRCODE = '45000';
                        END IF;
                END IF;
    END IF;

    RETURN NULL; -- AFTER/CONSTRAINT triggers ignoram o retorno
END
$BODY$;

ALTER FUNCTION public.fn_tg_ct_validate_tb_contas_lancamentos_consistency() OWNER TO CURRENT_USER;

DROP TRIGGER IF EXISTS ct_validate_tb_contas_lancamentos_consistency ON public.tb_contas;
CREATE CONSTRAINT TRIGGER ct_validate_tb_contas_lancamentos_consistency
    AFTER UPDATE OF analitica, aceita_movimento_oposto, credora, ativa ON public.tb_contas
    DEFERRABLE INITIALLY DEFERRED
    FOR EACH ROW
    EXECUTE FUNCTION public.fn_tg_ct_validate_tb_contas_lancamentos_consistency();
