# promo_APP_Android (Kotlin Native)

App Android 100% nativo em Kotlin + Jetpack Compose + MVVM.

## Escopo V1
- Auth (Supabase GoTrue)
- Obras (lista e selecao)
- Pedidos (lista + busca + criacao/edicao)
- Estoque (consulta e atualizacao)

## Arquitetura
- `app`: navegacao e composicao das features
- `core`: modelos/contratos/erros/estado de tela
- `data`: cliente Supabase (Auth + PostgREST), storage seguro e repositorios
- `feature-auth`, `feature-obras`, `feature-pedidos`, `feature-estoque`: tela + ViewModel

## Requisitos locais
- JDK 17
- Android SDK instalado (variavel `ANDROID_HOME` ou `local.properties` com `sdk.dir=...`)
- Variaveis de ambiente (ou `~/.gradle/gradle.properties`):
  - `SUPABASE_URL`
  - `SUPABASE_ANON_KEY`

## Build e testes locais
Linux/macOS:
```bash
./gradlew lintDebug testDebugUnitTest assembleDebug
```

Windows:
```bat
gradlew.bat lintDebug testDebugUnitTest assembleDebug
```

## Release interna (APK)
- Pipeline CI (`android-native-ci.yml`): lint + testes + debug APK.
- Pipeline release (`android-native-release.yml`): gera debug APK e release APK.
- A release assinada e produzida quando os secrets estiverem configurados:
  - `ANDROID_KEYSTORE_BASE64`
  - `KEY_ALIAS`
  - `KEYSTORE_PASSWORD`
  - `KEY_PASSWORD`

## Contrato de ambiente
- O app utiliza `SUPABASE_URL` e `SUPABASE_ANON_KEY` em `BuildConfig`.
- Nao versionar `.env`, keystore ou chaves privadas.

## Legacy congelado
O app Capacitor antigo foi congelado em `legacy-capacitor/` apenas para referencia e rollback historico.
