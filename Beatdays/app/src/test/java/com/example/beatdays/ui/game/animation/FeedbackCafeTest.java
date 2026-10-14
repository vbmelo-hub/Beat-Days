package com.example.beatdays.ui.game.animation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.example.beatdays.game.MotorRecepcao;
import com.example.beatdays.model.Julgamento;

import org.junit.Test;

public class FeedbackCafeTest {
    private final MotorRecepcao motor = new MotorRecepcao();
    private final FeedbackCafe feedback = new FeedbackCafe();

    private void tocar(long tempo) {
        motor.pressionar(tempo);
        motor.soltar();
        feedback.atualizar(tempo, motor);
    }

    @Test
    public void greatCedoEGoodTardeUsamInstanteRealDoToque() {
        tocar(20890);
        assertEquals(Julgamento.GREAT, feedback.getJulgamento());
        assertEquals(FeedbackCafe.Direcao.CEDO, feedback.getDirecao());
        tocar(25160);
        assertEquals(Julgamento.GOOD, feedback.getJulgamento());
        assertEquals(FeedbackCafe.Direcao.TARDE, feedback.getDirecao());
    }

    @Test
    public void perfectNaoPrecisaDeCorrecaoEDemonstracaoNaoInventaResultado() {
        feedback.atualizar(10000, motor);
        assertNull(feedback.getJulgamento());
        tocar(21050);
        assertEquals(Julgamento.PERFECT, feedback.getJulgamento());
        assertEquals(FeedbackCafe.Direcao.NENHUMA, feedback.getDirecao());
    }

    @Test
    public void missEToqueIgnoradoNaoViramAcertoNemDica() {
        motor.processarPerdidas(21250);
        feedback.atualizar(21250, motor);
        assertEquals(Julgamento.MISS, feedback.getJulgamento());
        assertEquals(FeedbackCafe.Direcao.NENHUMA, feedback.getDirecao());
        assertFalse(feedback.isForaDoRitmo());
        tocar(22000);
        assertTrue(feedback.isForaDoRitmo());
        assertNull(motor.pressionar(22100));
        feedback.atualizar(22400, motor);
        assertEquals(.5f, feedback.getAlpha(), 0);
        feedback.atualizar(22450, motor);
        assertNull(feedback.getJulgamento());
    }

    @Test
    public void restauracaoPreservaDirecaoEFeedbackNaoVoltaAntesDoToque() {
        tocar(20890);
        MotorRecepcao restaurado = new MotorRecepcao();
        restaurado.restaurar(motor.salvar());
        feedback.atualizar(21050, restaurado);
        assertEquals(Julgamento.GREAT, feedback.getJulgamento());
        assertEquals(FeedbackCafe.Direcao.NENHUMA, feedback.getDirecao());
        restaurado.restaurarInstantes(motor.salvarInstantes());
        feedback.atualizar(20980, restaurado);
        assertEquals(Julgamento.GREAT, feedback.getJulgamento());
        assertEquals(FeedbackCafe.Direcao.CEDO, feedback.getDirecao());
        feedback.atualizar(20889, restaurado);
        assertNull(feedback.getJulgamento());
        feedback.atualizar(21340, restaurado);
        assertNull(feedback.getJulgamento());
    }
}
