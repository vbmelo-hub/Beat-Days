package com.example.beatdays.ui.game;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.SystemClock;
import android.view.View;
import android.widget.TextView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.beatdays.MainActivity;
import com.example.beatdays.R;
import com.example.beatdays.audio.ControladorAudio;
import com.example.beatdays.game.MotorRecepcao;
import com.example.beatdays.game.RoteiroRecepcao;
import com.example.beatdays.game.TreinoCafe;
import com.example.beatdays.model.EntregaCafe;
import com.example.beatdays.ui.game.support.GestosTeste;
import com.example.beatdays.ui.game.view.EfeitosCafeView;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(AndroidJUnit4.class)
public class AberturaInstrumentedTest {
    private final AtomicReference<MainActivity> atividade = new AtomicReference<>();

    @Before
    public void preparar() {
        zerarAjuste();
    }

    @After
    public void encerrar() {
        zerarAjuste();
    }

    private void zerarAjuste() {
        InstrumentationRegistry.getInstrumentation()
                .getTargetContext()
                .getSharedPreferences("beatdays_ajustes", Context.MODE_PRIVATE)
                .edit()
                .putInt("atraso_cena_ms", 0)
                .commit();
    }

    @Test
    public void aberturaAcompanhaPausaRecriacaoESaiAntesDoPrimeiroAviso() throws Exception {
        try (ActivityScenario<MainActivity> scenario =
                ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(atividade::set);
            abertura(0);
            tocar();
            buscar(800);
            abertura(1);
            pontos(0, 0);
            salvarImagem("beatdays-abertura.png");
            tocar(R.id.btn_pausar);
            long pausa = posicao();
            SystemClock.sleep(200);
            Bitmap antes = quadro();
            scenario.recreate();
            scenario.onActivity(atividade::set);
            esperarPosicao((int) pausa);
            abertura(1);
            Bitmap depois = quadro();
            assertTrue("A placa deve preservar o quadro na pausa", antes.sameAs(depois));
            antes.recycle();
            depois.recycle();
            InstrumentationRegistry.getInstrumentation()
                    .runOnMainSync(
                            () -> {
                                atividade.get().findViewById(R.id.btn_pausar).performClick();
                                audio().pausar();
                            });
            buscar(2000);
            abertura(1);
            salvarImagem("beatdays-abertura.png");
            buscar(3200);
            abertura(.5f);
            buscar(3500);
            abertura(0);
            buscar(4050);
            abertura(0);
            pontos(0, 0);
            InstrumentationRegistry.getInstrumentation()
                    .runOnMainSync(
                            () -> {
                                EfeitosCafeView efeitos =
                                        atividade.get().findViewById(R.id.efeitos_cafe);
                                assertEquals(EntregaCafe.Aviso.MIKA, efeitos.getAvisoAtivo());
                                assertEquals(4000, motor().getEntregas().get(0).avisoMs);
                                assertNull(motor().getJulgamento(id(5000)));
                            });
            salvarImagem("beatdays-abertura-demonstracao.png");
        }
    }

    @Test
    public void treinoPulaAberturaERecomecarNormalReapresentaAFase() throws Exception {
        try (ActivityScenario<MainActivity> scenario =
                ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(atividade::set);
            tocar(R.id.btn_treinar);
            esperarBuscaTreino();
            buscar(12050);
            abertura(0);
            pontos(0, 0);
            InstrumentationRegistry.getInstrumentation()
                    .runOnMainSync(() -> assertTrue(treino().estaAtivo()));
            tocar(R.id.btn_pausar);
            tocar(R.id.btn_recomecar_ajuste);
            buscar(800);
            abertura(1);
            pontos(0, 0);
            InstrumentationRegistry.getInstrumentation()
                    .runOnMainSync(
                            () -> {
                                assertFalse(treino().estaAtivo());
                                assertFalse(audio().estaTocando());
                                for (EntregaCafe e : motor().getEntregas()) {
                                    assertNull(motor().getJulgamento(e.id));
                                }
                            });
        }
    }

    private TreinoCafe treino() {
        try {
            Field f = MainActivity.class.getDeclaredField("treino");
            f.setAccessible(true);
            return (TreinoCafe) f.get(atividade.get());
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    private void esperarBuscaTreino() {
        AtomicLong pronto = new AtomicLong();
        long limite = SystemClock.uptimeMillis() + 4000;
        while (SystemClock.uptimeMillis() < limite && pronto.get() == 0) {
            InstrumentationRegistry.getInstrumentation()
                    .runOnMainSync(
                            () -> {
                                try {
                                    Field f = MainActivity.class.getDeclaredField("buscandoTreino");
                                    f.setAccessible(true);
                                    if (!f.getBoolean(atividade.get())) {
                                        audio().pausar();
                                        pronto.set(1);
                                    }
                                } catch (ReflectiveOperationException e) {
                                    throw new AssertionError(e);
                                }
                            });
            if (pronto.get() == 0) {
                SystemClock.sleep(20);
            }
        }
        assertEquals(1, pronto.get());
    }

    private void abertura(float alpha) {
        InstrumentationRegistry.getInstrumentation()
                .runOnMainSync(
                        () -> {
                            EfeitosCafeView efeitos =
                                    atividade.get().findViewById(R.id.efeitos_cafe);
                            assertEquals(alpha, efeitos.getAlphaAbertura(), .01f);
                            View cena = atividade.get().findViewById(R.id.cena_cafe);
                            assertEquals(0, cena.getTranslationX(), 0);
                            assertEquals(0, cena.getTranslationY(), 0);
                            assertEquals(1, cena.getScaleX(), 0);
                            assertEquals(1, cena.getScaleY(), 0);
                        });
    }

    private Bitmap quadro() {
        AtomicReference<Bitmap> captura = new AtomicReference<>();
        InstrumentationRegistry.getInstrumentation()
                .runOnMainSync(
                        () -> {
                            View v = atividade.get().findViewById(R.id.cena_cafe);
                            Bitmap b =
                                    Bitmap.createBitmap(
                                            v.getWidth(), v.getHeight(), Bitmap.Config.ARGB_8888);
                            v.draw(new Canvas(b));
                            captura.set(b);
                        });
        return captura.get();
    }

    private MotorRecepcao motor() {
        try {
            Field f = MainActivity.class.getDeclaredField("motor");
            f.setAccessible(true);
            return (MotorRecepcao) f.get(atividade.get());
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    private int id(long tempo) {
        for (EntregaCafe e : RoteiroRecepcao.criarEntregas()) {
            if (e.tempoMs == tempo) {
                return e.id;
            }
        }
        throw new AssertionError("Captura ausente: " + tempo);
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
        esperarPosicao(tempo);
    }

    private void esperarPosicao(int tempo) {
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

    private void tocar() {
        tocar(0);
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

    private void salvarImagem(String nome) throws Exception {
        AtomicReference<Bitmap> captura = new AtomicReference<>();
        InstrumentationRegistry.getInstrumentation()
                .runOnMainSync(
                        () -> {
                            View tela = atividade.get().getWindow().getDecorView();
                            Bitmap bitmap =
                                    Bitmap.createBitmap(
                                            tela.getWidth(),
                                            tela.getHeight(),
                                            Bitmap.Config.ARGB_8888);
                            tela.draw(new Canvas(bitmap));
                            captura.set(bitmap);
                        });
        Bitmap bitmap = captura.get();
        assertNotNull(bitmap);
        File destino = new File(atividade.get().getExternalFilesDir(null), nome);
        try (FileOutputStream stream = new FileOutputStream(destino)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        }
        bitmap.recycle();
    }
}
