# Beat Days 1.0.6 — revisão e organização

Projeto revisado diretamente em `E:/Beatdays`, em 14/10/2026. Fase única Café Rush. Aplicativo `com.example.beatdays`, minSdk 26, versão 1.0.6/código 8.

## Alterações

- Pacotes por responsabilidade: `game`, `model`, `audio`, `data`, `ui.common`, `ui.about`, `ui.editor` e `ui.game` dividido em animação, desenho e sprites. Testes acompanham os pacotes de produção. O componente do launcher permanece `com.example.beatdays.MainActivity`.
- Retiradas nove classes de protótipos de fases/chart/metrônomo/figurinos, testes exclusivos desses protótipos e métodos sem uso. Mantido o motor de julgamento utilizado pela recepção.
- Removidos 43 recursos sem referências, ícones de template e duplicações de temas. As referências dos sprites usam IDs explícitos; o recurso `keep` e a busca dinâmica por nome deixaram de ser necessários.
- Preferências de recorde e sincronia centralizadas, preservando as chaves salvas e a leitura do recorde antigo.
- IDs Android em `lower_snake_case`; Java/AOSP com quatro espaços, imports explícitos, chaves nos condicionais/laços e comentários somente de finalidade de classes/funções/utilidades. Licenças dos arquivos externos foram preservadas.
- Dependências diretas ajustadas ao uso Java e ao runner Android. Restrição transitiva Core KTX mantém a versão anterior. Sem atualização das versões das bibliotecas.
- Documentação em `docs`, ferramentas em `tools/audio` e `tools/quality`, distribuições em `dist/1.0.6`. Convenções registradas em `.editorconfig`, `.gitattributes` e `.gitignore`. Chaves, caches e resultados de compilação ficam fora do código versionável.
- Verificador usa JDK 21 e cache Gradle local sem acentos. Contorna a falha observada do executor de testes no perfil Windows, sem modificar o ambiente global.

## Validação

- **127 testes locais e 53 testes Android aprovados, sem falhas ou testes ignorados.**
- Compilação debug, APK de testes, versão otimizada assinada e lint concluídos.
- Testes Android no emulador API 37, incluindo toques físicos, proteção contra spam, treino, pausa/recriação, sincronia, menus, captura/erro, alinhamento e transições dos sprites.
- Partida completa de 80 segundos com entradas reais: 31 PERFECT, zero MISS e precisão de 100%. Resultado detalhado em `Beatdays-organizacao-android/beatdays-qualidade-partida.txt`.
- Testes visuais antigos foram atualizados para considerar a mistura de 65 ms nas poses de repouso. Captura e erro continuam exigindo resposta imediata. Acrescentadas verificações da conclusão da mistura e da soma das opacidades.
- Formatação de todos os 83 arquivos Java, sintaxe dos scripts, pacotes/diretórios, referências de recursos, manifesto e recortes conferidos.
- **43 PNGs/WAVs ativos mantêm exatamente os hashes anteriores.** Os oito WAVs também foram conferidos dentro dos dois APKs. A trilha não foi recomposta nesta revisão.
- Nome, versão e assinaturas dos APKs conferidos; cópias entregues são idênticas às da distribuição no projeto.
- No APK assinado instalado no emulador, abertura horizontal, Sobre, pausa e retorno ao início também foram conferidos. Evidências em `Beatdays-organizacao-android/release-*.xml`.

O lint apresenta **zero erros e sete avisos**: três sobre orientação horizontal fixa, um sobre versão de dependência e três sobre quantidade/profundidade de views ou composição de um cabeçalho. Não foram ocultados. O exportador SARIF permanece desativado por erro interno da ferramenta; análises e relatórios HTML/texto continuam ativos. Não houve teste novo em celular físico nesta revisão.

## APKs

- `Beatdays-CafeRush-1.0.6-assinado.apk`: 40.8 MiB. SHA-256: `ab1cd0b337db79fb7e6126a40e0cae896150015272d5b6fa995ea4cdf7c83f95`.
- `Beatdays-CafeRush-1.0.6-atualizacao.apk`: 46.3 MiB. SHA-256: `323a454bc45398f72a5391a25ea4ab987e8e216d238c06ed0e9cc9774d1456ed`.

A versão `atualizacao` mantém a assinatura da instalação de desenvolvimento; a versão `assinado` usa a chave de produção existente. O Android exige certificados iguais para atualização.

## Backup e evidências

`Beatdays-backup-antes-organizacao-1.0.6.zip` preserva código, recursos, protótipos arquivados e distribuições anteriores antes da limpeza. Fica nos artefatos da conversa; não contém chaves privadas, senhas nem configuração local do SDK. Essas configurações continuam no projeto local.

Evidências públicas: logs de compilação, testes Android, assinaturas, manifesto de hashes e capturas no diretório de artefatos desta conversa. As fontes da trilha e os metadados estão em `tools/audio`; ferramentas de qualidade e instruções estão em `tools/quality` e `docs/development.md`.
