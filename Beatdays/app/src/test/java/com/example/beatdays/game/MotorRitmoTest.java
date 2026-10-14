package com.example.beatdays.game;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.example.beatdays.model.Julgamento;

import org.junit.Test;

import java.util.Arrays;

public class MotorRitmoTest {
    private MotorRitmo motor() {
        MotorRitmo motor = new MotorRitmo();
        motor.carregarBatidas(Arrays.asList(1000L, 1600L, 2200L));
        return motor;
    }

    @Test
    public void preservaLimitesDeJulgamento() {
        assertEquals(Julgamento.PERFECT, motor().registrarNoIndice(0, 930));
        assertEquals(Julgamento.GREAT, motor().registrarNoIndice(0, 929));
        assertEquals(Julgamento.GREAT, motor().registrarNoIndice(0, 1130));
        assertEquals(Julgamento.GOOD, motor().registrarNoIndice(0, 1131));
        assertEquals(Julgamento.GOOD, motor().registrarNoIndice(0, 1200));
        assertEquals(Julgamento.MISS, motor().registrarNoIndice(0, 1201));
    }

    @Test
    public void naoCapturaOutraNotaNemRepeteCaptura() {
        MotorRitmo motor = motor();
        assertEquals(Julgamento.MISS, motor.registrarNoIndice(0, 1600));
        assertEquals(Julgamento.PERFECT, motor.registrarNoIndice(1, 1600));
        assertEquals(Julgamento.MISS, motor.registrarNoIndice(1, 1600));
        assertArrayEquals(new boolean[] {false, true, false}, motor.getEstadoBatidas());
    }

    @Test
    public void perdaNaoContaDuasVezes() {
        MotorRitmo motor = motor();
        assertTrue(motor.marcarComoPerdida(0));
        assertFalse(motor.marcarComoPerdida(0));
    }

    @Test
    public void restauraNotasSemMudarIndice() {
        MotorRitmo original = motor();
        original.registrarNoIndice(0, 1000);
        original.marcarComoPerdida(1);
        MotorRitmo restaurado = motor();
        restaurado.restaurarEstadoBatidas(original.getEstadoBatidas());
        assertEquals(Julgamento.MISS, restaurado.registrarNoIndice(1, 1600));
        assertEquals(Julgamento.PERFECT, restaurado.registrarNoIndice(2, 2200));
    }

    @Test
    public void reiniciarLimpaNotasProcessadas() {
        MotorRitmo motor = motor();
        motor.marcarComoPerdida(0);
        motor.carregarBatidas(Arrays.asList(1000L, 1600L, 2200L));
        assertArrayEquals(new boolean[3], motor.getEstadoBatidas());
    }

    @Test
    public void indiceInvalidoNaoConsomeNotas() {
        MotorRitmo motor = motor();
        assertEquals(Julgamento.MISS, motor.registrarNoIndice(-1, 1000));
        assertEquals(Julgamento.MISS, motor.registrarNoIndice(3, 1000));
        assertArrayEquals(new boolean[3], motor.getEstadoBatidas());
    }
}
