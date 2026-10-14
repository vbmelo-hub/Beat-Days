package com.example.beatdays.game;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;

import com.example.beatdays.model.Julgamento;
import com.example.beatdays.ui.game.animation.AtendimentoCafe;
import com.example.beatdays.ui.game.animation.CoreografiaCafe;
import com.example.beatdays.ui.game.animation.ReacaoMika;

import org.junit.Test;

public final class MissMikaQualidadeTest {
    @Test
    public void missFisicoTemPrioridadeDurantePreparoELancamento() {
        MotorRecepcao motor = new MotorRecepcao();
        assertEquals(Julgamento.MISS, motor.pressionar(67210).julgamento);
        motor.soltar();
        for (long tempo : new long[] {67210, 67260, 67500}) {
            AtendimentoCafe a = new AtendimentoCafe();
            a.atualizar(tempo, motor);
            CoreografiaCafe c = new CoreografiaCafe();
            c.atualizar(tempo, motor, -10000, null);
            assertNotEquals(CoreografiaCafe.Mika.ESPERA, c.getMika());
            ReacaoMika r = new ReacaoMika();
            r.atualizar(tempo, motor, a, c.getMika(), c.getCliente(), c.getProgressoCliente());
            assertEquals(ReacaoMika.Tipo.SUSTO, r.getTipo());
            assertEquals(CoreografiaCafe.Paleta.CINZA, c.getPaleta());
        }
    }

    @Test
    public void perdaAutomaticaMostraSustoMesmoComProximoAlimentoEmPreparo() {
        MotorRecepcao motor = new MotorRecepcao();
        motor.processarPerdidas(29000);
        motor.processarPerdidas(29701);
        AtendimentoCafe a = new AtendimentoCafe();
        a.atualizar(29701, motor);
        CoreografiaCafe c = new CoreografiaCafe();
        c.atualizar(29701, motor, -10000, null);
        assertEquals(CoreografiaCafe.Mika.PREPARA, c.getMika());
        ReacaoMika r = new ReacaoMika();
        r.atualizar(29701, motor, a, c.getMika(), c.getCliente(), c.getProgressoCliente());
        assertEquals(ReacaoMika.Tipo.SUSTO, r.getTipo());
    }

    @Test
    public void erroEmRespiroReageSemClienteENaoEstendeRecuperacao() {
        MotorRecepcao motor = new MotorRecepcao();
        motor.pressionar(27000);
        motor.soltar();
        AtendimentoCafe a = new AtendimentoCafe();
        a.atualizar(27010, motor);
        assertNull(a.getPedidoAtual());
        CoreografiaCafe c = new CoreografiaCafe();
        c.atualizar(27010, motor, -10000, null);
        ReacaoMika r = new ReacaoMika();
        r.atualizar(27010, motor, a, c.getMika(), c.getCliente(), c.getProgressoCliente());
        assertEquals(ReacaoMika.Tipo.SUSTO, r.getTipo());
        assertNull(motor.pressionar(27100));
        motor.soltar();
        assertEquals(27550, motor.getBloqueadoAteMs());
        c.atualizar(27550, motor, -10000, null);
        a.atualizar(27550, motor);
        r.atualizar(27550, motor, a, c.getMika(), c.getCliente(), c.getProgressoCliente());
        assertEquals(ReacaoMika.Tipo.NENHUMA, r.getTipo());
    }
}
