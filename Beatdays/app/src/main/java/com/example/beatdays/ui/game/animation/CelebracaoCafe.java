package com.example.beatdays.ui.game.animation;

import com.example.beatdays.game.MotorRecepcao;
import com.example.beatdays.model.EntregaCafe;
import com.example.beatdays.model.Julgamento;

/** Agradecimento breve depois de guardar todo o pedido. Derivado do relógio, sem julgar toques. */
public final class CelebracaoCafe {
    public static final long DURACAO_MS = 400;

    private CelebracaoCafe() {}

    /** -1 fora da reação; de 0 a 1 durante o agradecimento. */
    public static float progresso(
            long tempo,
            MotorRecepcao motor,
            AtendimentoCafe atendimento,
            CoreografiaCafe.Cliente pose) {
        AtendimentoCafe.Pedido pedido = atendimento.getPedidoAtual();
        if (pedido == null
                || atendimento.getEtapa() != AtendimentoCafe.Etapa.DESPEDIDA
                || atendimento.getDesfecho() != AtendimentoCafe.Desfecho.COMPLETO
                || pose == CoreografiaCafe.Cliente.ERRA) {
            return -1;
        }
        long ultima = 0;
        for (EntregaCafe e : pedido.itens) {
            long instante;
            if (e.demonstracao) {
                instante = e.tempoMs;
            } else {
                Julgamento resultado = motor.getJulgamento(e.id);
                if (resultado == null || resultado == Julgamento.MISS) {
                    return -1;
                }
                instante = motor.getInstante(e.id);
            }
            ultima = Math.max(ultima, instante);
        }
        long inicio = ultima + AnimacaoCaptura.FIM_ALIMENTO_MS;
        if (tempo < inicio || tempo >= inicio + DURACAO_MS || tempo >= pedido.saidaMs) {
            return -1;
        }
        return (tempo - inicio) / (float) DURACAO_MS;
    }

    public static float impulso(float progresso) {
        return progresso < 0 ? 0 : (float) Math.sin(Math.min(1, progresso) * Math.PI);
    }
}
