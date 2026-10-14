package com.example.beatdays.ui.game.animation;

import com.example.beatdays.game.RoteiroRecepcao;
import com.example.beatdays.model.EntregaCafe;

import java.util.List;

/** Identifica os sons do tutorial pelo chart. Não observa toques nem altera resultados. */
public final class LegendaAvisos {
    private EntregaCafe.Aviso tipo;
    private int numero, quantidade;
    private float alpha;

    public void atualizar(long tempo, List<EntregaCafe> entregas, boolean partidaIniciada) {
        tipo = null;
        numero = quantidade = 0;
        alpha = 0;
        if (!partidaIniciada || tempo < 0 || tempo >= RoteiroRecepcao.INICIO_PONTUACAO_MS) {
            return;
        }
        for (int i = 0; i < entregas.size(); ) {
            EntregaCafe primeira = entregas.get(i);
            int fimGrupo = i + 1;
            while (fimGrupo < entregas.size() && entregas.get(fimGrupo).pedido == primeira.pedido) {
                fimGrupo++;
            }
            EntregaCafe ultima = entregas.get(fimGrupo - 1);
            long fim = Math.min(primeira.tempoMs - 100, ultima.avisoMs + 400);
            if (tempo >= primeira.avisoMs && tempo < fim) {
                tipo = primeira.tipoAviso;
                quantidade = fimGrupo - i;
                for (int n = i; n < fimGrupo; n++) {
                    if (entregas.get(n).avisoMs <= tempo) {
                        numero++;
                    }
                }
                alpha =
                        Math.min(
                                1,
                                Math.min((tempo - primeira.avisoMs) / 120f, (fim - tempo) / 120f));
                return;
            }
            i = fimGrupo;
        }
    }

    public EntregaCafe.Aviso getTipo() {
        return tipo;
    }

    public int getNumero() {
        return numero;
    }

    public int getQuantidade() {
        return quantidade;
    }

    public float getAlpha() {
        return alpha;
    }
}
