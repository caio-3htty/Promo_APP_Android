# Baseline de Paridade Web -> Android

- Data do freeze: 2026-03-14
- Commit web de referencia: `73c4984` (`promo_APP_Web`)
- Objetivo: usar este commit como contrato funcional/visual durante a migracao Android.

## Matriz de Rotas (contrato)

| Web | Android |
|---|---|
| `/login` | `login` |
| `/acesso/avaliar` | `acesso/avaliar/{token}` |
| `/` | `index` |
| `/sem-acesso` | `sem-acesso` |
| `/obras` | `obras` |
| `/usuarios-acessos` | `usuarios-acessos` |
| `/cadastros/fornecedores` | `cadastros/fornecedores` |
| `/cadastros/materiais` | `cadastros/materiais` |
| `/cadastros/material-fornecedor` | `cadastros/material-fornecedor` |
| `/dashboard/:obraId` | `dashboard/{obraId}` |
| `/dashboard/:obraId/pedidos` | `dashboard/{obraId}/pedidos` |
| `/dashboard/:obraId/recebimento` | `dashboard/{obraId}/recebimento` |
| `/dashboard/:obraId/estoque` | `dashboard/{obraId}/estoque` |

## Guardas equivalentes implementadas

- `ProtectedRoute` -> `RequireSession` no `MainActivity`
- `RequireOperationalAccess` -> `AccessPolicy.hasOperationalAccess`
- `RequireRole` -> validacoes por `AppRole` nas rotas protegidas
- `RequireObraAccess` -> `AccessPolicy.hasObraAccess`
- `RequirePermission` e `RequireObraPermission` -> `AccessPolicy.can(permission, obraId)`

## Escopo entregue neste ciclo

- Auth: login + signup (`company_owner` / `company_internal`) + review token (`account-access-request`)
- Navegacao: espelhamento de rotas centrais do web com bloqueios RBAC/obra
- Modulos: Obras, Pedidos, Recebimento, Estoque, Cadastros, Usuarios/Acessos
- Base visual: `PromoTheme` e layout mobile com estados de carregamento/erro
