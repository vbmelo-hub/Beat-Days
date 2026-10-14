package com.example.beatdays.ui.game.animation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.example.beatdays.game.MotorRecepcao;
import com.example.beatdays.game.RoteiroRecepcao;
import com.example.beatdays.model.EntregaCafe;
import com.example.beatdays.model.Julgamento;
import com.example.beatdays.ui.game.animation.AtendimentoCafe.Situacao;

import org.junit.Test;

public class IndicadorPedidoTest {
    @Test
    public void avisoAcompanhaDuracaoDoSomSemConfirmarRecebimento() {
        for (EntregaCafe e : RoteiroRecepcao.criarEntregas()) {
            long duracao = e.tipoAviso == EntregaCafe.Aviso.TIMER ? 160 : 300;
            assertEquals(0, IndicadorPedido.aviso(e.avisoMs - 1, e, Situacao.PENDENTE), 0);
            assertEquals(1, IndicadorPedido.aviso(e.avisoMs, e, Situacao.PENDENTE), 0);
            assertTrue(IndicadorPedido.aviso(e.avisoMs + duracao - 1, e, Situacao.PENDENTE) > 0);
            assertEquals(0, IndicadorPedido.aviso(e.avisoMs + duracao, e, Situacao.PENDENTE), 0);
            assertEquals(0, IndicadorPedido.aviso(e.avisoMs, e, Situacao.RECEBIDO), 0);
        }
    }

    @Test
    public void semEntradaEMissNaoProduzemConfirmacao() {
        MotorRecepcao motor = new MotorRecepcao();
        EntregaCafe e =
                motor.getEntregas().stream().filter(a -> a.tempoMs == 21000).findFirst().get();
        assertEquals(
                0,
                IndicadorPedido.confirmacao(21000, motor.getInstante(e.id), Situacao.PENDENTE),
                0);
        motor.processarPerdidas(21250);
        assertEquals(Julgamento.MISS, motor.getJulgamento(e.id));
        assertEquals(
                0,
                IndicadorPedido.confirmacao(21250, motor.getInstante(e.id), Situacao.PERDIDO),
                0);
    }

    @Test
    public void acertoCedoUsaInstanteFisicoEFadeTerminaAntesDoProximoItemRapido() {
        MotorRecepcao motor = new MotorRecepcao();
        MotorRecepcao.Resultado r = motor.pressionar(20900);
        assertEquals(Julgamento.GREAT, r.julgamento);
        long t = motor.getInstante(r.entrega.id);
        assertEquals(0, IndicadorPedido.confirmacao(t - 1, t, Situacao.RECEBIDO), 0);
        assertEquals(1, IndicadorPedido.confirmacao(t, t, Situacao.RECEBIDO), 0);
        assertTrue(IndicadorPedido.confirmacao(t + 90, t, Situacao.RECEBIDO) < .5f);
        assertEquals(0, IndicadorPedido.confirmacao(t + 180, t, Situacao.RECEBIDO), 0);
        assertEquals(1, IndicadorPedido.escala(0, 0), 0);
        assertTrue(IndicadorPedido.escala(0, 1) <= 1.101f);
    }

    @Test
    public void trioRapidoDestacaCadaAvisoEAcertoSeparadamente() {
        MotorRecepcao motor = new MotorRecepcao();
        java.util.List<EntregaCafe> itens = new java.util.ArrayList<>();
        for (EntregaCafe e : motor.getEntregas()) {
            if (e.pedido == 16) {
                itens.add(e);
            }
        }
        for (int n = 0; n < 3; n++) {
            int destacados = 0;
            for (EntregaCafe e : itens) {
                if (IndicadorPedido.aviso(itens.get(n).avisoMs, e, Situacao.PENDENTE) > 0) {
                    destacados++;
                }
            }
            assertEquals(1, destacados);
            long t = itens.get(n).tempoMs + 30;
            assertEquals(Julgamento.PERFECT, motor.pressionar(t).julgamento);
            motor.soltar();
            int confirmados = 0;
            for (EntregaCafe e : itens) {
                if (motor.getJulgamento(e.id) != null
                        && IndicadorPedido.confirmacao(
                                        t, motor.getInstante(e.id), Situacao.RECEBIDO)
                                > 0) {
                    confirmados++;
                }
            }
            assertEquals(1, confirmados);
        }
    }

    @Test
    public void pausaRecriaMesmoPulsoERetornoDoTreinoRemoveConfirmacaoAntiga() {
        MotorRecepcao motor = new MotorRecepcao();
        MotorRecepcao.Resultado r = motor.pressionar(13530);
        motor.soltar();
        assertEquals(Julgamento.PERFECT, r.julgamento);
        float pulso =
                IndicadorPedido.confirmacao(
                        13580, motor.getInstante(r.entrega.id), Situacao.RECEBIDO);
        MotorRecepcao restaurado = new MotorRecepcao();
        restaurado.restaurar(motor.salvar());
        restaurado.restaurarInstantes(motor.salvarInstantes());
        assertEquals(Julgamento.PERFECT, restaurado.getJulgamento(r.entrega.id));
        assertEquals(
                pulso,
                IndicadorPedido.confirmacao(
                        13580, restaurado.getInstante(r.entrega.id), Situacao.RECEBIDO),
                0);
        assertTrue(motor.reiniciarPedidoTreino(2));
        assertEquals(
                0,
                IndicadorPedido.confirmacao(
                        13580, motor.getInstante(r.entrega.id), Situacao.PENDENTE),
                0);
    }
}
