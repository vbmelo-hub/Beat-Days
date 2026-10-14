package com.example.beatdays.ui.game.animation;

/** Mistura curta entre poses de repouso; captura, erro e passos respondem imediatamente. */
public final class TransicaoPose {
    private int atual = -1, anterior = -1;
    private long inicio, ultimo = -1;
    private final float[] pesos;

    public TransicaoPose(int quantidade) {
        pesos = new float[quantidade];
    }

    public float[] atualizar(int alvo, long tempo, boolean imediata) {
        if (alvo != atual) {
            anterior = atual;
            atual = alvo;
            inicio = tempo;
            if (imediata || ultimo < 0 || tempo < ultimo || tempo - ultimo > 250) {
                anterior = -1;
            }
        }
        java.util.Arrays.fill(pesos, 0);
        float p = anterior < 0 ? 1 : Math.max(0, Math.min(1, (tempo - inicio) / 65f));
        if (atual >= 0) {
            pesos[atual] = p;
        }
        if (anterior >= 0 && p < 1) {
            pesos[anterior] = 1 - p;
        }
        ultimo = tempo;
        return pesos;
    }
}
