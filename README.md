# Prumo Android Client

Shell Android (Capacitor) que embute o build do `prumo-web-client`.

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

Por padrao os scripts procuram o repo web em `../prumo-web-client`.
Se estiver em outro caminho, defina `PRUMO_WEB_CLIENT_DIR`.

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
As variaveis sao consumidas no build do `prumo-web-client`:
- `VITE_SUPABASE_URL`
- `VITE_SUPABASE_PUBLISHABLE_KEY`

## GitHub Actions
Workflows incluidos:
- `android-ci`: valida sync do shell com build web embutido.

# Promo_APP_Android
