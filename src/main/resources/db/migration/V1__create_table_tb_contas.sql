CREATE SEQUENCE IF NOT EXISTS public.tb_contas_id_seq
    INCREMENT 1
    START 1;

CREATE TABLE IF NOT EXISTS public.tb_contas
(
    id integer NOT NULL DEFAULT nextval('public.tb_contas_id_seq'::regclass),
    id_superior integer,
    sequencia integer NOT NULL,
    descricao text NOT NULL,
    analitica boolean NOT NULL DEFAULT false,
    credora boolean NOT NULL,
    ativa boolean NOT NULL DEFAULT true,
    created_by_system boolean NOT NULL DEFAULT false,
    
    created_at timestamptz NOT NULL DEFAULT NOW(),
    updated_at timestamptz NOT NULL DEFAULT NOW(),

    CONSTRAINT tb_contas_pkey PRIMARY KEY (id),

    CONSTRAINT tb_contas_id_superior_sequencia_unique UNIQUE NULLS NOT DISTINCT (id_superior, sequencia),

    CONSTRAINT tb_contas_id_superior_fkey FOREIGN KEY (id_superior)
        REFERENCES public.tb_contas (id) MATCH SIMPLE
        ON UPDATE RESTRICT
        ON DELETE RESTRICT
        DEFERRABLE INITIALLY DEFERRED,
        
    CONSTRAINT tb_contas_descricao_check CHECK (descricao <> ''::text)
);

ALTER SEQUENCE public.tb_contas_id_seq
    OWNED BY public.tb_contas.id;

-- Função reaproveitável: atualiza updated_at antes de persisitir alterações
CREATE OR REPLACE FUNCTION public.fn_tg_update_audit()
    RETURNS trigger
    LANGUAGE plpgsql
    COST 100
    VOLATILE NOT LEAKPROOF
AS $BODY$
BEGIN
    NEW.updated_at := now();
    RETURN NEW;
END
$BODY$;

-- Garantir que não haja trigger com o mesmo nome antes de criar
DROP TRIGGER IF EXISTS tg_before_update_audit ON public.tb_contas;

CREATE TRIGGER tg_before_update_audit
    BEFORE UPDATE
    ON public.tb_contas
    FOR EACH ROW
    EXECUTE FUNCTION public.fn_tg_update_audit();

-- =============================
-- Segurança: usuários/roles e RLS
-- (Executar ANTES de DML para evitar erro 55006 em ALTER TABLE com eventos pendentes de trigger)
-- =============================

-- 1) Criar usuário da aplicação, se não existir (executa como superuser via Flyway)
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = '${app_user}') THEN
        EXECUTE format('CREATE ROLE %I LOGIN PASSWORD %L', '${app_user}', '${app_user_password}');
    END IF;
END $$;

-- 2) Criar role intermediária, se não existir
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'core_contas_manage') THEN
        CREATE ROLE core_contas_manage;
    END IF;
END $$;

-- 3) Conceder privilégios mínimos
GRANT USAGE ON SCHEMA public TO ${app_user};
GRANT SELECT ON public.tb_contas TO core_contas_manage;
GRANT core_contas_manage TO ${app_user};

-- 4) RLS: Habilitar e criar políticas (sem DML pendente nesta transação)
ALTER TABLE public.tb_contas ENABLE ROW LEVEL SECURITY;

-- Leitura: todos os registros
DROP POLICY IF EXISTS tb_contas_select_all ON public.tb_contas;
CREATE POLICY tb_contas_select_all ON public.tb_contas FOR SELECT TO core_contas_manage USING (true);

-- Escrita: apenas registros não criados pelo sistema (uma política por ação)
DROP POLICY IF EXISTS tb_contas_insert_non_system ON public.tb_contas;
CREATE POLICY tb_contas_insert_non_system ON public.tb_contas
    FOR INSERT TO core_contas_manage
    WITH CHECK (created_by_system = false);

