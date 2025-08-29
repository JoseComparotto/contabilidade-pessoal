-- V6: Lançamentos
-- Objetivo: criar tabela de lançamentos (tb_lancamentos), grants, auditoria e validações de negócio.
-- Observações:
-- - Valor usa numeric(14,2) em vez de money (evita localidade e facilita integrações com JPA/BigDecimal).
-- - Regras de negócio:
--   * valor > 0 (zero não permitido).
--   * contas de débito e crédito devem ser distintas e analíticas.
--   * movimento contrário à natureza da conta só é permitido quando a conta aceitar movimento oposto.
--   * Observação: a validação de conta ATIVA fica no backend para não invalidar lançamentos históricos após inativação.

CREATE SEQUENCE IF NOT EXISTS public.tb_lancamentos_id_seq
    INCREMENT 1
    START 1;

CREATE TABLE IF NOT EXISTS public.tb_lancamentos (

    id bigint NOT NULL DEFAULT nextval('public.tb_lancamentos_id_seq'::regclass),
    descricao text NOT NULL,
    valor numeric(14,2) NOT NULL,
    data_competencia date NOT NULL,

    id_conta_credito integer NOT NULL,
    id_conta_debito integer NOT NULL,

    created_at timestamptz NOT NULL DEFAULT NOW(),
    updated_at timestamptz NOT NULL DEFAULT NOW(),

    CONSTRAINT tb_lancamentos_pkey PRIMARY KEY (id),
    CONSTRAINT tb_lancamentos_descricao_check CHECK (descricao <> ''::text),
    -- valor deve ser positivo (zero e negativos não permitidos)
    CONSTRAINT tb_lancamentos_valor_check CHECK (valor > 0),
    -- contas de débito e crédito precisam ser distintas
    CONSTRAINT tb_lancamentos_contas_distintas CHECK (id_conta_credito <> id_conta_debito),
    -- FKs para contas (deferrable para facilitar transações complexas)
    CONSTRAINT tb_lancamentos_conta_credito_fk FOREIGN KEY (id_conta_credito)
        REFERENCES public.tb_contas (id) MATCH SIMPLE
        ON UPDATE RESTRICT
        ON DELETE RESTRICT
        DEFERRABLE INITIALLY DEFERRED,
    CONSTRAINT tb_lancamentos_conta_debito_fk FOREIGN KEY (id_conta_debito)
        REFERENCES public.tb_contas (id) MATCH SIMPLE
        ON UPDATE RESTRICT
        ON DELETE RESTRICT
        DEFERRABLE INITIALLY DEFERRED
);

ALTER SEQUENCE public.tb_lancamentos_id_seq
    OWNED BY public.tb_lancamentos.id;

-- Índices auxiliares para FKs (melhoram desempenho de joins e validações)
CREATE INDEX IF NOT EXISTS ix_tb_lancamentos_conta_credito ON public.tb_lancamentos (id_conta_credito);
CREATE INDEX IF NOT EXISTS ix_tb_lancamentos_conta_debito  ON public.tb_lancamentos (id_conta_debito);

-- Auditoria: reaproveita função pública fn_tg_update_audit definida nas migrações anteriores
DROP TRIGGER IF EXISTS tg_before_update_audit ON public.tb_lancamentos;

CREATE TRIGGER tg_before_update_audit
    BEFORE UPDATE
    ON public.tb_lancamentos
    FOR EACH ROW
    EXECUTE FUNCTION public.fn_tg_update_audit();

-- Segurança: role de gestão e permissões mínimas
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'core_lancamentos_manage') THEN
        CREATE ROLE core_lancamentos_manage;
    END IF;
END $$;

GRANT core_lancamentos_manage TO ${app-user};

GRANT SELECT, INSERT, UPDATE, DELETE
    ON TABLE public.tb_lancamentos
    TO core_lancamentos_manage;

GRANT SELECT, USAGE
    ON SEQUENCE public.tb_lancamentos_id_seq
    TO core_lancamentos_manage;

-- Trigger (constraint) para proteger colunas imutáveis
CREATE OR REPLACE FUNCTION public.fn_tg_ct_block_tb_lancamentos_immutable_columns()
RETURNS trigger
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = pg_catalog, public
AS $BODY$
BEGIN
    IF TG_OP = 'UPDATE' THEN
        IF NEW.id IS DISTINCT FROM OLD.id THEN
            RAISE EXCEPTION 'Campo id é imutável em public.tb_lancamentos' USING ERRCODE = '45000';
        END IF;

        IF NEW.created_at IS DISTINCT FROM OLD.created_at THEN
            RAISE EXCEPTION 'Campo created_at é imutável em public.tb_lancamentos' USING ERRCODE = '45000';
        END IF;

        -- Allow updated_at only when set by the audit trigger (guarded by GUC flag)
        IF NEW.updated_at IS DISTINCT FROM OLD.updated_at
           AND COALESCE(current_setting('contas.bypass_immutable_updated_at', true), '0') <> '1' THEN
            RAISE EXCEPTION 'Campo updated_at é imutável em public.tb_lancamentos' USING ERRCODE = '45000';
        END IF;
    END IF;

    RETURN NULL; -- ignored for AFTER/CONSTRAINT triggers
