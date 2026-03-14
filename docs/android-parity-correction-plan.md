# Plano de Correcao Final - Paridade Android x Web

Data: 2026-03-14  
Baseline web congelado: `promo_APP_Web@73c4984`

## Estado atual por release

### Release 1 (base de paridade)
- Concluido: rotas principais espelhadas, auth/login/signup/review, guardas de sessao/papel/obra/permissao.
- Concluido: shell base para `index`, `sem-acesso`, `obras`, `dashboard`, `cadastros`, `usuarios-acessos`.
- Concluido: `AccessPolicy` com regras equivalentes ao helper RBAC do web.

### Release 2 (paridade funcional)
- Concluido: CRUD base de obras/cadastros/pedidos/estoque/recebimento.
- Concluido: lixeira + restauracao + exclusao permanente em obras/cadastros.
- Concluido: pedidos com busca, filtro, criacao, edicao, aprovacao/cancelamento e soft-delete.
- Concluido: recebimento com `codigo_compra` obrigatorio + `data_recebimento` + upsert de estoque.
- Concluido: usuarios/acessos com edicao de ativo/inativo, papel, modo template/custom e vinculos de obra.
- Pendente: paridade completa do fluxo de grants customizados por permissao/escopo de obra (nivel `UsuariosAcessos` web).
- Pendente: i18n total (pt-BR/en/es) com cobertura de labels/mensagens em todas as telas Android.

### Release 3 (paridade visual alta + hardening)
- Em andamento: alinhamento de tema/tokens Android para aproximar da identidade do web.
- Pendente: refinamento de layout para densidade/tabela/dialogos equivalente ao web.
- Pendente: testes de navegacao e matriz RBAC por rota (perfil x obra).
- Concluido: pipeline CI com `lint + tests + assembleDebug + assembleRelease`.

## Backlog de fechamento (ordem de execucao)

1. `UsuariosAcessos`: trazer grants detalhados (`user_permission_grants`, `user_permission_obras`) com UX de escopo tenant/all_obras/selected_obras.
2. `Pedidos`: substituir campos manuais de IDs por seletores completos (dialog/lista) e modal de detalhes equivalente ao web.
3. `Recebimento`: trocar entrada livre de data ISO por seletor de data com validacao.
4. `I18n Android`: provider + dicionarios `pt-BR/en/es` + persistencia de preferencia.
5. `Paridade visual`: componentes base (PageShell, cards, tabela responsiva, empty/error/loading) com guideline do web.
6. `Teste de paridade`: suite de navegacao/guardas cobrindo rotas protegidas e negacoes 401/403.

## Critérios de saida

- Todas as rotas web com equivalente Android funcional, incluindo estados de bloqueio.
- Sem acao manual por ID para fluxos principais de operacao.
- Textos principais internacionalizados nos 3 idiomas.
- Checklist visual aprovado por tela (estrutura, hierarquia, feedback).
- CI Android verde (`lintDebug`, `testDebugUnitTest`, `assembleDebug`, `assembleRelease`).
