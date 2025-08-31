# Contabilidade Pessoal

Aplicação Spring Boot para gestão de plano de contas contábeis, com API REST documentada em OpenAPI/Swagger e interface web com Thymeleaf.

## Visão geral

- Gestão hierárquica de contas (sintéticas e analíticas) com natureza (credora/devedora) e suporte a contas redutoras.
- API REST para CRUD de contas (flat/tree) e leitura de lançamentos.
- UI web simples para listar, criar, editar e excluir contas.
- PostgreSQL com migrações Flyway, RLS (Row Level Security) e gatilhos de integridade (constraint triggers).
- Swagger UI em /api/docs servindo um OpenAPI estático versionado em docs/openapi.json.

## Stack

- Java 17, Spring Boot 3.5.x (Web, Data JPA, Thymeleaf)
- PostgreSQL 16 (Docker Compose)
- Flyway (migrações e provisionamento)
- springdoc-openapi (Swagger UI)
- Maven

## Requisitos

- Docker e Docker Compose
- JDK 17+
- Maven 3.9+

## Como executar

Pré-requisito: copie o arquivo de variáveis de ambiente e ajuste conforme necessário:

```bash
cp .env.example .env
# Edite .env com seu editor (usuário/senha do Postgres e do app)
```

### Opção A) Tudo com Docker Compose (app + db)

Sobe Postgres e a aplicação em containers.

```bash
# Compose v2
docker compose up -d --build
# ou (Compose v1)
docker-compose up -d --build
```

URLs:
- UI Web: http://localhost:8080/contas
- Swagger UI: http://localhost:8080/api/docs
- OpenAPI (estático): http://localhost:8080/api/openapi.json

Observações:
- O serviço `app` depende do `db` e usa `DB_URL=jdbc:postgresql://db:5432/${POSTGRES_DB}` dentro da rede do Compose.
- Para rebuild após alterações de código: `docker compose up -d --build app`.

Parar e limpar:

```bash
docker compose down           # para e mantém os dados do Postgres (volume)
docker compose down -v        # para e remove o volume (apaga dados)
```

### Opção B) App local + banco no Docker

Sobe apenas o Postgres via Docker e roda o app com Maven localmente.

```bash
# Sobe somente o banco
docker compose up -d db

# Em outro terminal, inicia a aplicação
mvn spring-boot:run
```

URLs:
- UI Web: http://localhost:8080/contas
- Swagger UI: http://localhost:8080/api/docs
- OpenAPI (estático): http://localhost:8080/api/openapi.json

## Variáveis de ambiente (.env)

- POSTGRES_DB: nome do banco (default: contabilidade)
- POSTGRES_USER / POSTGRES_PASSWORD: credenciais do superusuário do Postgres
- APP_DB_USER / APP_DB_PASSWORD: usuário da aplicação (utilizado por JPA)
- DB_PORT: porta publicada do Postgres (default: 5432)

A aplicação lê o arquivo .env automaticamente (Spring config import) para datasource e Flyway.

Em Docker Compose (serviço `app`), a variável `DB_URL` já é definida para apontar ao serviço `db`.

## Banco de dados e migrações (Flyway)

- Migrações em src/main/resources/db/migration.
- V1 cria a tabela public.tb_contas, sequências, gatilhos de auditoria (updated_at) e dados iniciais (classes de contas raiz e alguns níveis inferiores). Também cria/grava permissões e RLS usando placeholders do Flyway para o usuário do app.
- V2 complementa permissões (GRANTs) para a role core_contas_manage.
- V3 adiciona constraint triggers (deferrable):
  - Bloqueio de colunas imutáveis (id, created_by_system, id_superior, sequencia, created_at, updated_at – com exceção do update via trigger de auditoria).
  - Validação de que id_superior aponta para conta sintética (analitica = false).
  - Impede marcar uma conta como analítica (analitica = true) se ela possuir inferiores.
- O script docker/initdb/01-init.sh garante a criação do APP_DB_USER no container e grants mínimos.

Observação: a aplicação usa dois contextos de credenciais no Flyway e no JPA:
- Flyway: POSTGRES_USER/POSTGRES_PASSWORD (superuser) para executar migrações administrativas.
- JPA: APP_DB_USER/APP_DB_PASSWORD com permissões concedidas via migrações.

## API

