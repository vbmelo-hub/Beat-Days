package com.example.beatdays.model;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.example.beatdays.game.MotorRecepcao;

import org.junit.Test;

public class ResumoTimingCafeTest {
    private final MotorRecepcao motor = new MotorRecepcao();

    private void tocar(long tempo) {
        assertNotNull(motor.pressionar(tempo));
        motor.soltar();
    }

    @Test
    public void treinoPerfectMissESpamNaoContamComoTimingImpreciso() {
        tocar(12890);
        tocar(21030);
        tocar(22000);
        assertNull(motor.pressionar(22100));
        motor.soltar();
        motor.processarPerdidas(80000);
        ResumoTimingCafe resumo = new ResumoTimingCafe(motor);
        assertFalse(resumo.temDesvios());
        assertEquals(0, resumo.getCedo());
        assertEquals(0, resumo.getTarde());
    }

    @Test
    public void greatEGoodCedoETardeGeramOrientacaoPelaMaioria() {
        tocar(20890);
        tocar(24820);
        tocar(25670);
        ResumoTimingCafe resumo = new ResumoTimingCafe(motor);
        assertEquals(2, resumo.getCedo());
        assertEquals(1, resumo.getTarde());
        assertEquals(ResumoTimingCafe.Tendencia.CEDO, resumo.getTendencia());
        tocar(29660);
        tocar(30100);
        resumo = new ResumoTimingCafe(motor);
        assertEquals(2, resumo.getCedo());
        assertEquals(3, resumo.getTarde());
        assertEquals(ResumoTimingCafe.Tendencia.TARDE, resumo.getTendencia());
    }

    @Test
    public void empateNaoRecomendaAdiantarOuAtrasar() {
        tocar(20890);
        tocar(25160);
        ResumoTimingCafe resumo = new ResumoTimingCafe(motor);
        assertTrue(resumo.temDesvios());
        assertEquals(1, resumo.getCedo());
        assertEquals(1, resumo.getTarde());
        assertEquals(ResumoTimingCafe.Tendencia.EQUILIBRADO, resumo.getTendencia());
    }

    @Test
    public void restauracaoPreservaResumoESnapshotAntigoNaoInventaDirecao() {
        tocar(20890);
        tocar(25160);
        MotorRecepcao salvo = new MotorRecepcao();
        salvo.restaurar(motor.salvar());
        assertFalse(new ResumoTimingCafe(salvo).temDesvios());
        salvo.restaurarInstantes(motor.salvarInstantes());
        ResumoTimingCafe resumo = new ResumoTimingCafe(salvo);
        assertEquals(1, resumo.getCedo());
        assertEquals(1, resumo.getTarde());
    }

    @Test
    public void lerResumoNaoMudaMotorEReinicioLimpaIndicacoes() {
        tocar(20890);
        int[] julgamentos = motor.salvar();
        long[] instantes = motor.salvarInstantes();
        new ResumoTimingCafe(motor);
        new ResumoTimingCafe(motor);
        assertArrayEquals(julgamentos, motor.salvar());
        assertArrayEquals(instantes, motor.salvarInstantes());
        motor.reiniciar();
        assertFalse(new ResumoTimingCafe(motor).temDesvios());
    }
}
