package com.example.beatdays.ui.game.animation;

import com.example.beatdays.game.MotorRecepcao;
import com.example.beatdays.model.EntregaCafe;
import com.example.beatdays.model.Julgamento;

/** Reação breve ao atendimento atual. Apenas observa o motor; não guarda eventos atrasados. */
public final class ReacaoMika {
    public enum Tipo {
        NENHUMA,
        APROVA,
        SUSTO
    }

    public static final long APROVACAO_MS = 200;
    private Tipo tipo = Tipo.NENHUMA;
    private float progresso = -1;

    public void atualizar(
            long tempo,
            MotorRecepcao motor,
            AtendimentoCafe atendimento,
            CoreografiaCafe.Mika poseMika,
            CoreografiaCafe.Cliente poseCliente,
            float progressoCliente) {
        tipo = Tipo.NENHUMA;
        progresso = -1;
        AtendimentoCafe.Pedido pedido = atendimento.getPedidoAtual();
        if (poseCliente == CoreografiaCafe.Cliente.ERRA) {
            if (progressoCliente >= 0 && progressoCliente < 1) {
                tipo = Tipo.SUSTO;
                progresso = progressoCliente;
            }
            return;
        }
        if (pedido == null || poseMika != CoreografiaCafe.Mika.ESPERA) {
            return;
        }
        long ultimo = -1;
        for (EntregaCafe entrega : pedido.itens) {
            Julgamento j = motor.getJulgamento(entrega.id);
            long captura = motor.getInstante(entrega.id);
            if (!entrega.demonstracao
                    && j != null
                    && j != Julgamento.MISS
                    && captura >= 0
                    && captura <= tempo) {
                ultimo = Math.max(ultimo, captura);
            }
        }
        if (ultimo >= 0 && tempo - ultimo < APROVACAO_MS) {
            tipo = Tipo.APROVA;
            progresso = (tempo - ultimo) / (float) APROVACAO_MS;
        }
    }

    public Tipo getTipo() {
        return tipo;
    }

    public float getProgresso() {
        return progresso;
    }

    public float getAlpha() {
        return tipo == Tipo.NENHUMA ? 0 : (1 - progresso) * (1 - progresso);
    }

    public float getInclinacao() {
        return tipo == Tipo.NENHUMA
                ? 0
                : (tipo == Tipo.APROVA ? -1.4f : 1.8f) * (float) Math.sin(progresso * Math.PI);
    }
}
