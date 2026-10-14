package com.example.beatdays.ui.game.animation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.example.beatdays.game.MotorRecepcao;
import com.example.beatdays.model.EntregaCafe;
import com.example.beatdays.model.Julgamento;

import org.junit.Test;

public class AtendimentoCafeTest {
    private final AtendimentoCafe atendimento = new AtendimentoCafe();
    private final MotorRecepcao motor = new MotorRecepcao();

    private void atualizar(long t) {
        atendimento.atualizar(t, motor);
    }

    private void pegar(long t) {
        assertNotEquals(Julgamento.MISS, motor.pressionar(t).julgamento);
        motor.soltar();
    }

    @Test
    public void cadaPedidoTemUmClienteETransicoesSemSobreposicao() {
        assertEquals(18, atendimento.getPedidos().size());
        long fim = -1;
        int alimentos = 0;
        for (AtendimentoCafe.Pedido p : atendimento.getPedidos()) {
            assertTrue(p.entradaMs >= fim);
            atualizar(p.entradaMs);
            assertEquals(p.id, atendimento.getPedidoAtual().id);
            assertEquals(AtendimentoCafe.Etapa.CHEGANDO, atendimento.getEtapa());
            assertEquals(-1, atendimento.getDeslocamento(), 0);
            atualizar(p.chegadaMs);
            assertEquals(0, atendimento.getDeslocamento(), 0);
            atualizar(p.saidaMs);
            assertEquals(AtendimentoCafe.Etapa.SAINDO, atendimento.getEtapa());
            atualizar(p.fimMs);
            assertEquals(AtendimentoCafe.Etapa.AUSENTE, atendimento.getEtapa());
            assertNull(atendimento.getPedidoAtual());
            alimentos += p.itens.size();
            fim = p.fimMs;
        }
        assertEquals(40, alimentos);
        assertTrue(fim < 75000);
    }

    @Test
    public void balcaoMostraDoisItensEAvisoNuncaContaComoRecebido() {
        atualizar(24620);
        assertEquals(5, atendimento.getPedidoAtual().id);
        assertEquals(2, atendimento.getPedidoAtual().itens.size());
        assertEquals(0, atendimento.getRecebidos());
        assertEquals(AtendimentoCafe.Desfecho.PENDENTE, atendimento.getDesfecho());
        assertEquals(AtendimentoCafe.Situacao.PENDENTE, atendimento.getSituacao(0));
        assertEquals(AtendimentoCafe.Situacao.PENDENTE, atendimento.getSituacao(1));
        for (int estado : motor.salvar()) {
            assertEquals(0, estado);
        }
    }

    @Test
    public void completarPedidoExigeTodasAsCapturasEReconstroiAposPausa() {
        pegar(25050);
        atualizar(25050);
        assertEquals(1, atendimento.getRecebidos());
        assertEquals(AtendimentoCafe.Desfecho.PENDENTE, atendimento.getDesfecho());
        pegar(25550);
        atualizar(25600);
        assertEquals(2, atendimento.getRecebidos());
        assertEquals(AtendimentoCafe.Desfecho.COMPLETO, atendimento.getDesfecho());
        assertEquals(AtendimentoCafe.Etapa.DESPEDIDA, atendimento.getEtapa());
        MotorRecepcao restaurado = new MotorRecepcao();
        restaurado.restaurar(motor.salvar());
        atendimento.atualizar(25600, restaurado);
        assertEquals(2, atendimento.getRecebidos());
        assertEquals(AtendimentoCafe.Desfecho.COMPLETO, atendimento.getDesfecho());
        atendimento.atualizar(25800, restaurado);
        assertEquals(AtendimentoCafe.Desfecho.COMPLETO, atendimento.getDesfecho());
        assertEquals(0, atendimento.getDeslocamento(), 0);
    }

    @Test
    public void capturaParcialEPedidoPerdidoPossuemDesfechosDiferentes() {
        pegar(25050);
        motor.processarPerdidas(25710);
        atualizar(25720);
        assertEquals(1, atendimento.getRecebidos());
        assertEquals(1, atendimento.getPerdidos());
        assertEquals(AtendimentoCafe.Desfecho.PARCIAL, atendimento.getDesfecho());
        assertEquals(AtendimentoCafe.Situacao.PERDIDO, atendimento.getSituacao(1));
        motor.processarPerdidas(33210);
        atualizar(33220);
        assertEquals(0, atendimento.getRecebidos());
        assertEquals(1, atendimento.getPerdidos());
        assertEquals(AtendimentoCafe.Desfecho.PERDIDO, atendimento.getDesfecho());
    }

    @Test
    public void demonstracaoIlustraRecepcaoSemInventarJulgamentos() {
        atualizar(4900);
        assertEquals(0, atendimento.getRecebidos());
        atualizar(5050);
        assertEquals(1, atendimento.getRecebidos());
        assertEquals(AtendimentoCafe.Desfecho.COMPLETO, atendimento.getDesfecho());
        for (EntregaCafe e : motor.getEntregas()) {
            assertNull(motor.getJulgamento(e.id));
        }
    }

    @Test
    public void trioRapidoMantemClienteAteAcabarRespostaEReacao() {
        pegar(57050);
        pegar(57300);
        pegar(57550);
        atualizar(57600);
        assertEquals(3, atendimento.getRecebidos());
        assertEquals(AtendimentoCafe.Desfecho.COMPLETO, atendimento.getDesfecho());
        assertEquals(0, atendimento.getDeslocamento(), 0);
        atualizar(58249);
        assertEquals(0, atendimento.getDeslocamento(), 0);
        atualizar(58400);
        assertTrue(atendimento.getDeslocamento() < 0);
        atualizar(58550);
        assertNull(atendimento.getPedidoAtual());
        atualizar(59600);
        assertEquals(14, atendimento.getPedidoAtual().id);
        assertEquals(0, atendimento.getRecebidos());
        assertEquals(AtendimentoCafe.Desfecho.PENDENTE, atendimento.getDesfecho());
    }

    @Test
    public void resultadoFuturoNaoApareceAoReconstruirMomentoAnterior() {
        pegar(25050);
        pegar(25550);
        atualizar(24900);
        assertEquals(0, atendimento.getRecebidos());
        assertEquals(AtendimentoCafe.Desfecho.PENDENTE, atendimento.getDesfecho());
    }
}
