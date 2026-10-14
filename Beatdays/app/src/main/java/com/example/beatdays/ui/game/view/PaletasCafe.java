package com.example.beatdays.ui.game.view;

import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;

import com.example.beatdays.ui.game.animation.CoreografiaCafe;

/** Filtros pré-calculados: não aloca matrizes em cada quadro da animação. */
final class PaletasCafe {
    private final ColorMatrixColorFilter[][][] filtros =
            new ColorMatrixColorFilter[CoreografiaCafe.Paleta.values().length]
                    [CoreografiaCafe.Paleta.values().length][16];

    PaletasCafe() {
        float[] identidade = new ColorMatrix().getArray();
        float[][] cores = {
            identidade,
            duotone(1.20f, .94f, .62f),
            duotone(1.36f, .85f, .95f),
            duotone(.42f, 1.05f, 1.20f),
            duotone(1.30f, 1.02f, .82f),
            duotone(1.05f, .78f, 1.30f),
            duotone(.78f, 1.16f, .85f),
            duotone(1f, 1f, 1f)
        };
        for (int p = 1; p < CoreografiaCafe.Paleta.CINZA.ordinal(); p++) {
            for (int i = 0; i < 20; i++) {
                cores[p][i] = identidade[i] + (cores[p][i] - identidade[i]) * .58f;
            }
        }
        float[] valores = new float[20];
        for (int origem = 0; origem < cores.length; origem++) {
            for (int destino = 0; destino < cores.length; destino++) {
                for (int nivel = 0; nivel < 16; nivel++) {
                    if (origem == 0 && destino == 0) {
                        continue;
                    }
                    if (origem == 0 && nivel == 0 || destino == 0 && nivel == 15) {
                        continue;
                    }
                    float mistura = nivel / 15f;
                    for (int i = 0; i < 20; i++) {
                        valores[i] =
                                cores[origem][i] + (cores[destino][i] - cores[origem][i]) * mistura;
                    }
                    filtros[origem][destino][nivel] =
                            new ColorMatrixColorFilter(new ColorMatrix(valores));
                }
            }
        }
    }

    private float[] duotone(float r, float g, float b) {
        return new float[] {
            .213f * r, .715f * r, .072f * r, 0, 0, .213f * g, .715f * g, .072f * g, 0, 0, .213f * b,
            .715f * b, .072f * b, 0, 0, 0, 0, 0, 1, 0
        };
    }

    ColorMatrixColorFilter obter(
            CoreografiaCafe.Paleta anterior, CoreografiaCafe.Paleta atual, float mistura) {
        return filtros[anterior.ordinal()][atual.ordinal()][
                Math.max(0, Math.min(15, Math.round(mistura * 15)))];
    }
}
