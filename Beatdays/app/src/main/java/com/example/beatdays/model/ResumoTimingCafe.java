package com.example.beatdays.model;

import com.example.beatdays.game.MotorRecepcao;
import com.example.beatdays.game.RoteiroRecepcao;

/** Conta somente acertos imprecisos da fase valendo, sem alterar a partida. */
public final class ResumoTimingCafe {
    public enum Tendencia {
        CEDO,
        TARDE,
        EQUILIBRADO
    }

    private int cedo, tarde;

    public ResumoTimingCafe(MotorRecepcao motor) {
        for (EntregaCafe entrega : motor.getEntregas()) {
            if (entrega.demonstracao || entrega.tempoMs < RoteiroRecepcao.INICIO_PONTUACAO_MS) {
                continue;
            }
            Julgamento julgamento = motor.getJulgamento(entrega.id);
            if (julgamento != Julgamento.GREAT && julgamento != Julgamento.GOOD) {
                continue;
            }
            long instante = motor.getInstante(entrega.id);
            if (instante < 0 || instante == entrega.tempoMs) {
                continue;
            }
            if (instante < entrega.tempoMs) {
                cedo++;
            } else {
                tarde++;
            }
        }
    }

    public int getCedo() {
        return cedo;
    }

    public int getTarde() {
        return tarde;
    }

    public boolean temDesvios() {
        return cedo + tarde > 0;
    }

    public Tendencia getTendencia() {
        return cedo > tarde
                ? Tendencia.CEDO
                : tarde > cedo ? Tendencia.TARDE : Tendencia.EQUILIBRADO;
    }
}
