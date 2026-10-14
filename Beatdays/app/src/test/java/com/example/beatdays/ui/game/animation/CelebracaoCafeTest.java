package com.example.beatdays.ui.game.animation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.example.beatdays.game.MotorRecepcao;

import org.junit.Test;

public class CelebracaoCafeTest {
    private final MotorRecepcao motor = new MotorRecepcao();
    private final AtendimentoCafe atendimento = new AtendimentoCafe();

    private float em(long tempo) {
        atendimento.atualizar(tempo, motor);
        return CelebracaoCafe.progresso(tempo, motor, atendimento, CoreografiaCafe.Cliente.ESPERA);
    }

    private void tocar(long tempo) {
        motor.pressionar(tempo);
        motor.soltar();
    }

    @Test
    public void soComecaDepoisDaCapturaRealETerminaUmaVez() {
        assertEquals(-1, em(21400), 0);
        tocar(21050);
        assertEquals(-1, em(21369), 0);
        assertEquals(0, em(21370), 0);
        assertEquals(.5f, em(21570), 0);
        assertEquals(-1, em(21770), 0);
        assertEquals(-1, em(24100), 0);
    }

    @Test
    public void duplaIncompletaEPerdidaNaoAgradecem() {
        tocar(25050);
        assertEquals(-1, em(25400), 0);
        motor.processarPerdidas(25800);
        assertEquals(-1, em(26100), 0);
        assertEquals(AtendimentoCafe.Desfecho.PARCIAL, atendimento.getDesfecho());
        motor.processarPerdidas(33250);
        assertEquals(-1, em(33600), 0);
    }

    @Test
    public void capturaTardiaRespeitaSaidaEErroInterrompeReacao() {
        tocar(21190);
        assertEquals(-1, em(21509), 0);
        assertEquals(0, em(21510), 0);
        assertTrue(em(21749) > 0);
        assertEquals(-1, em(21750), 0);
        atendimento.atualizar(21600, motor);
        assertEquals(
                -1,
                CelebracaoCafe.progresso(21600, motor, atendimento, CoreografiaCafe.Cliente.ERRA),
                0);
    }

    @Test
    public void trioSoAgradeceDepoisDeTodosEReconstroiMesmoInstante() {
        tocar(57050);
        tocar(57300);
        assertEquals(-1, em(57600), 0);
        tocar(57550);
        assertEquals(-1, em(57869), 0);
        assertEquals(.5f, em(58070), 0);
        float quadro = em(58070);
        assertEquals(quadro, em(58070), 0);
        assertEquals(-1, em(58250), 0);
    }
}
