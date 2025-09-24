-- Migration: V12__create_mt_contas_arvore_and_trigger.sql
-- Cria a materialized view de árvore de contas, view de mapeamento
-- e trigger para atualizar a materialized view quando `public.tb_contas` mudar.

SET search_path = public;

-- Limpa objetos antigos para tornar a migration idempotente
DROP TRIGGER IF EXISTS trg_refresh_mt_contas_arvore ON public.tb_contas;
DROP FUNCTION IF EXISTS public.refresh_mt_contas_arvore();
DROP VIEW IF EXISTS public.vw_movimento_diario;
DROP VIEW IF EXISTS public.vw_contas_mapeamento;
DROP MATERIALIZED VIEW IF EXISTS public.mt_contas_arvore;

-- Cria a materialized view que representa a árvore de contas
CREATE MATERIALIZED VIEW public.mt_contas_arvore AS
WITH RECURSIVE q1 AS (
  SELECT
    c.id,
    ARRAY[c.sequencia]::int[] AS path,
    ARRAY[]::int[] AS ids_superiores
  FROM public.tb_contas c
  WHERE id_superior IS NULL

  UNION ALL

  SELECT
    c.id,
    q1.path || c.sequencia,
    q1.ids_superiores || c.id_superior
  FROM public.tb_contas c
  INNER JOIN q1 ON c.id_superior = q1.id
)
SELECT
  id,
  ids_superiores,
  array_to_string(path, '.') AS codigo,
  row_number() OVER (ORDER BY path) AS posicao
FROM q1
WITH DATA;

-- Índice simples para melhorar buscas por id (não único, apenas desempenho)
CREATE INDEX IF NOT EXISTS idx_mt_contas_arvore_id ON public.mt_contas_arvore (id);

-- View de mapeamento entre contas sintéticas e analíticas
CREATE VIEW public.vw_contas_mapeamento AS
SELECT
  unnest(a.ids_superiores) AS id_conta_sintetica,
  c.id AS id_conta_analitica
FROM public.tb_contas c
INNER JOIN public.mt_contas_arvore a ON c.id = a.id
WHERE c.analitica;

-- View de movimento diário (direto + indireto via mapeamento de contas)
CREATE VIEW public.vw_movimento_diario AS
WITH q AS (
 SELECT tb_lancamentos.id_conta_credito AS id_conta,
  tb_lancamentos.data_competencia,
  tb_lancamentos.status,
  tb_lancamentos.valor AS credito,
  0::numeric(14,2) AS debito
 FROM tb_lancamentos
 UNION ALL
 SELECT tb_lancamentos.id_conta_debito AS id_conta,
  tb_lancamentos.data_competencia,
  tb_lancamentos.status,
  0::numeric(14,2) AS credito,
  tb_lancamentos.valor AS debito
 FROM tb_lancamentos
),
movimento_diario_direto AS (
 SELECT id_conta,
   status,
   data_competencia,
   sum(credito) AS credito,
   sum(debito) AS debito
  FROM q
 GROUP BY id_conta, status, data_competencia
), 
movimento_diario_indireto AS (
 SELECT 
   cm.id_conta_sintetica AS id_conta,
   m.status,
   m.data_competencia,
   SUM(m.credito) AS credito,
   SUM(m.debito) AS debito
 FROM movimento_diario_direto m
 JOIN public.vw_contas_mapeamento cm
   ON m.id_conta = cm.id_conta_analitica
 GROUP BY
   cm.id_conta_sintetica,
   m.status,
   m.data_competencia
)
SELECT 
 id_conta, status, data_competencia, credito, debito
FROM movimento_diario_direto
UNION ALL
SELECT 
 id_conta, status, data_competencia, credito, debito
FROM movimento_diario_indireto
ORDER BY 1, 2, 3;

-- Função que atualiza a materialized view
CREATE OR REPLACE FUNCTION public.refresh_mt_contas_arvore()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
  -- Atualiza a materialized view inteira. Usamos REFRESH sem CONCURRENTLY
  -- para manter compatibilidade com execuções dentro de transações.
  REFRESH MATERIALIZED VIEW public.mt_contas_arvore;
  RETURN NULL;
END;
$$;

-- Trigger statement-level: executa uma única atualização por operação DML
CREATE TRIGGER trg_refresh_mt_contas_arvore
AFTER INSERT OR UPDATE OR DELETE OR TRUNCATE ON public.tb_contas
FOR EACH STATEMENT
EXECUTE FUNCTION public.refresh_mt_contas_arvore();