# Organização do código

O projeto usa um único módulo Android, com pacotes por responsabilidade. `MainActivity` permanece na raiz como ponto de entrada, preservando o componente do launcher nas atualizações.

| Pacote | Responsabilidade |
| --- | --- |
| `game` | Motor de julgamentos, recepção, roteiro e treino; sem dependências Android. |
| `model` | Batidas, entregas, julgamentos e cálculos de resultado. |
| `audio` | Reprodução da trilha e efeitos acionados pelo jogador. |
| `data` | Preferências de recorde e sincronia; mantém as chaves existentes. |
| `ui.game.animation` | Cálculos de poses, transições, avisos e reações. |
| `ui.game.view` | Cena, efeitos, ambiente e encaminhamento do toque. |
| `ui.game.sprite` | Recortes, referências de escala e desenho dos sprites. |
| `ui.common` | Configuração de tela imersiva. |
| `ui.about` / `ui.editor` | Tela Sobre e editor de batidas. |

A posição da música continua sendo a referência de tempo. A interface observa os resultados; os efeitos visuais não concedem pontos. O treino, o bloqueio após MISS, os zooms dos trios e as cores mantêm as regras existentes.

As poses são referenciadas por recursos explícitos `R.drawable`, com os recortes definidos em `sprites_geometry.json`. Isso permite conferir recursos na compilação e dispensa buscar desenhos por nome em tempo de execução. Os PNGs e os arquivos WAV não foram alterados.

A estrutura dos source sets segue a [documentação do Android](https://developer.android.com/studio/projects). A divisão em pacotes é uma decisão deste projeto, dimensionada para uma fase, sem módulos ou abstrações sem uso.
