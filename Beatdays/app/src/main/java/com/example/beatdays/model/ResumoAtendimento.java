package com.example.beatdays.model;

import com.example.beatdays.game.MotorRecepcao;
import com.example.beatdays.game.RoteiroRecepcao;

/** Reconstitui o serviço da partida a partir das entregas já julgadas, sem conceder pontos. */
public final class ResumoAtendimento {
    private int total, completos, parciais, perdidos, pendentes;

    public ResumoAtendimento(MotorRecepcao motor) {
        int grupo = -1, recebidos = 0, falhas = 0, aguardando = 0;
        for (EntregaCafe e : motor.getEntregas()) {
            if (e.demonstracao || e.tempoMs < RoteiroRecepcao.INICIO_PONTUACAO_MS) {
                continue;
            }
            if (grupo >= 0 && grupo != e.pedido) {
                registrar(recebidos, falhas, aguardando);
                recebidos = falhas = aguardando = 0;
            }
            grupo = e.pedido;
            Julgamento j = motor.getJulgamento(e.id);
            if (j == null) {
                aguardando++;
            } else if (j == Julgamento.MISS) {
                falhas++;
            } else {
                recebidos++;
            }
        }
        if (grupo >= 0) {
            registrar(recebidos, falhas, aguardando);
        }
    }

    private void registrar(int recebidos, int falhas, int aguardando) {
        total++;
        if (aguardando > 0) {
            pendentes++;
        } else if (falhas == 0) {
            completos++;
        } else if (recebidos == 0) {
            perdidos++;
        } else {
            parciais++;
        }
    }

    public int getTotal() {
        return total;
    }

    public int getCompletos() {
        return completos;
    }

    public int getParciais() {
        return parciais;
    }

    public int getPerdidos() {
        return perdidos;
    }

    public int getPendentes() {
        return pendentes;
    }
}
