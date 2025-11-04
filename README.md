# Especificação Técnica do Sistema Contabilidade Pessoal

## 1. Visão Geral

O sistema **Contabilidade Pessoal** é uma aplicação de controle contábil pessoal e familiar, desenvolvida em **Spring Boot 3.5.5** com persistência em **PostgreSQL**.  
Atualmente, é orientado a **templates Thymeleaf** (sem API REST), e foi projetado para registrar e gerenciar lançamentos contábeis baseados em partidas dobradas entre contas de débito e crédito.

O foco principal é permitir ao usuário controlar seu patrimônio, receitas e despesas utilizando o mesmo raciocínio de um plano de contas contábil.

---

## 2. Arquitetura e Stack Tecnológica

**Tipo de aplicação:** Monolítica (Spring Boot MVC + Thymeleaf).  
**Banco de dados:** PostgreSQL (implantado na Azure).  
**Infraestrutura:** App Service (aplicação) + Azure Database for PostgreSQL.  
**Gerenciamento de schema:** Flyway (migrações versionadas).  
**Containerização (opcional):** docker-compose.yml disponível para ambiente local.  
**Linguagem:** Java 17.

### Estrutura principal de pastas

```plain
/src/main/java/me/josecomparotto/contabilidade_pessoal
├── application/
│ ├── converter/ (Conversores JPA)
│ ├── mapper/ (Mapeamento entre Entidades e DTOs)
│ ├── service/ (Regras de negócio)
│ ├── controller/ (Controllers MVC Thymeleaf)
├── model/
│ ├── entity/ (Entidades JPA)
│ ├── dto/ (Objetos de transferência de dados)
│ └── enums/ (Natureza, TipoConta, Status, etc.)
/migrations (scripts Flyway SQL)
```

---

## 3. Módulos e Funcionalidades

### 3.1. Contas
- Estrutura hierárquica (contas sintéticas e analíticas).  
- Categorizadas por **natureza** (credora ou devedora).  
- Suporte a **contas redutoras**.  
- Controle de **aceitação de movimento oposto**.  
- Controle de **ativação/inativação**.  
- Gatilhos de integridade e consistência automáticos via PostgreSQL.

### 3.2. Lançamentos
- Representam uma transação contábil com **conta de débito e conta de crédito**.  
- Campos principais: `descricao`, `valor`, `data_competencia`, `status`.  
- Status possíveis: `PREVISTO`, `EFETIVO`, `CANCELADO`.  
- Regras rígidas de integridade contábil (detalhadas abaixo).

### 3.3. Visualização
- Exibição hierárquica das contas.  
- Saldo natural por conta e indicador de redutora.  
- Listagem e edição de lançamentos.  

---

## 4. Requisitos Funcionais

| ID | Descrição | Prioridade |
|----|------------|------------|
| RF001 | Permitir o cadastro de contas contábeis com hierarquia. | Alta |
| RF002 | Permitir edição apenas de contas criadas pelo usuário. | Alta |
| RF003 | Permitir cadastro de lançamentos contábeis (partida dobrada). | Alta |
| RF004 | Impedir lançamentos com contas sintéticas. | Alta |
| RF005 | Impedir lançamentos com contas inativas. | Alta |
| RF006 | Permitir marcação de lançamentos como **Previstos**, **Efetivos** ou **Cancelados**. | Média |
| RF007 | Atualizar saldos automaticamente conforme lançamentos. | Alta |
| RF008 | Controlar natureza contábil (credora/devedora) de cada conta. | Alta |
| RF009 | Impedir violação de regras de hierarquia e redutoras. | Alta |
| RF010 | Permitir inativar contas apenas com saldo zero. | Média |

---

## 5. Requisitos Não Funcionais

| Categoria | Requisito |
|------------|------------|
| Banco de Dados | PostgreSQL 16+ |
| Desempenho | Operações CRUD com resposta < 2s |
| Segurança | Acesso apenas local (uso pessoal) |
| Backup | Dump SQL automatizado ou manual |
| Deploy | Azure App Service + Azure Database |
| Versionamento | Git + Flyway + Maven |
| Internacionalização | Apenas português (pt-BR) |

---

## 6. Regras de Negócio

### 6.1. Contas (`tb_contas`)
1. **Hierarquia:** `id_superior` deve apontar sempre para uma conta **sintética**.  
2. **Analítica:** Apenas contas analíticas podem receber lançamentos.  
3. **Redutora:** Natureza oposta à da raiz (ativo/passivo/receita/despesa).  
4. **Aceita movimento oposto:** herda da conta superior, e não pode ser `FALSE` se existir descendente `TRUE`.  
5. **Imutabilidade:** Campos `id`, `id_superior`, `sequencia`, `created_by_system`, `created_at` são imutáveis.  
6. **Inativação:** Só é permitida se o saldo da conta for zero (soma débitos = soma créditos).