- Base path: /api
- Endpoints principais (ver OpenAPI para detalhes):
  - GET /contas?view=flat|tree
  - GET /contas/{id}?view=flat|tree
  - POST /contas
  - PUT /contas/{id}
  - DELETE /contas/{id}
  - GET /lancamentos
  - GET /lancamentos/{id}

Swagger UI: /api/docs. O arquivo docs/openapi.json é copiado para static/api/openapi.json durante o build (plugin maven-resources), e servido em /api/openapi.json.

## Regras de Negócio

Esta seção consolida as regras vigentes no domínio (banco + backend). Onde aplicável, são impostas por gatilhos/constraints no PostgreSQL e refletidas na UI/serviço.

### Contas (tb_contas)

- Estrutura e imutabilidade
  - Colunas imutáveis após criação: id, created_by_system, id_superior, sequencia, created_at e updated_at (exceto quando atualizado automaticamente pelo gatilho de auditoria).
  - updated_at é preenchido automaticamente a cada UPDATE (trigger BEFORE UPDATE).
  - Entre irmãos (mesmo id_superior) a sequencia é única (código derivado do caminho, ex.: 1.2.3).

- Hierarquia e tipo (analítica/sintética)
  - Uma conta superior deve ser sempre sintética (analitica = false).
  - Não é permitido tornar uma conta analítica (analitica = true) se ela já possui inferiores.
  - Contas sintéticas (analitica = false) não podem possuir lançamentos associados (nem crédito nem débito).

- Natureza e “redutora”
  - A natureza “redutora” é relativa à raiz do grupo: uma conta é redutora quando sua natureza (credora/devedora) difere da natureza da raiz.
  - Uma conta redutora não pode ter inferiores não-redutoras (validação tanto do lado do pai quanto do filho imediato).

- Aceita movimento oposto (aceita_movimento_oposto)
  - Uma conta só pode aceitar movimento oposto se a superior também aceitar.
  - Não é permitido definir aceita_movimento_oposto = false enquanto existir qualquer descendente que aceite.
  - Provisionamento inicial: raízes com sequencia 1, 2 e 3 e todos os seus descendentes não-redutoras iniciam aceitando movimento oposto.

- Ativação (ativa)
  - Ao inativar uma conta (ativa = false), a soma dos débitos deve ser igual à soma dos créditos (saldo líquido zero) para permitir a atualização.

- Edição/Exclusão na UI/Serviço
  - createdBySystem = true impede edição e exclusão.
  - Alterar tipo (analítica/sintética) é bloqueado quando há inferiores; demais regras de edição obedecem às validações acima.

### Lançamentos (tb_lancamentos)

- Regras de dados
  - descricao não pode ser vazia.
  - valor > 0 (zero e negativos não são permitidos).
  - id_conta_credito e id_conta_debito devem ser distintos.
  - Ambas as contas usadas devem ser analíticas.

- Sentido do movimento vs. natureza da conta
  - Natureza relativa: a condição de um lançamento ser NATURAL ou REDUTOR é avaliada em relação à natureza absoluta (credora/devedora) da conta considerada. Para lançamentos indiretos (agregados via contas sintéticas), a natureza das contas intermediárias não altera a classificação; considera-se apenas a natureza da conta atual e o lado do lançamento (débito/crédito).
  - Movimento contrário (REDUTOR) só é permitido quando a conta aceitar movimento oposto.
  - Exemplos:
    - Conta credora: crédito é NATURAL; débito é REDUTOR (oposto).
    - Conta devedora: débito é NATURAL; crédito é REDUTOR (oposto).

- Imutabilidade quando há contas inativas
  - Um lançamento associado a qualquer conta inativa (crédito ou débito) é imutável: não pode sofrer UPDATE nem DELETE enquanto a associação envolver conta(s) inativa(s).

- Auditoria
  - updated_at é atualizado automaticamente em UPDATE (usa a mesma função de auditoria das contas).

### Segurança e RLS

- Contas (tb_contas)
  - RLS habilitado. Role intermediária: core_contas_manage.
  - Políticas:
    - SELECT: permitido para core_contas_manage.
    - INSERT/UPDATE/DELETE: permitidos apenas quando created_by_system = false.
  - Permissões públicas amplas são revogadas.

- Lançamentos (tb_lancamentos)
  - Role core_lancamentos_manage com SELECT/INSERT/UPDATE/DELETE na tabela e SELECT/USAGE na sequência.

## Glossário de Negócios

