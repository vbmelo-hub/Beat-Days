package com.example.beatdays.model;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.example.beatdays.game.MotorRecepcao;
import com.example.beatdays.game.RoteiroRecepcao;

import org.junit.Test;

public class ResumoAtendimentoTest {
    @Test
    public void demoETreinoNaoContamEPedidosSemResultadoFicamPendentes() {
        MotorRecepcao motor = new MotorRecepcao();
        motor.pressionar(13000);
        motor.soltar();
        motor.processarPerdidas(19000);
        ResumoAtendimento resumo = new ResumoAtendimento(motor);
        assertEquals(14, resumo.getTotal());
        assertEquals(14, resumo.getPendentes());
        assertEquals(0, resumo.getCompletos());
        assertEquals(0, resumo.getPerdidos());
        assertEquals(0, resumo.getParciais());
    }

    @Test
    public void todosAlimentosAcertadosComQualquerQualidadeCompletamOs14Pedidos() {
        MotorRecepcao motor = new MotorRecepcao();
        for (EntregaCafe e : motor.getEntregas()) {
            if (e.tempoMs >= RoteiroRecepcao.INICIO_PONTUACAO_MS) {
                long atraso = e.quantidade == 1 ? 180 : e.id % 2 == 0 ? 0 : 90;
                assertNotNull(motor.pressionar(e.tempoMs + atraso));
                motor.soltar();
            }
        }
        ResumoAtendimento resumo = new ResumoAtendimento(motor);
        assertEquals(14, resumo.getCompletos());
        assertEquals(0, resumo.getParciais());
        assertEquals(0, resumo.getPerdidos());
        assertEquals(0, resumo.getPendentes());
    }

    @Test
    public void pedidosCompletosParciaisEPerdidosSaoDistinguidos() {
        MotorRecepcao motor = new MotorRecepcao();
        motor.pressionar(21000);
        motor.soltar();
        motor.pressionar(25000);
        motor.soltar();
        motor.processarPerdidas(80000);
        ResumoAtendimento resumo = new ResumoAtendimento(motor);
        assertEquals(1, resumo.getCompletos());
        assertEquals(1, resumo.getParciais());
        assertEquals(12, resumo.getPerdidos());
        assertEquals(0, resumo.getPendentes());
    }

    @Test
    public void perderTodosNaoAfirmaQueUmPedidoFoiCompleto() {
        MotorRecepcao motor = new MotorRecepcao();
        motor.processarPerdidas(80000);
        ResumoAtendimento resumo = new ResumoAtendimento(motor);
        assertEquals(0, resumo.getCompletos());
        assertEquals(14, resumo.getPerdidos());
    }

    @Test
    public void toqueForaDaJanelaNaoDuplicarPedidoNemTirarAlimentoRecebido() {
        MotorRecepcao motor = new MotorRecepcao();
        motor.pressionar(21000);
        motor.soltar();
        motor.pressionar(22000);
        motor.soltar();
        ResumoAtendimento resumo = new ResumoAtendimento(motor);
        assertEquals(14, resumo.getTotal());
        assertEquals(1, resumo.getCompletos());
        assertEquals(13, resumo.getPendentes());
    }

    @Test
    public void restaurarEReiniciarReconstituemResumoSemContadoresDuplicados() {
        MotorRecepcao antes = new MotorRecepcao();
        antes.pressionar(21000);
        antes.soltar();
        antes.pressionar(25000);
        antes.soltar();
        antes.processarPerdidas(80000);
        MotorRecepcao depois = new MotorRecepcao();
        depois.restaurar(antes.salvar());
        depois.restaurarInstantes(antes.salvarInstantes());
        ResumoAtendimento resumo = new ResumoAtendimento(depois);
        assertEquals(1, resumo.getCompletos());
        assertEquals(1, resumo.getParciais());
        assertEquals(12, resumo.getPerdidos());
        int[] julgamentos = depois.salvar();
        new ResumoAtendimento(depois);
        assertArrayEquals(julgamentos, depois.salvar());
        depois.reiniciar();
        resumo = new ResumoAtendimento(depois);
        assertEquals(0, resumo.getCompletos());
        assertEquals(14, resumo.getPendentes());
    }
}
