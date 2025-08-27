-- Permissões necessárias mesmo com a RLS
GRANT INSERT, UPDATE, DELETE
    ON TABLE public.tb_contas
    TO core_contas_manage;

GRANT SELECT, USAGE
    ON SEQUENCE public.tb_contas_id_seq
    TO core_contas_manage;