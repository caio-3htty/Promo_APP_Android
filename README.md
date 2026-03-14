# Prumo Android Client

Shell Android (Capacitor) que embute o build do `promo_APP_Web`.

## Escopo atual
- Paridade visual/funcional com o web.
- Empacotamento local do build web no APK.

## Requisitos
- Node.js 20+
- npm 10+
- Android Studio + Android SDK (para gerar APK local)

## Rodar local
```bash
npm install
npm run android:open
```

Por padrao os scripts procuram o repo web em `../promo_APP_Web`.
Se estiver em outro caminho, defina `PROMO_APP_WEB_DIR`.

## Fluxo de build Android
```bash
npm run android:sync
npm run android:build
```

## Validacao local
```bash
npm run doctor
```

## Variaveis de ambiente
As variaveis sao consumidas no build do `promo_APP_Web`:
- `VITE_SUPABASE_URL`
- `VITE_SUPABASE_ANON_KEY`
- `VITE_SUPABASE_PUBLISHABLE_KEY` (alias legado opcional)

## GitHub Actions
Workflows incluidos:
- `android-ci`: valida sync do shell com build web embutido.

# Promo_APP_Android



