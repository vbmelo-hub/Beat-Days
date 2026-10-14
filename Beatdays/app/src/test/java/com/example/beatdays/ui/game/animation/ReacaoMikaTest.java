package com.example.beatdays.ui.game.animation;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.example.beatdays.game.MotorRecepcao;
import com.example.beatdays.model.Julgamento;

import org.junit.Test;

public class ReacaoMikaTest {
    private final MotorRecepcao motor = new MotorRecepcao();
    private final AtendimentoCafe atendimento = new AtendimentoCafe();
    private final CoreografiaCafe coreografia = new CoreografiaCafe();
    private final ReacaoMika reacao = new ReacaoMika();

    private void atualizar(long tempo) {
        atendimento.atualizar(tempo, motor);
        coreografia.atualizar(tempo, motor, -10000, null);
        reacao.atualizar(
                tempo,
                motor,
                atendimento,
                coreografia.getMika(),
                coreografia.getCliente(),
                coreografia.getProgressoCliente());
    }

    @Test
    public void demonstracaoEAlimentoSemToqueNaoGeramAprovacao() {
        atualizar(5050);
        assertEquals(ReacaoMika.Tipo.NENHUMA, reacao.getTipo());
        atualizar(21050);
        assertEquals(ReacaoMika.Tipo.NENHUMA, reacao.getTipo());
        assertEquals(0L, java.util.Arrays.stream(motor.salvar()).filter(v -> v > 0).count());
        assertEquals(0, reacao.getAlpha(), 0);
        assertEquals(0, reacao.getInclinacao(), 0);
    }

    @Test
    public void aprovacaoUsaAcertoFisicoCedoETerminaAntesDoProximoToqueRapido() {
        assertEquals(Julgamento.GREAT, motor.pressionar(20900).julgamento);
        motor.soltar();
        atualizar(20899);
        assertEquals(ReacaoMika.Tipo.NENHUMA, reacao.getTipo());
        atualizar(20950);
        assertEquals(ReacaoMika.Tipo.APROVA, reacao.getTipo());
        assertEquals(.25f, reacao.getProgresso(), 0);
        assertTrue(Math.abs(reacao.getInclinacao()) <= 1.4f);
        assertEquals(
                1L,
                java.util.Arrays.stream(motor.salvar())
                        .filter(v -> v == Julgamento.GREAT.ordinal() + 1)
                        .count());
        atualizar(21100);
        assertEquals(ReacaoMika.Tipo.NENHUMA, reacao.getTipo());
    }

    @Test
    public void lancamentoTemPrioridadeSemReiniciarReacaoQuandoTermina() {
        assertEquals(Julgamento.PERFECT, motor.pressionar(68030).julgamento);
        motor.soltar();
        int[] antes = motor.salvar();
        long[] instantes = motor.salvarInstantes();
        atualizar(68030);
        assertEquals(CoreografiaCafe.Mika.LANCAR, coreografia.getMika());
        assertEquals(ReacaoMika.Tipo.NENHUMA, reacao.getTipo());
        atualizar(68110);
        assertEquals(ReacaoMika.Tipo.APROVA, reacao.getTipo());
        assertEquals(.4f, reacao.getProgresso(), 0);
        atualizar(68230);
        assertEquals(ReacaoMika.Tipo.NENHUMA, reacao.getTipo());
        assertArrayEquals(antes, motor.salvar());
        assertArrayEquals(instantes, motor.salvarInstantes());
    }

    @Test
    public void sustoSegueMissPor550MsESpamNaoRenovaReacao() {
        assertEquals(Julgamento.MISS, motor.pressionar(25900).julgamento);
        motor.soltar();
        atualizar(26030);
        assertEquals(ReacaoMika.Tipo.SUSTO, reacao.getTipo());
        assertEquals(130 / 550f, reacao.getProgresso(), .0001f);
        assertNull(motor.pressionar(26120));
        motor.soltar();
        assertEquals(26450, motor.getBloqueadoAteMs());
        atualizar(26430);
        assertEquals(ReacaoMika.Tipo.SUSTO, reacao.getTipo());
        atualizar(26450);
        assertEquals(ReacaoMika.Tipo.NENHUMA, reacao.getTipo());
    }

    @Test
    public void restauracaoMantemMesmoQuadroSemLevarAprovacaoParaOutroPedido() {
        motor.pressionar(25030);
        motor.soltar();
        atualizar(25110);
        assertEquals(ReacaoMika.Tipo.APROVA, reacao.getTipo());
        float progresso = reacao.getProgresso(),
                alpha = reacao.getAlpha(),
                giro = reacao.getInclinacao();
        MotorRecepcao restaurado = new MotorRecepcao();
        restaurado.restaurar(motor.salvar());
        restaurado.restaurarInstantes(motor.salvarInstantes());
        AtendimentoCafe novoAtendimento = new AtendimentoCafe();
        novoAtendimento.atualizar(25110, restaurado);
        CoreografiaCafe novaCoreografia = new CoreografiaCafe();
        novaCoreografia.atualizar(25110, restaurado, -10000, null);
        ReacaoMika nova = new ReacaoMika();
        nova.atualizar(
                25110,
                restaurado,
                novoAtendimento,
                novaCoreografia.getMika(),
                novaCoreografia.getCliente(),
                novaCoreografia.getProgressoCliente());
        assertEquals(progresso, nova.getProgresso(), 0);
        assertEquals(alpha, nova.getAlpha(), 0);
        assertEquals(giro, nova.getInclinacao(), 0);
        atualizar(26600);
        assertEquals(ReacaoMika.Tipo.NENHUMA, reacao.getTipo());
        atualizar(28000);
        assertEquals(ReacaoMika.Tipo.NENHUMA, reacao.getTipo());
        atualizar(75000);
        assertEquals(CoreografiaCafe.Mika.DESPEDIDA, coreografia.getMika());
        assertEquals(ReacaoMika.Tipo.NENHUMA, reacao.getTipo());
    }
}
