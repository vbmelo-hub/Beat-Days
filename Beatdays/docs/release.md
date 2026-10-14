# Distribuição

Versão desta organização: **1.0.6**, código **8**. Os APKs são gerados em `dist/1.0.6` após a validação.

- `Beatdays-CafeRush-1.0.6-atualizacao.apk`: assinatura de desenvolvimento da instalação de teste anterior.
- `Beatdays-CafeRush-1.0.6-assinado.apk`: build otimizado com a chave de produção existente.

O Android exige assinaturas iguais para atualizar uma instalação. Aplicativo e componente do launcher foram preservados, assim como recordes e ajustes salvos. A chave de produção permanece em `signing/beatdays-release.jks`; senhas não estão na documentação ou nos artefatos públicos.

Para gerar a distribuição: `./gradlew.bat -g .tools/gradle-home assembleRelease --no-configuration-cache`, com a configuração privada existente de assinatura. Os resultados das verificações desta revisão e os hashes dos APKs acompanham os artefatos de distribuição.

Código, recursos, arquivo histórico e distribuições anteriores foram preservados em `Beatdays-backup-antes-organizacao-1.0.6.zip`, nos artefatos da conversa, antes de retirar as sobras do projeto ativo. O backup não inclui chaves privadas, senhas ou configuração local do SDK. O histórico fica fora da árvore de trabalho ativa.