DROP POLICY IF EXISTS tb_contas_update_non_system ON public.tb_contas;
CREATE POLICY tb_contas_update_non_system ON public.tb_contas
    FOR UPDATE TO core_contas_manage
    USING (created_by_system = false)
    WITH CHECK (created_by_system = false);

DROP POLICY IF EXISTS tb_contas_delete_non_system ON public.tb_contas;
CREATE POLICY tb_contas_delete_non_system ON public.tb_contas
    FOR DELETE TO core_contas_manage
    USING (created_by_system = false);

-- Opcional: revogar permissões públicas amplas
REVOKE ALL ON public.tb_contas FROM PUBLIC;

-- Inserts padrão (id_superior = NULL). Use ON CONFLICT para serem idempotentes.
INSERT INTO public.tb_contas (id_superior, sequencia, descricao, credora, analitica, ativa, created_by_system)
VALUES
    (NULL, 1, 'ATIVO', false, false, true, true),
    (NULL, 2, 'PASSIVO', true, false, true, true),
    (NULL, 3, 'PATRIMÔNIO LÍQUIDO', true, false, true, true),
    (NULL, 4, 'RECEITAS', true, false, true, true),
        (NULL, 5, 'DESPESAS', false, false, true, true)
ON CONFLICT ON CONSTRAINT tb_contas_id_superior_sequencia_unique DO NOTHING;
-- Contas sintéticas inferiores (não analíticas), herdam 'credora' do pai
-- Inferiores de 1 - ATIVO
INSERT INTO public.tb_contas (id_superior, sequencia, descricao, credora, analitica, ativa, created_by_system)
SELECT p.id, 1, 'Ativo Circulante', p.credora, false, true, true
FROM public.tb_contas p
WHERE p.id_superior IS NULL AND p.sequencia = 1
ON CONFLICT ON CONSTRAINT tb_contas_id_superior_sequencia_unique DO NOTHING;

INSERT INTO public.tb_contas (id_superior, sequencia, descricao, credora, analitica, ativa, created_by_system)
SELECT p.id, 2, 'Ativo Não Circulante', p.credora, false, true, true
FROM public.tb_contas p
WHERE p.id_superior IS NULL AND p.sequencia = 1
ON CONFLICT ON CONSTRAINT tb_contas_id_superior_sequencia_unique DO NOTHING;

-- Inferiores de 2 - PASSIVO
INSERT INTO public.tb_contas (id_superior, sequencia, descricao, credora, analitica, ativa, created_by_system)
SELECT p.id, 1, 'Passivo Circulante', p.credora, false, true, true
FROM public.tb_contas p
WHERE p.id_superior IS NULL AND p.sequencia = 2
ON CONFLICT ON CONSTRAINT tb_contas_id_superior_sequencia_unique DO NOTHING;

INSERT INTO public.tb_contas (id_superior, sequencia, descricao, credora, analitica, ativa, created_by_system)
SELECT p.id, 2, 'Passivo Não Circulante', p.credora, false, true, true
FROM public.tb_contas p
WHERE p.id_superior IS NULL AND p.sequencia = 2
ON CONFLICT ON CONSTRAINT tb_contas_id_superior_sequencia_unique DO NOTHING;

-- Inferiores de 4 - RECEITAS
INSERT INTO public.tb_contas (id_superior, sequencia, descricao, credora, analitica, ativa, created_by_system)
SELECT p.id, 1, 'Receitas Tributáveis', p.credora, false, true, true
FROM public.tb_contas p
WHERE p.id_superior IS NULL AND p.sequencia = 4
ON CONFLICT ON CONSTRAINT tb_contas_id_superior_sequencia_unique DO NOTHING;

INSERT INTO public.tb_contas (id_superior, sequencia, descricao, credora, analitica, ativa, created_by_system)
SELECT p.id, 2, 'Receitas Não Tributáveis', p.credora, false, true, true
FROM public.tb_contas p
WHERE p.id_superior IS NULL AND p.sequencia = 4
ON CONFLICT ON CONSTRAINT tb_contas_id_superior_sequencia_unique DO NOTHING;