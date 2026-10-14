package com.example.beatdays.ui.game.animation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ImpactoCapturaTest {
    @Test
    public void nuncaApareceAntesDoToqueEAcabaAntesDeGuardar() {
        assertEquals(0, ImpactoCaptura.alpha(-1), 0);
        assertEquals(1, ImpactoCaptura.escala(-1), 0);
        assertTrue(ImpactoCaptura.DURACAO_MS < AnimacaoCaptura.INICIO_GUARDAR_MS);
        for (long t : new long[] {120, 140, 230, 250, 320}) {
            assertEquals(0, ImpactoCaptura.alpha(t), 0);
            assertEquals(1, ImpactoCaptura.escala(t), 0);
        }
    }

    @Test
    public void pulsoLimitadoNaoProvocaSaltosOuCrescimentoContínuo() {
        assertEquals(1, ImpactoCaptura.escala(0), 0);
        assertEquals(1, ImpactoCaptura.escala(100), 0);
        float anterior = 1;
        for (int t = 0; t <= 120; t++) {
            float escala = ImpactoCaptura.escala(t);
            assertTrue(escala >= 1 && escala <= 1.050001f);
            assertTrue(Math.abs(escala - anterior) < .002f);
            anterior = escala;
        }
    }

    @Test
    public void brilhoSomeMonotonicamenteENaoDependeDeEstadoAoPausar() {
        float anterior = 1;
        for (int t = 0; t <= 120; t++) {
            float alpha = ImpactoCaptura.alpha(t);
            assertTrue(alpha >= 0 && alpha <= anterior);
            anterior = alpha;
        }
        float escala = ImpactoCaptura.escala(50), alpha = ImpactoCaptura.alpha(50);
        ImpactoCaptura.escala(250);
        ImpactoCaptura.alpha(250);
        assertEquals(escala, ImpactoCaptura.escala(50), 0);
        assertEquals(alpha, ImpactoCaptura.alpha(50), 0);
    }
}
