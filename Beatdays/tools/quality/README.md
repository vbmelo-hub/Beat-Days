# Qualidade

`format-java.ps1` aplica a formatação Java/AOSP ou confere os arquivos com `-Check`. Requer um JDK e usa um download fixo com hash verificado; o JAR não é incluído no aplicativo.

`check_project.py` usa somente a biblioteca padrão do Python para conferir correspondência entre pacotes e diretórios, referências de recursos, classes do manifesto e dos layouts, nomes de IDs, recortes PNG e integridade da música e dos sete efeitos. Execute a partir da raiz: `python tools/quality/check_project.py`.

`verify.ps1` executa testes locais, compilação do aplicativo e dos testes Android e lint pelo wrapper Gradle. Usa `.tools/gradle-home`, evitando caminhos com acentos no executor de testes do Windows, e reaproveita apenas dependências e wrapper do cache anterior. Execute com JDK 21: `./tools/quality/verify.ps1`; `-Offline` é opcional. Testes em um dispositivo conectado são executados com `./gradlew.bat -g .tools/gradle-home connectedDebugAndroidTest --no-configuration-cache`.