- Plano de contas: estrutura hierárquica das contas contábeis usada para classificar lançamentos.
- Conta: nó do plano de contas. Pode ser sintética (agrupadora) ou analítica (lançável).
- Conta sintética (analitica=false): não recebe lançamentos; serve para agrupar contas analíticas.
- Conta analítica (analitica=true): pode receber lançamentos (débito/crédito).
- Superior / Inferior / Raiz: relação hierárquica. Raiz é a conta sem superior; inferiores são os filhos.
- Sequência / Código: número ordinal entre irmãos; o código da conta é a concatenação das sequências no caminho (ex.: 1.2.3).
- Natureza (credora/devedora): indicação do lado natural do saldo/movimento da conta.
- Natureza relativa: classificação do lançamento (NATURAL ou REDUTOR) em relação à natureza absoluta da conta avaliada. Em lançamentos indiretos (quando vistos de uma conta sintética), ignora-se a natureza das contas intermediárias; considera-se apenas a natureza da conta atual e se o lançamento é débito (lado D) ou crédito (lado C).
- Conta redutora: conta cuja natureza difere da natureza da raiz do seu grupo.
- Aceita movimento oposto: permissão para registrar movimentos no sentido contrário à natureza da conta.
- Lançamento: registro contábil com data de competência, valor, conta de débito e conta de crédito.
- Débito / Crédito: lados do lançamento. O sentido “natural” depende da natureza da conta.
- Movimento natural: débito em conta devedora ou crédito em conta credora.
- Movimento oposto: débito em conta credora ou crédito em conta devedora; só permitido quando a conta aceitar.
- Data de competência: data em que o efeito econômico do lançamento ocorre.
- Saldo natural: saldo calculado respeitando a natureza da conta.
- Saldo contábil: diferença algébrica de débitos e créditos sem considerar a natureza.
- Conta ativa/inativa: contas inativas não podem ter saldo líquido diferente de zero para serem inativadas; lançamentos associados a contas inativas tornam-se imutáveis.
- created_by_system: flag que indica contas provisionadas pelo sistema; impede edição e exclusão.

## UI Web

- Lista de contas: /contas (exibe código, descrição, saldo formatado e ações). Linhas de contas sintéticas ficam em negrito; contas redutoras em itálico.
- Formulário de criação/edição em /contas/new e /contas/{id}/edit com controles reutilizáveis (fragments/form-controls.html):
  - Campo seletor de conta superior, tipo de conta como radios segmentados (analítica/sintética) e flag de redutora.
  - Editabilidade de campos controlada pelo back-end (editable, editableProperties) e refletida na UI.

## Como rodar testes

```bash
mvn test
```

Os testes usam o mesmo Postgres da aplicação (perfil test em src/test/resources/application-test.properties). Garanta que o container do banco esteja em execução.

## Build de produção

```bash
mvn -DskipTests package
# artefato target/contabilidade-pessoal-*.jar
```

Para executar o JAR:

```bash
java -jar target/contabilidade-pessoal-*.jar
```

Com Docker, a imagem é construída usando o arquivo `dockerfile` na raiz (multi-stage com Maven + OpenJDK 17). Se seu ambiente for sensível a maiúsculas/minúsculas para o nome do arquivo, garanta que o Compose esteja apontando para o nome correto do arquivo de build.

## Estrutura do projeto (essencial)

- src/main/java/me/josecomparotto/contabilidade_pessoal
  - controller/api e controller/web (REST e páginas Thymeleaf)
  - model (entity Conta, DTOs, enums)
  - repository (ContaRepository)
  - service (ContasService)
  - application/converter, application/mapper
  - config/WebConfig (CORS e redirect para docs)
- src/main/resources
  - db/migration (Flyway)
  - templates (Thymeleaf)
  - static/css, static/js
  - application.yaml
- docs/openapi.json (especificação da API)
- docker-compose.yml e docker/initdb/01-init.sh

## Notas de domínio

- Código da conta é derivado do caminho (sequência hierárquica) ex.: 1.2.3.
- Natureza redutora é determinada em relação à natureza da conta raiz do grupo.
- Regras de edição/exclusão:
  - createdBySystem = true impede edição e exclusão.
  - Não é possível alterar tipo se houver inferiores.
  - Exclusão só é permitida quando não há inferiores e a conta não é de sistema.

## Licença

Não definida neste repositório até o momento.