### 6.2. Lançamentos (`tb_lancamentos`)
1. Contas de débito e crédito devem ser **diferentes**.  
2. Ambas as contas devem ser **analíticas**.  
3. Valor deve ser **positivo (>0)**.  
4. Movimento contrário à natureza só é permitido se a conta **aceitar movimento oposto**.  
5. Lançamentos ligados a contas inativas são **imutáveis**.  
6. Campos `id`, `created_at` e `updated_at` são imutáveis (exceto via trigger).  

### 6.3. Integridade e Auditoria
- Toda alteração atualiza `updated_at` automaticamente.  
- Triggers de validação garantem consistência entre `tb_contas` e `tb_lancamentos`.  

---

## 7. Modelo de Dados e Entidades

### 7.1. Conta
| Campo | Tipo | Descrição |
|--------|------|-----------|
| id | int | Identificador |
| id_superior | int | Conta pai (hierarquia) |
| sequencia | int | Ordem de exibição |
| descricao | text | Nome da conta |
| analitica | bool | Se pode receber lançamentos |
| credora | bool | Natureza (TRUE=credora, FALSE=devedora) |
| aceita_movimento_oposto | bool | Permite lançamentos opostos |
| ativa | bool | Indica se está ativa |
| created_by_system | bool | Indica conta padrão do sistema |
| created_at | timestamptz | Data de criação |
| updated_at | timestamptz | Última atualização |

### 7.2. Lançamento
| Campo | Tipo | Descrição |
|--------|------|-----------|
| id | bigint | Identificador |
| descricao | text | Descrição do lançamento |
| valor | numeric(14,2) | Valor positivo |
| data_competencia | date | Data contábil |
| id_conta_credito | int | Conta de crédito |
| id_conta_debito | int | Conta de débito |
| status | varchar(50) | PREVISTO, EFETIVO, CANCELADO |
| created_at | timestamptz | Criação |
| updated_at | timestamptz | Atualização |

---

## 8. DTOs e Mapeamentos (View Models)

### ContaViewDto
Campos: `id`, `codigo`, `descricao`, `displayText`, `natureza`, `tipo`, `saldoAtual`, `redutora`, `aceitaMovimentoOposto`, `ativa`, `editable`, `deletable`, `editableProperties`.

### ContaNewDto
Campos: `descricao`, `tipo`.

### ContaEditDto
Campos: `descricao`, `tipo`, `redutora`.

### LancamentoDto
Campos: `id`, `descricao`, `valor`, `dataCompetencia`, `contaCredito`, `contaDebito`, `status`, `editable`, `deletable`.

### LancamentoNewDto
Campos: `descricao`, `valor`, `dataCompetencia`, `idContaCredito`, `idContaDebito`.

### LancamentoEditDto
Campos: `descricao`, `valor`, `dataCompetencia`, `status`.

---

## 9. Endpoints REST Planejados (futura migração)

| Endpoint | Método | Descrição |
|-----------|---------|------------|
| `/api/contas` | GET | Listar todas as contas |
| `/api/contas/{id}` | GET | Detalhar conta |
| `/api/contas` | POST | Criar nova conta |
| `/api/contas/{id}` | PUT | Atualizar conta |
| `/api/contas/{id}` | DELETE | Inativar ou excluir conta |
| `/api/lancamentos` | GET | Listar lançamentos |
| `/api/lancamentos/{id}` | GET | Detalhar lançamento |
| `/api/lancamentos` | POST | Criar novo lançamento |
| `/api/lancamentos/{id}` | PUT | Editar lançamento |
| `/api/lancamentos/{id}` | DELETE | Excluir lançamento |

---

## 10. Glossário

| Termo | Definição |
|--------|------------|
| Conta | Unidade contábil que representa um item patrimonial, de resultado ou fluxo. |
| Analítica | Conta final (lançável). |
| Sintética | Conta agregadora (não lançável). |
| Redutora | Conta com natureza oposta à da conta raiz. |
| Natureza | Indica se a conta é **credora** ou **devedora**. |
| Lançamento | Registro contábil de débito e crédito entre duas contas. |
| Movimento Oposto | Lançamento que inverte a natureza normal da conta. |
| Saldo Natural | Saldo considerando a natureza original da conta. |
| Status do Lançamento | PREVISTO, EFETIVO, CANCELADO. |

---

## 11. Backlog / Funcionalidades Futuras

| ID | Descrição | Prioridade |
|----|------------|------------|
| BF001 | API REST completa (Spring Boot) | Alta |
| BF002 | Frontend React (migração de Thymeleaf) | Alta |
| BF003 | Lançamentos recorrentes | Média |
| BF004 | Importação de extrato bancário (OFX/CSV) | Média |
| BF005 | Projeção de caixa e indicadores contábeis (CCL, CCL-C, DRE, Balanço) | Alta |
| BF006 | Anexos (comprovantes) | Baixa |
| BF007 | Exportação de relatórios em PDF/Excel | Média |

---

**Autor:** José Comparotto  
**Versão do Documento:** 1.0  
**Última atualização:** 2025-11-04