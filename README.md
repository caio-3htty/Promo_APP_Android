# promo_APP_Android (Kotlin Native)

App Android 100% nativo em Kotlin + Jetpack Compose + MVVM.

## Escopo atual (paridade web em progresso)
- Auth completo: login + signup (`company_owner` / `company_internal`) + review de solicitacao por token
- Guardas RBAC/obra alinhados ao web (session, papel, permissao e escopo por obra)
- Rotas Android espelhadas do web:
  - `index`, `sem-acesso`, `obras`, `dashboard/{obraId}`
  - `dashboard/{obraId}/pedidos`, `dashboard/{obraId}/recebimento`, `dashboard/{obraId}/estoque`
  - `cadastros/fornecedores`, `cadastros/materiais`, `cadastros/material-fornecedor`
  - `usuarios-acessos`, `acesso/avaliar/{token}`
- Fluxos operacionais: pedidos, recebimento com `codigo_compra` obrigatorio e atualizacao de estoque, cadastros e gestao basica de usuarios/acessos.

Baseline de contrato web congelado: ver [docs/web-baseline-parity.md](docs/web-baseline-parity.md).

## Arquitetura
- `app`: navegacao e composicao das features
- `core`: modelos/contratos/erros/estado de tela
- `data`: cliente Supabase (Auth + PostgREST), storage seguro e repositorios
- `feature-auth`, `feature-pedidos`, `feature-estoque`: tela + ViewModel de modulos centrais
- `app/*Screens.kt`: shell de paridade para rotas adicionais (obras/cadastros/usuarios/recebimento)

## Requisitos locais
- JDK 17
- Android SDK instalado (variavel `ANDROID_HOME` ou `local.properties` com `sdk.dir=...`)
- Variaveis de ambiente (ou `~/.gradle/gradle.properties`):
  - `SUPABASE_PROJECT_REF` (opcional; default `awkvzbpnihtgceqdwisc`)
  - `SUPABASE_URL`
  - `SUPABASE_ANON_KEY`
  - aliases aceitos: `VITE_SUPABASE_URL`, `VITE_SUPABASE_PUBLISHABLE_KEY`

## Build e testes locais
Linux/macOS:
```bash
./gradlew lintDebug testDebugUnitTest assembleDebug assembleRelease
```

Windows:
```bat
gradlew.bat lintDebug testDebugUnitTest assembleDebug assembleRelease
```

## Code Hygiene
- Verificar referencias de legado: `git grep -n "legacy-capacitor"`
- Gate tecnico:
  - Linux/macOS: `./gradlew lintDebug testDebugUnitTest assembleDebug assembleRelease`
  - Windows: `gradlew.bat lintDebug testDebugUnitTest assembleDebug assembleRelease`

## Release interna (APK)
- Pipeline CI (`android-native-ci.yml`): lint + testes + debug APK.
- Pipeline release (`android-native-release.yml`): gera debug APK e release APK.
- A release assinada e produzida quando os secrets estiverem configurados:
  - `ANDROID_KEYSTORE_BASE64`
  - `KEY_ALIAS`
  - `KEYSTORE_PASSWORD`
  - `KEY_PASSWORD`

## Contrato de ambiente
- O app injeta `SUPABASE_URL` e `SUPABASE_ANON_KEY` em `BuildConfig`.
- Resolucao de fallback:
  1. `gradle.properties` do projeto/usuario
  2. variaveis de ambiente `SUPABASE_*`
  3. aliases `VITE_SUPABASE_*`
  4. URL derivada de `SUPABASE_PROJECT_REF`
- Nao versionar `.env`, keystore ou chaves privadas.

## Limpeza de legado
O legado Capacitor foi removido deste repositorio. O fluxo oficial e exclusivamente Kotlin nativo.
