package com.example.beatdays.ui.game;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import android.content.Context;
import android.os.SystemClock;
import android.view.View;
import android.widget.TextView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.beatdays.MainActivity;
import com.example.beatdays.R;
import com.example.beatdays.audio.ControladorAudio;
import com.example.beatdays.ui.game.support.GestosTeste;
import com.example.beatdays.ui.game.view.CenaCafe;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(AndroidJUnit4.class)
public class SincroniaInstrumentedTest {
    private final AtomicReference<MainActivity> atividade = new AtomicReference<>();

    @Before
    public void preparar() {
        limpar();
    }

    @After
    public void encerrar() {
        limpar();
    }

    private void limpar() {
        InstrumentationRegistry.getInstrumentation()
                .getTargetContext()
                .getSharedPreferences("beatdays_ajustes", Context.MODE_PRIVATE)
                .edit()
                .clear()
                .commit();
    }

    @Test
    public void ajustePersisteNaoAlteraPedidoAtualESincronizaCenaEEntrada() throws Exception {
        try (ActivityScenario<MainActivity> scenario =
                ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(atividade::set);
            tocar(R.id.tv_overlay_dica);
            tocar(R.id.btn_pausar);
            long pausado = posicao();
            for (int n = 0; n < 20; n++) {
                tocar(R.id.btn_atrasar_cena);
            }
            scenario.onActivity(
                    a -> {
                        assertEquals(
                                View.VISIBLE,
                                a.findViewById(R.id.painel_sincronia).getVisibility());
                        assertEquals(
                                a.getString(R.string.sincronia_valor, 200),
                                ((TextView) a.findViewById(R.id.tv_sincronia))
                                        .getText()
                                        .toString());
                        assertEquals(0, inteiro("ajustePartidaMs"));
                        assertEquals(
                                200,
                                a.getSharedPreferences("beatdays_ajustes", Context.MODE_PRIVATE)
                                        .getInt("atraso_cena_ms", 0));
                    });
            assertEquals(pausado, posicao(), 35);
            scenario.recreate();
            scenario.onActivity(atividade::set);
            scenario.onActivity(
                    a -> {
                        assertEquals(200, inteiro("ajusteConfiguradoMs"));
                        assertEquals(0, inteiro("ajustePartidaMs"));
                    });
            tocar(R.id.btn_pausar);
            buscar(21050);
            tocar(R.id.tv_score);
            pontos(100, 1);
            tocar(R.id.btn_pausar);
            tocar(R.id.btn_recomecar_ajuste);
            pontos(0, 0);
            scenario.onActivity(
                    a ->
                            assertEquals(
                                    View.GONE,
                                    a.findViewById(R.id.painel_sincronia).getVisibility()));
            buscar(16320);
            scenario.onActivity(
                    a ->
                            assertEquals(
                                    1.03f,
                                    ((CenaCafe) a.findViewById(R.id.cena_cafe)).getScaleX(),
                                    .0001f));
            buscar(21200);
            tocar(R.id.tv_score);
            pontos(100, 1);
            tocar(R.id.btn_pausar);
            for (int n = 0; n < 40; n++) {
                tocar(R.id.btn_adiantar_cena);
            }
            scenario.onActivity(
                    a -> {
                        assertEquals(-200, inteiro("ajusteConfiguradoMs"));
                        assertEquals(200, inteiro("ajustePartidaMs"));
                    });
            tocar(R.id.btn_recomecar_ajuste);
            buscar(20800);
            tocar(R.id.tv_score);
            pontos(100, 1);
            tocar(R.id.btn_pausar);
            tocar(R.id.btn_zerar_sincronia);
            scenario.onActivity(
                    a -> {
                        assertEquals(0, inteiro("ajusteConfiguradoMs"));
                        assertEquals(-200, inteiro("ajustePartidaMs"));
                    });
            tocar(R.id.btn_recomecar_ajuste);
            buscar(21000);
            tocar(R.id.tv_score);
            pontos(100, 1);
            tocar(R.id.btn_pausar);
            for (int n = 0; n < 30; n++) {
                tocar(R.id.btn_atrasar_cena);
            }
            scenario.onActivity(
                    a -> {
                        assertEquals(250, inteiro("ajusteConfiguradoMs"));
                        assertFalse(a.findViewById(R.id.btn_atrasar_cena).isEnabled());
                    });
            for (int n = 0; n < 55; n++) {
                tocar(R.id.btn_adiantar_cena);
            }
            scenario.onActivity(
                    a -> {
                        assertEquals(-250, inteiro("ajusteConfiguradoMs"));
                        assertFalse(a.findViewById(R.id.btn_adiantar_cena).isEnabled());
                    });
            tocar(R.id.btn_zerar_sincronia);
            pontos(100, 1);
        }
    }

    @Test
    public void novaAberturaMantemAjusteSalvo() {
        try (ActivityScenario<MainActivity> scenario =
                ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(atividade::set);
            tocar(R.id.tv_overlay_dica);
            tocar(R.id.btn_pausar);
            for (int n = 0; n < 10; n++) {
                tocar(R.id.btn_atrasar_cena);
            }
        }
        try (ActivityScenario<MainActivity> scenario =
                ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(atividade::set);
            tocar(R.id.tv_overlay_dica);
            scenario.onActivity(
                    a -> {
                        assertEquals(100, inteiro("ajusteConfiguradoMs"));
                        assertEquals(100, inteiro("ajustePartidaMs"));
                    });
            buscar(21100);
            tocar(R.id.tv_score);
            pontos(100, 1);
        }
    }

    private int inteiro(String nome) {
        try {
            Field f = MainActivity.class.getDeclaredField(nome);
            f.setAccessible(true);
            return f.getInt(atividade.get());
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    private ControladorAudio audio() {
        try {
            Field f = MainActivity.class.getDeclaredField("audio");
            f.setAccessible(true);
            return (ControladorAudio) f.get(atividade.get());
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    private long posicao() {
        AtomicLong tempo = new AtomicLong();
        InstrumentationRegistry.getInstrumentation()
                .runOnMainSync(() -> tempo.set(audio().getPosicaoAtualMs()));
        return tempo.get();
    }

    private void buscar(int tempo) {
        InstrumentationRegistry.getInstrumentation()
                .runOnMainSync(
                        () -> {
                            audio().pausar();
                            audio().definirPosicaoMs(tempo);
                        });
        long limite = SystemClock.uptimeMillis() + 4000;
        int estavel = 0;
        while (SystemClock.uptimeMillis() < limite) {
            estavel = Math.abs(posicao() - tempo) <= 30 ? estavel + 1 : 0;
            if (estavel >= 6) {
                return;
            }
            SystemClock.sleep(30);
        }
        fail("Busca não alcançou " + tempo);
    }

    private void tocar(int id) {
        GestosTeste.tocar(atividade.get(), id);
    }

    private void pontos(int score, int combo) {
        InstrumentationRegistry.getInstrumentation()
                .runOnMainSync(
                        () -> {
                            MainActivity a = atividade.get();
                            assertEquals(
                                    a.getString(R.string.score, score),
                                    ((TextView) a.findViewById(R.id.tv_score))
                                            .getText()
                                            .toString());
                            assertEquals(
                                    a.getString(R.string.combo, combo),
                                    ((TextView) a.findViewById(R.id.tv_combo))
                                            .getText()
                                            .toString());
                        });
    }
}
