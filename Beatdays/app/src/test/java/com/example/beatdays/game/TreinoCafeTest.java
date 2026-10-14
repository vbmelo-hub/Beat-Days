package com.example.beatdays.game;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.example.beatdays.model.EntregaCafe;
import com.example.beatdays.model.Julgamento;

import org.junit.Test;

public class TreinoCafeTest {
    private final MotorRecepcao motor = new MotorRecepcao();
    private final TreinoCafe treino = new TreinoCafe();

    private void tocar(long tempo) {
        treino.registrar(motor.pressionar(tempo));
        motor.soltar();
    }

    private void perdas(long tempo) {
        for (MotorRecepcao.Resultado r : motor.processarPerdidas(tempo)) {
            treino.registrar(r);
        }
    }

    private int id(long alvo) {
        for (EntregaCafe e : motor.getEntregas()) {
            if (e.tempoMs == alvo) {
                return e.id;
            }
        }
        throw new AssertionError();
    }

    @Test
    public void jogarDiretoNaoAtivaRepeticao() {
        perdas(20000);
        assertEquals(-1, treino.atualizar(20000, motor));
        assertFalse(treino.estaAtivo());
    }

    @Test
    public void semEntradaRearmaDuplaSemGerarAcertos() {
        treino.iniciar();
        perdas(15000);
        assertEquals(-1, treino.atualizar(15599, motor));
        assertEquals(11600, treino.atualizar(15600, motor));
        assertEquals(1, treino.getRepeticoes());
        assertNull(motor.getJulgamento(id(13000)));
        assertNull(motor.getJulgamento(id(13500)));
        assertEquals(-1, motor.getInstante(id(13000)));
        perdas(15600);
        assertEquals(11600, treino.atualizar(15600, motor));
        assertEquals(2, treino.getRepeticoes());
    }

    @Test
    public void capturaParcialNaoAprovaLicao() {
        treino.iniciar();
        tocar(13000);
        perdas(15000);
        assertEquals(11600, treino.atualizar(15600, motor));
        assertEquals(TreinoCafe.Etapa.DUPLA, treino.getEtapa());
        assertNull(motor.getJulgamento(id(13000)));
        assertNull(motor.getJulgamento(id(13500)));
    }

    @Test
    public void greatEGoodAprovamDuplaETresCapturasAprovamTrio() {
        treino.iniciar();
        tocar(12890);
        tocar(13660);
        assertEquals(-1, treino.atualizar(15600, motor));
        assertEquals(TreinoCafe.Etapa.TRIO, treino.getEtapa());
        tocar(17500);
        tocar(18090);
        tocar(18660);
        assertEquals(-1, treino.atualizar(19600, motor));
        assertEquals(TreinoCafe.Etapa.CONCLUIDO, treino.getEtapa());
        assertFalse(treino.estaAtivo());
    }

    @Test
    public void spamNaoAprovaLicaoMesmoComDuasCapturas() {
        treino.iniciar();
        tocar(13000);
        tocar(13500);
        tocar(14000);
        tocar(14100);
        assertTrue(treino.falhou());
        assertEquals(11600, treino.atualizar(15600, motor));
        assertEquals(-1, motor.getBloqueadoAteMs());
        assertFalse(treino.falhou());
    }

    @Test
    public void repetirTrioPreservaDuplaEResultadosForaDaLicao() {
        treino.iniciar();
        tocar(13000);
        tocar(13500);
        treino.atualizar(15600, motor);
        tocar(17500);
        perdas(19600);
        assertEquals(15600, treino.atualizar(19600, motor));
        assertEquals(Julgamento.PERFECT, motor.getJulgamento(id(13000)));
        assertEquals(Julgamento.PERFECT, motor.getJulgamento(id(13500)));
        for (long t : new long[] {17500, 18000, 18500}) {
            assertNull(motor.getJulgamento(id(t)));
            assertEquals(-1, motor.getToqueTrio(id(t)));
        }
        tocar(21000);
        assertTrue(motor.reiniciarPedidoTreino(3));
        assertEquals(Julgamento.PERFECT, motor.getJulgamento(id(21000)));
        assertFalse(motor.reiniciarPedidoTreino(4));
        assertFalse(motor.reiniciarPedidoTreino(0));
        assertFalse(motor.reiniciarPedidoTreino(999));
        assertEquals(Julgamento.PERFECT, motor.getJulgamento(id(21000)));
    }

    @Test
    public void restauracaoPreservaLicaoErroERepeticaoENovaPartidaLimpa() {
        treino.iniciar();
        perdas(15600);
        treino.atualizar(15600, motor);
        tocar(12000);
        TreinoCafe outro = new TreinoCafe();
        outro.restaurar(treino.getEtapa().ordinal(), treino.falhou(), treino.getRepeticoes());
        assertEquals(TreinoCafe.Etapa.DUPLA, outro.getEtapa());
        assertTrue(outro.falhou());
        assertEquals(1, outro.getRepeticoes());
        MotorRecepcao salvo = new MotorRecepcao();
        salvo.restaurar(motor.salvar());
        salvo.restaurarInstantes(motor.salvarInstantes());
        assertEquals(11600, outro.atualizar(15600, salvo));
        outro.reiniciar();
        assertEquals(TreinoCafe.Etapa.INATIVO, outro.getEtapa());
        assertEquals(0, outro.getRepeticoes());
        outro.restaurar(999, true, -1);
        assertFalse(outro.estaAtivo());
        assertFalse(outro.falhou());
    }
}
