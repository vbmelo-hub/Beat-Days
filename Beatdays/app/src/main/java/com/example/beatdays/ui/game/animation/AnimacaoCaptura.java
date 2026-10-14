package com.example.beatdays.ui.game.animation;

/** Tempos compartilhados entre as mãos do cliente e o alimento no relógio da música. */
public final class AnimacaoCaptura {
    public static final long INICIO_GUARDAR_MS = 140, FIM_GUARDAR_MS = 230, FIM_ALIMENTO_MS = 320;

    private AnimacaoCaptura() {}

    public static boolean estaPegando(long desde) {
        return desde >= 0 && desde < INICIO_GUARDAR_MS;
    }

    public static boolean estaGuardando(long desde) {
        return desde >= INICIO_GUARDAR_MS && desde < FIM_GUARDAR_MS;
    }

    public static float progressoGuardar(long desde) {
        return suave((desde - INICIO_GUARDAR_MS) / (float) (FIM_GUARDAR_MS - INICIO_GUARDAR_MS));
    }

    public static float progressoSumir(long desde) {
        return suave((desde - FIM_GUARDAR_MS) / (float) (FIM_ALIMENTO_MS - FIM_GUARDAR_MS));
    }

    private static float suave(float p) {
        p = Math.max(0, Math.min(1, p));
        return p * p * (3 - 2 * p);
    }
}
