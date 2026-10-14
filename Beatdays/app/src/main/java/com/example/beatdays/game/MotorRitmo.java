package com.example.beatdays.game;

import com.example.beatdays.model.Batida;
import com.example.beatdays.model.Julgamento;

import java.util.ArrayList;
import java.util.List;

public final class MotorRitmo {
    private static final long JANELA_PERFECT = 70;
    private static final long JANELA_GREAT = 130;
    private static final long JANELA_GOOD = 200;
    private final List<Batida> batidas;

    public MotorRitmo() {
        batidas = new ArrayList<>();
    }

    public void carregarBatidas(List<Long> temposMs) {
        batidas.clear();
        for (Long tempo : temposMs) {
            batidas.add(new Batida(tempo));
        }
    }

    /** Julga somente a ação indicada, sem consumir outra nota de um gesto diferente. */
    public Julgamento registrarNoIndice(int indice, long tempoAtualMs) {
        if (indice < 0 || indice >= batidas.size()) {
            return Julgamento.MISS;
        }
        Batida batida = batidas.get(indice);
        long diferenca = Math.abs(tempoAtualMs - batida.getTempoMs());
        if (batida.isProcessada() || diferenca > JANELA_GOOD) {
            return Julgamento.MISS;
        }
        batida.marcarComoProcessada();
        if (diferenca <= JANELA_PERFECT) {
            return Julgamento.PERFECT;
        }
        if (diferenca <= JANELA_GREAT) {
            return Julgamento.GREAT;
        }
        return Julgamento.GOOD;
    }

    public boolean marcarComoPerdida(int indice) {
        if (indice < 0 || indice >= batidas.size() || batidas.get(indice).isProcessada()) {
            return false;
        }
        batidas.get(indice).marcarComoProcessada();
        return true;
    }

    public static long getJanelaGoodMs() {
        return JANELA_GOOD;
    }

    public boolean[] getEstadoBatidas() {
        boolean[] estado = new boolean[batidas.size()];
        for (int i = 0; i < batidas.size(); i++) {
            estado[i] = batidas.get(i).isProcessada();
        }
        return estado;
    }

    public void restaurarEstadoBatidas(boolean[] estado) {
        if (estado == null || estado.length != batidas.size()) {
            return;
        }
        for (int i = 0; i < estado.length; i++) {
            if (estado[i]) {
                batidas.get(i).marcarComoProcessada();
            }
        }
    }
}
