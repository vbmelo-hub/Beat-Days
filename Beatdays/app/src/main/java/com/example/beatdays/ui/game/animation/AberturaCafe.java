package com.example.beatdays.ui.game.animation;

/** A placa inicial acompanha o áudio e termina antes do primeiro aviso de 4000 ms. */
public final class AberturaCafe {
    public static final long FIM_MS = 3400;

    private AberturaCafe() {}

    public static float alpha(long tempo, boolean partidaIniciada) {
        if (!partidaIniciada || tempo < 0 || tempo >= FIM_MS) {
            return 0;
        }
        return Math.min(1, Math.min(tempo / 180f, (FIM_MS - tempo) / 400f));
    }
}
