-- Migration: V13__set_security_definer_for_refresh_mt_contas_arvore.sql

SET search_path = public;

-- Função que atualiza a materialized view
CREATE OR REPLACE FUNCTION public.refresh_mt_contas_arvore()
RETURNS TRIGGER
VOLATILE SECURITY DEFINER
LANGUAGE plpgsql
AS $$
BEGIN
  -- Atualiza a materialized view inteira. Usamos REFRESH sem CONCURRENTLY
  -- para manter compatibilidade com execuções dentro de transações.
  REFRESH MATERIALIZED VIEW public.mt_contas_arvore;
  RETURN NULL;
END;
$$;