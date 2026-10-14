package com.example.beatdays.ui.game.animation;

import com.example.beatdays.game.MotorRecepcao;
import com.example.beatdays.model.EntregaCafe;
import com.example.beatdays.model.Julgamento;

/** Feedback breve do resultado já registrado. Nunca captura, pontua ou altera o motor. */
public final class FeedbackCafe {
    public enum Direcao {
        NENHUMA,
        CEDO,
        TARDE
    }

    public static final long DURACAO_MS = 450;
    private Julgamento julgamento;
    private Direcao direcao = Direcao.NENHUMA;
    private boolean foraDoRitmo;
    private float alpha;

    public void atualizar(long tempo, MotorRecepcao motor) {
        julgamento = null;
        direcao = Direcao.NENHUMA;
        foraDoRitmo = false;
        alpha = 0;
        long ultimo = -1;
        EntregaCafe entrega = null;
        for (EntregaCafe e : motor.getEntregas()) {
            if (e.demonstracao || motor.getJulgamento(e.id) == null) {
                continue;
            }
            long instante = motor.getInstante(e.id);
            if (instante < 0 || instante > tempo || tempo - instante >= DURACAO_MS) {
                continue;
            }
            if (instante > ultimo
                    || instante == ultimo && (entrega == null || e.tempoMs > entrega.tempoMs)) {
                ultimo = instante;
                entrega = e;
                julgamento = motor.getJulgamento(e.id);
            }
        }
        long erro = motor.getUltimoErroToqueMs();
        if (erro >= 0 && erro >= ultimo && erro <= tempo && tempo - erro < DURACAO_MS) {
            ultimo = erro;
            entrega = null;
            julgamento = Julgamento.MISS;
            foraDoRitmo = true;
        }
        if (julgamento == null) {
            return;
        }
        alpha = Math.min(1, (DURACAO_MS - (tempo - ultimo)) / 100f);
        if (entrega != null
                && ultimo != entrega.tempoMs
                && (julgamento == Julgamento.GREAT || julgamento == Julgamento.GOOD)) {
            direcao = ultimo < entrega.tempoMs ? Direcao.CEDO : Direcao.TARDE;
        }
    }

    public Julgamento getJulgamento() {
        return julgamento;
    }

    public Direcao getDirecao() {
        return direcao;
    }

    public boolean isForaDoRitmo() {
        return foraDoRitmo;
    }

    public float getAlpha() {
        return alpha;
    }
}
