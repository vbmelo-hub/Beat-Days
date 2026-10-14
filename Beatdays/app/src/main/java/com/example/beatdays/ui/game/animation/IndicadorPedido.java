package com.example.beatdays.ui.game.animation;

import com.example.beatdays.model.EntregaCafe;

/** Pulsos locais do pedido, reconstruídos pelo relógio do áudio e pela captura registrada. */
public final class IndicadorPedido {
    public static final long CONFIRMACAO_MS = 180;

    private IndicadorPedido() {}

    private static float pulso(long tempo, long inicio, long duracao) {
        long depois = tempo - inicio;
        if (inicio < 0 || depois < 0 || depois >= duracao) {
            return 0;
        }
        float p = 1 - depois / (float) duracao;
        return p * p;
    }

    public static float aviso(long tempo, EntregaCafe entrega, AtendimentoCafe.Situacao situacao) {
        if (situacao != AtendimentoCafe.Situacao.PENDENTE) {
            return 0;
        }
        return pulso(
                tempo, entrega.avisoMs, entrega.tipoAviso == EntregaCafe.Aviso.TIMER ? 160 : 300);
    }

    public static float confirmacao(long tempo, long captura, AtendimentoCafe.Situacao situacao) {
        return situacao == AtendimentoCafe.Situacao.RECEBIDO
                ? pulso(tempo, captura, CONFIRMACAO_MS)
                : 0;
    }

    public static float escala(float aviso, float confirmacao) {
        return 1
                + .08f * Math.max(0, Math.min(1, aviso))
                + .10f * Math.max(0, Math.min(1, confirmacao));
    }
}
