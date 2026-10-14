# Produção de áudio

Scripts de composição e prévia, independentes do aplicativo. Requerem Python 3 e NumPy.

Execute `python tools/audio/compor_cafe_rush.py` na raiz para gerar a trilha final e os sete efeitos em `app/src/main/res/raw`, além dos artefatos em `dist/audio`. Execute `python tools/audio/renderizar_previa.py` para gerar a prévia de acertos e MISS. A partitura CSV corresponde ao roteiro ativo em `game/RoteiroRecepcao.java`; alterações no roteiro exigem atualizar o CSV antes de recompor.

`metadata/cafe_rush.json` registra os hashes e eventos dos arquivos que acompanham esta revisão. O acompanhamento foi mantido nesta organização; os scripts não foram executados para substituir a música instalada.
