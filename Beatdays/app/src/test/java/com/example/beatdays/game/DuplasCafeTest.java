package com.example.beatdays.game;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.example.beatdays.model.EntregaCafe;
import com.example.beatdays.model.Julgamento;
import com.example.beatdays.model.ResultadoCafe;
import com.example.beatdays.ui.game.animation.CoreografiaCafe;

import org.junit.Test;

import java.util.List;

public class DuplasCafeTest {
    private EntregaCafe entrega(long tempo) {
        for (EntregaCafe e : RoteiroRecepcao.criarEntregas()) {
            if (e.tempoMs == tempo) {
                return e;
            }
        }
        throw new AssertionError("Captura não encontrada: " + tempo);
    }

    @Test
    public void duplasDeixamPausaERepetemMesmoIntervaloDosAvisos() {
        List<EntregaCafe> entregas = RoteiroRecepcao.criarEntregas();
        int duplas = 0;
        for (int i = 0; i + 1 < entregas.size(); i++) {
            EntregaCafe a = entregas.get(i), b = entregas.get(i + 1);
            if (a.quantidade != 2 || i > 0 && entregas.get(i - 1).pedido == a.pedido) {
                continue;
            }
            assertEquals(a.pedido, b.pedido);
            assertEquals(EntregaCafe.Aviso.RELOGIO, a.tipoAviso);
            assertEquals(EntregaCafe.Aviso.RELOGIO, b.tipoAviso);
            assertEquals(500, b.avisoMs - a.avisoMs);
            assertEquals(500, a.tempoMs - b.avisoMs);
            assertEquals(b.avisoMs - a.avisoMs, b.tempoMs - a.tempoMs);
            assertTrue(a.tempoMs - 200 > b.avisoMs + 280);
            duplas++;
        }
        assertEquals(4, duplas);
        assertEquals(2, entrega(13000).quantidade);
        assertEquals(13500, entrega(13500).tempoMs);
    }

    @Test
    public void demonstracaoTreinoEAberturaMantemAvisosCapturasEAlimentos() {
        int[] original = {1, 3, 2, 3, 1, 2, 3, 1, 3, 2, 3, 3, 1, 3, 2, 3, 3, 1};
        int preservados = 0;
        for (int p = 0; p < 8; p++) {
            int intervalo = p == 13 || p == 15 ? 250 : 500;
            int espera = original[p] == 3 && intervalo == 500 ? 1500 : 1000;
            for (int n = 0; n < original[p]; n++) {
                long aviso = 4000 + p * 4000L + n * intervalo;
                EntregaCafe e = entrega(aviso + espera);
                assertEquals(p, e.pedido);
                assertEquals(aviso, e.avisoMs);
                assertEquals(EntregaCafe.Alimento.values()[(p + n) % 4], e.alimento);
                preservados++;
            }
        }
        assertEquals(16, preservados);
    }

    @Test
    public void cadaAlimentoDaDuplaExigeUmaPressaoIndependente() {
        MotorRecepcao motor = new MotorRecepcao();
        motor.processarPerdidas(24000);
        MotorRecepcao.Resultado a = motor.pressionar(25000);
        motor.soltar();
        MotorRecepcao.Resultado b = motor.pressionar(25500);
        motor.soltar();
        assertEquals(Julgamento.PERFECT, a.julgamento);
        assertEquals(Julgamento.PERFECT, b.julgamento);
        assertNotEquals(a.entrega.id, b.entrega.id);
        assertEquals(2, a.entrega.quantidade);
        assertEquals(a.entrega.pedido, b.entrega.pedido);
        assertTrue(motor.processarPerdidas(25800).isEmpty());
    }

    @Test
    public void segurarPrimeiraCapturaNaoPegaSegundaAutomaticamente() {
        MotorRecepcao motor = new MotorRecepcao();
        motor.processarPerdidas(24000);
        assertEquals(Julgamento.PERFECT, motor.pressionar(25000).julgamento);
        assertNull(motor.pressionar(25500));
        assertNull(motor.getJulgamento(entrega(25500).id));
        assertEquals(1, motor.processarPerdidas(25701).size());
        assertEquals(Julgamento.MISS, motor.getJulgamento(entrega(25500).id));
    }

    @Test
    public void spamDurantePenalidadeNaoCapturaPrimeiroNemRenovaBloqueio() {
        MotorRecepcao motor = new MotorRecepcao();
        motor.processarPerdidas(24000);
        assertEquals(Julgamento.MISS, motor.pressionar(24790).julgamento);
        motor.soltar();
        for (long t = 24800; t <= 25330; t += 10) {
            assertNull(motor.pressionar(t));
            motor.soltar();
        }
        assertEquals(25340, motor.getBloqueadoAteMs());
        assertNull(motor.getJulgamento(entrega(25000).id));
        motor.processarPerdidas(25330);
        assertEquals(25340, motor.getBloqueadoAteMs());
        assertEquals(Julgamento.PERFECT, motor.pressionar(25500).julgamento);
    }

    @Test
    public void avisosECapturasDuplasNaoAtivamZoomNemCoresDosTrios() {
        MotorRecepcao motor = new MotorRecepcao();
        CoreografiaCafe cena = new CoreografiaCafe();
        for (long tempo : new long[] {12120, 12620, 13050, 13550}) {
            if (tempo >= 13000) {
                assertEquals(Julgamento.PERFECT, motor.pressionar(tempo).julgamento);
                motor.soltar();
            }
            cena.atualizar(tempo, motor, -10000, null);
            assertEquals(1, cena.getZoom(), 0);
            assertEquals(CoreografiaCafe.Paleta.NATURAL, cena.getPaleta());
        }
    }

    @Test
    public void novoTotalPontuavelGeraPerfeitoSoComTodasAsCapturas() {
        int total = 0;
        for (EntregaCafe e : RoteiroRecepcao.criarEntregas()) {
            if (e.tempoMs >= 20000) {
                total++;
            }
        }
        assertEquals(31, total);
        ResultadoCafe completo = new ResultadoCafe(31, 0, 0, 0, total);
        assertEquals(ResultadoCafe.Classificacao.PERFEITO, completo.getClassificacao());
        assertEquals(100, completo.getPrecisaoPercentual(), 0);
        assertNotEquals(
                ResultadoCafe.Classificacao.PERFEITO,
                new ResultadoCafe(28, 0, 0, 3, total).getClassificacao());
    }
}
