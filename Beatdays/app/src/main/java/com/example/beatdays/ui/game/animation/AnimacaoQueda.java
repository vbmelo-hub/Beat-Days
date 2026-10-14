package com.example.beatdays.ui.game.animation;

import com.example.beatdays.game.MotorRitmo;

/** Tempos visuais de uma entrega perdida. Não altera nem registra julgamentos. */
public final class AnimacaoQueda {
    public static final long INICIO_MS = MotorRitmo.getJanelaGoodMs();
    public static final long IMPACTO_MS = INICIO_MS + 400;
    public static final long FIM_PARTICULAS_MS = IMPACTO_MS + 300;
    public static final long INICIO_SUMIR_MS = IMPACTO_MS + 180;
    public static final long FIM_MS = INICIO_SUMIR_MS + 400;

    private AnimacaoQueda() {}

    private static float limitar(float p) {
        return Math.max(0, Math.min(1, p));
    }

    public static float progresso(long depois) {
        return limitar((depois - INICIO_MS) / (float) (IMPACTO_MS - INICIO_MS));
    }

    public static float impacto(long depois) {
        return limitar((depois - IMPACTO_MS) / (float) (FIM_PARTICULAS_MS - IMPACTO_MS));
    }

    public static float alpha(long depois) {
        return 1 - limitar((depois - INICIO_SUMIR_MS) / (float) (FIM_MS - INICIO_SUMIR_MS));
    }
}