END
$BODY$;

ALTER FUNCTION public.fn_tg_ct_block_tb_lancamentos_immutable_columns() OWNER TO CURRENT_USER;

DROP TRIGGER IF EXISTS ct_block_tb_lancamentos_immutable_columns ON public.tb_lancamentos;
CREATE CONSTRAINT TRIGGER ct_block_tb_lancamentos_immutable_columns
    AFTER UPDATE ON public.tb_lancamentos
    DEFERRABLE INITIALLY DEFERRED
    FOR EACH ROW
    EXECUTE FUNCTION public.fn_tg_ct_block_tb_lancamentos_immutable_columns();

-- Validações de negócio específicas dos lançamentos
-- Regras implementadas:
-- 1) id_conta_credito <> id_conta_debito
-- 2) contas usadas devem ser analíticas (a checagem de "ativa" é responsabilidade do backend)
-- 3) movimento contrário à natureza só é permitido se a conta aceitar movimento oposto
CREATE OR REPLACE FUNCTION public.fn_tg_ct_validate_tb_lancamentos_business()
RETURNS trigger
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = pg_catalog, public
AS $BODY$
DECLARE
    -- Crédito
    v_cre_analitica boolean;
    v_cre_credora boolean;
    v_cre_aceita_oposto boolean;
    -- Débito
    v_deb_analitica boolean;
    v_deb_credora boolean;
    v_deb_aceita_oposto boolean;
BEGIN
    IF TG_OP IN ('INSERT','UPDATE') THEN
        -- 1) contas distintas
        IF NEW.id_conta_credito = NEW.id_conta_debito THEN
            RAISE EXCEPTION 'id_conta_credito e id_conta_debito devem ser diferentes' USING ERRCODE = '45000';
        END IF;

        -- 2) carregar atributos das contas
                SELECT c.analitica, c.credora, c.aceita_movimento_oposto
                    INTO v_cre_analitica, v_cre_credora, v_cre_aceita_oposto
          FROM public.tb_contas c WHERE c.id = NEW.id_conta_credito;

                SELECT c.analitica, c.credora, c.aceita_movimento_oposto
                    INTO v_deb_analitica, v_deb_credora, v_deb_aceita_oposto
          FROM public.tb_contas c WHERE c.id = NEW.id_conta_debito;

        -- Defensive checks (FK deve garantir existência)
        IF v_cre_analitica IS NULL OR v_deb_analitica IS NULL THEN
            RAISE EXCEPTION 'Contas inexistentes para o lançamento' USING ERRCODE = '23503';
        END IF;

    -- 2) analíticas (status ativo verificado no backend)
        IF NOT v_cre_analitica OR NOT v_deb_analitica THEN
            RAISE EXCEPTION 'Somente contas analíticas podem ser usadas em lançamentos' USING ERRCODE = '45000';
        END IF;

        -- 3) movimento oposto
        -- Crédito em conta "não credora" é movimento oposto e requer permissão
        IF v_cre_credora IS DISTINCT FROM TRUE AND v_cre_aceita_oposto IS DISTINCT FROM TRUE THEN
            RAISE EXCEPTION 'Conta de crédito não aceita movimento oposto' USING ERRCODE = '45000';
        END IF;

        -- Débito em conta "credora" é movimento oposto e requer permissão
        IF v_deb_credora IS DISTINCT FROM FALSE AND v_deb_aceita_oposto IS DISTINCT FROM TRUE THEN
            RAISE EXCEPTION 'Conta de débito não aceita movimento oposto' USING ERRCODE = '45000';
        END IF;
    END IF;

    RETURN NULL; -- AFTER/CONSTRAINT trigger ignora o retorno
END
$BODY$;

ALTER FUNCTION public.fn_tg_ct_validate_tb_lancamentos_business() OWNER TO CURRENT_USER;

DROP TRIGGER IF EXISTS ct_validate_tb_lancamentos_business ON public.tb_lancamentos;
CREATE CONSTRAINT TRIGGER ct_validate_tb_lancamentos_business
    AFTER INSERT OR UPDATE ON public.tb_lancamentos
    DEFERRABLE INITIALLY DEFERRED
    FOR EACH ROW
    EXECUTE FUNCTION public.fn_tg_ct_validate_tb_lancamentos_business();

