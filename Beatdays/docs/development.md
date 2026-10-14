# Desenvolvimento e convenções

## Código

Java com formatação Android/AOSP, aplicada por google-java-format 1.24.0: quatro espaços, imports explícitos e remoção de imports sem uso. Classes usam UpperCamelCase, métodos e campos lowerCamelCase, constantes UPPER_SNAKE_CASE e pacotes em minúsculas. Os nomes existentes em português são mantidos; nomes de pacotes e arquivos descrevem suas responsabilidades.

Comentários são limitados à identificação de classes, seções e utilidades. Licenças dos arquivos fornecidos por terceiros, como o wrapper Gradle, são preservadas. Nomes de recursos Android usam lower_snake_case. `.editorconfig` e `.gitattributes` registram UTF-8, espaçamento e finais de linha.

O formatador é uma ferramenta de desenvolvimento, não uma dependência do APK. O script baixa sua versão fixa do Maven Central na primeira execução, confere SHA-256 e a guarda em `.tools`, ignorada pelo controle de versão. Referências: [formatador e opção AOSP](https://github.com/google/google-java-format), [guia Java](https://google.github.io/styleguide/javaguide.html).

## Verificações

```powershell
.\tools\quality\verify.ps1
.\gradlew.bat -g .tools/gradle-home connectedDebugAndroidTest --no-configuration-cache
.\tools\quality\format-java.ps1 -Check
python tools/quality/check_project.py
```

Os testes locais cobrem as janelas PERFECT/GREAT/GOOD, perdas, recuperação, roteiro, treino e animações. Os testes Android conferem entradas físicas, pausa, recriação, sincronia, menus, sprites e uma partida completa. Testes de protótipos retirados foram eliminados; as verificações do resultado agora consultam os cartões visíveis.

Use o SDK configurado em `local.properties`, sem versionar esse arquivo. Chaves de produção e senhas ficam em `signing`, também ignorada. A dependência Activity usa a API Java e o runner Android é declarado diretamente; Espresso sem uso foi retirado. A restrição Core KTX 1.9.0 preserva a versão transitiva resolvida na distribuição anterior, sem acrescentar uma dependência direta. Versões das bibliotecas existentes foram mantidas.

O verificador usa `.tools/gradle-home` e JDK 21. Na primeira execução, reaproveita somente as dependências e o wrapper do cache anterior. Não copia credenciais nem altera configurações globais. O caminho sem acentos evita a falha observada ao iniciar o executor de testes do Gradle no Windows; o projeto também deve estar em um caminho sem acentos. O cache pode ser recriado. `-Offline` dispensa consultas de rede quando todas as dependências estão presentes.

O exportador SARIF da ferramenta de lint desta configuração apresentou erro interno. Sua saída permanece desativada; verificações e relatórios HTML/texto continuam ativos.
