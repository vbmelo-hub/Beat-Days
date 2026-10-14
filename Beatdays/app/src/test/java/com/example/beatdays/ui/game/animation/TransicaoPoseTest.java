package com.example.beatdays.ui.game.animation;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class TransicaoPoseTest {
    @Test
    public void repousoMisturaPosesSemSomarMaisDeUmPersonagem() {
        TransicaoPose transicao = new TransicaoPose(2);
        transicao.atualizar(0, 0, true);
        transicao.atualizar(1, 100, false);
        float[] meio = transicao.atualizar(1, 132, false);
        assertEquals(1, meio[0] + meio[1], .0001);
        assertEquals(32 / 65f, meio[1], .0001);
        float[] fim = transicao.atualizar(1, 165, false);
        assertEquals(0, fim[0], 0);
        assertEquals(1, fim[1], 0);
    }

    @Test
    public void capturaEErroTrocamSemEsperarMistura() {
        TransicaoPose transicao = new TransicaoPose(3);
        transicao.atualizar(0, 0, true);
        transicao.atualizar(1, 100, false);
        float[] corte = transicao.atualizar(2, 110, true);
        assertEquals(0, corte[0], 0);
        assertEquals(0, corte[1], 0);
        assertEquals(1, corte[2], 0);
    }

    @Test
    public void saltoDeTempoNaoMantemPoseDeQuadroAntigo() {
        TransicaoPose transicao = new TransicaoPose(2);
        transicao.atualizar(0, 0, true);
        assertEquals(1, transicao.atualizar(1, 1000, false)[1], 0);
    }
}
