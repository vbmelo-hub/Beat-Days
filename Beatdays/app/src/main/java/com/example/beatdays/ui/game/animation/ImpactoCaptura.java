package com.example.beatdays.ui.game.animation;

/** Pulso local do alimento; termina antes de guardar e não move a câmera. */
public final class ImpactoCaptura {
    public static final long DURACAO_MS = 120;

    private ImpactoCaptura() {}

    public static float escala(long desde) {
        if (desde < 0 || desde >= 100) {
            return 1;
        }
        return 1 + .05f * (float) Math.sin(desde * Math.PI / 100);
    }

    public static float alpha(long desde) {
        if (desde < 0 || desde >= DURACAO_MS) {
            return 0;
        }
        float restante = 1 - desde / (float) DURACAO_MS;
        return restante * restante;
    }
}
