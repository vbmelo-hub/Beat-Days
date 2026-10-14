# Beat Days

Jogo de ritmo nativo em Java para Android. Fase única: **Café Rush**. Android 8.0/API 26+, tela horizontal, aplicativo `com.example.beatdays`.

## Estrutura

- `app/src/main/java`: código do aplicativo, separado em `game`, `model`, `audio`, `data` e `ui`.
- `app/src/main/res`: layouts, estilos, áudio e sprites ativos.
- `app/src/test`: testes locais do motor, roteiro e animações.
- `app/src/androidTest`: testes de interação e renderização no Android.
- `tools/audio`: fontes da composição e metadados da trilha.
- `tools/quality`: formatação e verificações de organização.
- `docs`: arquitetura, desenvolvimento, distribuição e créditos.
- `dist`: APKs e artefatos gerados; não faz parte do código versionado.

## Desenvolvimento

Abra esta pasta no Android Studio, configure o SDK local e use JDK 21. Pelo terminal do projeto:

```powershell
.\tools\quality\verify.ps1
```

O verificador usa um cache Gradle local para evitar falhas do executor de testes em perfis Windows com acentos. Use `-Offline` quando as dependências já estiverem disponíveis.

Para conferir a formatação: `./tools/quality/format-java.ps1 -Check`. Para formatar: execute o mesmo script sem `-Check`.

Detalhes em [desenvolvimento](docs/development.md), [arquitetura](docs/architecture.md), [distribuição](docs/release.md) e [créditos](docs/credits.md).

Resultado desta revisão: [organização e validação 1.0.6](docs/review-1.0.6.md).
