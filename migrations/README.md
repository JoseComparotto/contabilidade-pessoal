# Migrations (Flyway)

Coloque aqui os arquivos SQL de migração do Flyway usando o padrão de nome:

- V<versao>__<descricao>.sql
- Ex.: V1__criar_tabelas_iniciais.sql

Boas práticas:
- Use `create table if not exists` quando fizer sentido; evite `drop` em produção.
- Não edite uma migration que já rodou; crie uma nova com o próximo número.
- Use tipos nativos do Postgres (text, numeric, timestamptz, etc.).
- Adicione índices e constraints explicitamente.

Execução local:
- Ao iniciar a aplicação, o Spring Boot executa as migrations automaticamente (Flyway) no Postgres do docker-compose.

Exemplo (comentado):
```sql
-- V1__exemplo.sql
-- create table if not exists demo (
--   id bigserial primary key,
--   nome text not null,
--   criado_em timestamptz not null default now()
--);
```
