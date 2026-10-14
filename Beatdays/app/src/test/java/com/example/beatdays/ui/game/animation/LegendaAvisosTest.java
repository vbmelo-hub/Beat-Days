package com.example.beatdays.ui.game.animation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.example.beatdays.game.RoteiroRecepcao;
import com.example.beatdays.model.EntregaCafe;

import org.junit.Test;

public class LegendaAvisosTest {
    private final LegendaAvisos legenda = new LegendaAvisos();

    private void atualizar(long tempo) {
        legenda.atualizar(tempo, RoteiroRecepcao.criarEntregas(), true);
    }

    @Test
    public void fontesEContagensSeguemAvisosReaisSemAvancarDuranteIntervalo() {
        atualizar(4200);
        assertEquals(EntregaCafe.Aviso.MIKA, legenda.getTipo());
        assertEquals(1, legenda.getQuantidade());
        atualizar(12200);
        assertEquals(EntregaCafe.Aviso.RELOGIO, legenda.getTipo());
        assertEquals(1, legenda.getNumero());
        atualizar(12490);
        assertEquals(1, legenda.getNumero());
        atualizar(12500);
        assertEquals(2, legenda.getNumero());
        for (int n = 0; n < 3; n++) {
            atualizar(16200 + n * 500);
            assertEquals(EntregaCafe.Aviso.TIMER, legenda.getTipo());
            assertEquals(n + 1, legenda.getNumero());
            assertEquals(3, legenda.getQuantidade());
        }
    }

    @Test
    public void saiAntesDaRespostaENaoSurgeNoMenuOuNaFasePontuada() {
        for (long t : new long[] {0, 3999, 4400, 12900, 13000, 17400, 17500, 20000, 24050, 67050}) {
            atualizar(t);
            assertNull("Tempo: " + t, legenda.getTipo());
            assertEquals(0, legenda.getAlpha(), 0);
        }
        legenda.atualizar(12200, RoteiroRecepcao.criarEntregas(), false);
        assertNull(legenda.getTipo());
    }

    @Test
    public void pausaRepeteMesmoEstadoERetornoDaLicaoRecomecaContagem() {
        atualizar(12600);
        float alpha = legenda.getAlpha();
        assertEquals(2, legenda.getNumero());
        LegendaAvisos restaurada = new LegendaAvisos();
        restaurada.atualizar(12600, RoteiroRecepcao.criarEntregas(), true);
        assertEquals(legenda.getTipo(), restaurada.getTipo());
        assertEquals(legenda.getNumero(), restaurada.getNumero());
        assertEquals(alpha, restaurada.getAlpha(), 0);
        atualizar(11600);
        assertNull(legenda.getTipo());
        atualizar(12200);
        assertEquals(1, legenda.getNumero());
    }

    @Test
    public void desenhoRespeitaEspacoEntreUltimoAvisoEPrimeiraCaptura() {
        java.util.List<EntregaCafe> itens =
                java.util.Arrays.asList(
                        new EntregaCafe(0, 0, 3, EntregaCafe.Alimento.CAFE, 1000, 2000),
                        new EntregaCafe(1, 0, 3, EntregaCafe.Alimento.CROISSANT, 1250, 2250),
                        new EntregaCafe(2, 0, 3, EntregaCafe.Alimento.PANQUECA, 1500, 2500));
        legenda.atualizar(1710, itens, true);
        assertEquals(3, legenda.getNumero());
        assertTrue(legenda.getAlpha() > 0);
        legenda.atualizar(1900, itens, true);
        assertNull(legenda.getTipo());
        assertEquals(2000, itens.get(0).tempoMs);
    }
}
