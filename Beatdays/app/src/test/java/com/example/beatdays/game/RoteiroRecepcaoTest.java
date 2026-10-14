package com.example.beatdays.game;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.example.beatdays.model.EntregaCafe;
import com.example.beatdays.model.Julgamento;
import com.example.beatdays.ui.game.animation.AtendimentoCafe;
import com.example.beatdays.ui.game.animation.CoreografiaCafe;

import org.junit.Test;

import java.util.HashSet;
import java.util.List;

public class RoteiroRecepcaoTest {
    @Test
    public void frasesVariamEHaRespiroAntesDaPreparacaoParaOClimax() {
        List<AtendimentoCafe.Pedido> pedidos = new AtendimentoCafe().getPedidos();
        HashSet<Long> intervalos = new HashSet<>();
        for (int i = 8; i < pedidos.size(); i++) {
            intervalos.add(pedidos.get(i).chegadaMs - pedidos.get(i - 1).chegadaMs);
        }
        assertTrue(intervalos.size() >= 4);
        assertTrue(pedidos.get(12).entradaMs - pedidos.get(11).fimMs >= 2000);
        assertTrue(pedidos.get(17).entradaMs - pedidos.get(16).fimMs >= 1500);
    }

    @Test
    public void clientesVoosERespostasNaoInvademPedidoSeguinte() {
        List<AtendimentoCafe.Pedido> pedidos = new AtendimentoCafe().getPedidos();
        long fim = -1;
        for (AtendimentoCafe.Pedido pedido : pedidos) {
            assertTrue(pedido.entradaMs >= fim);
            for (EntregaCafe e : pedido.itens) {
                assertTrue(e.lancamentoMs >= pedido.chegadaMs);
            }
            fim = pedido.fimMs;
        }
        assertTrue(fim < RoteiroRecepcao.FIM_PEDIDOS_MS);
    }

    @Test
    public void todosOsAvisosAntecedemRespostaERepetemOIntervalo() {
        for (AtendimentoCafe.Pedido pedido : new AtendimentoCafe().getPedidos()) {
            List<EntregaCafe> itens = pedido.itens;
            assertEquals(itens.size(), itens.get(0).quantidade);
            if (itens.size() == 1) {
                assertEquals(1000, itens.get(0).tempoMs - itens.get(0).avisoMs);
                continue;
            }
            assertEquals(500, itens.get(0).tempoMs - itens.get(itens.size() - 1).avisoMs);
            for (int i = 1; i < itens.size(); i++) {
                assertEquals(
                        itens.get(i).avisoMs - itens.get(i - 1).avisoMs,
                        itens.get(i).tempoMs - itens.get(i - 1).tempoMs);
            }
        }
    }

    @Test
    public void climaxTemDoisTriosRapidosETerminaComPedidoSimples() {
        List<AtendimentoCafe.Pedido> pedidos = new AtendimentoCafe().getPedidos();
        for (int p : new int[] {15, 16}) {
            List<EntregaCafe> itens = pedidos.get(p).itens;
            assertEquals(3, itens.size());
            assertEquals(250, itens.get(1).avisoMs - itens.get(0).avisoMs);
        }
        assertEquals(1, pedidos.get(17).itens.size());
        assertEquals(72500, pedidos.get(17).itens.get(0).tempoMs);
    }

    @Test
    public void novoTrioMantemZoomAcumuladoCapturaFisicaEPausaRestaurada() {
        MotorRecepcao motor = new MotorRecepcao();
        CoreografiaCafe cena = new CoreografiaCafe();
        for (int n = 0; n < 3; n++) {
            cena.atualizar(67100 + n * 250, motor, -10000, null);
            assertEquals(1 + (n + 1) * .03f, cena.getZoom(), .00001f);
        }
        assertNull(motor.getJulgamento(id(68000)));
        for (int n = 0; n < 3; n++) {
            assertEquals(Julgamento.PERFECT, motor.pressionar(68000 + n * 250).julgamento);
            motor.soltar();
            cena.atualizar(68100 + n * 250, motor, -10000, null);
            assertEquals(1 + (2 - n) * .03f, cena.getZoom(), .00001f);
        }
        MotorRecepcao salvo = new MotorRecepcao();
        salvo.restaurar(motor.salvar());
        salvo.restaurarInstantes(motor.salvarInstantes());
        salvo.restaurarToquesTrio(motor.salvarToquesTrio());
        CoreografiaCafe restaurada = new CoreografiaCafe();
        restaurada.atualizar(68600, salvo, -10000, null);
        assertEquals(cena.getZoom(), restaurada.getZoom(), 0);
        assertEquals(cena.getPaleta(), restaurada.getPaleta());
        motor.reiniciar();
        assertNull(motor.getJulgamento(id(68000)));
    }

    private int id(long tempo) {
        for (EntregaCafe e : RoteiroRecepcao.criarEntregas()) {
            if (e.tempoMs == tempo) {
                return e.id;
            }
        }
        throw new AssertionError("Captura ausente: " + tempo);
    }
}
