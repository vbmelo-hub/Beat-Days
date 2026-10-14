package com.example.beatdays.ui.game;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
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
import com.example.beatdays.model.EntregaCafe;
import com.example.beatdays.ui.game.support.GestosTeste;

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
public class RoteiroInstrumentedTest {
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
    public void climaxExigeToqueMantemDegrausEPausaRecriaSemMudarChart() throws Exception {
        try (ActivityScenario<MainActivity> scenario =
                ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(atividade::set);
            tocar();
            buscar(66000);
            for (int n = 0; n < 3; n++) {
                buscar(67100 + n * 250);
                zoom(1 + (n + 1) * .03f);
                pontos(0, 0);
            }
            InstrumentationRegistry.getInstrumentation()
                    .runOnMainSync(
                            () -> {
                                for (long alvo : new long[] {68000, 68250, 68500}) {
                                    assertNull(motor().getJulgamento(id(alvo)));
                                }
                            });
            salvarImagem("beatdays-roteiro-climax.png");
            buscar(68030);
            tocar();
            pontos(100, 1);
            buscar(68120);
            zoom(1.06f);
            tocar(R.id.btn_pausar);
            long pausa = posicao();
            scenario.recreate();
            scenario.onActivity(atividade::set);
            esperarPosicao((int) pausa);
            zoom(1.06f);
            pontos(100, 1);
            InstrumentationRegistry.getInstrumentation()
                    .runOnMainSync(
                            () -> {
                                assertNull(motor().getJulgamento(id(68250)));
                                atividade.get().findViewById(R.id.btn_pausar).performClick();
                                audio().pausar();
                                assertEquals(
                                        View.GONE,
                                        atividade
                                                .get()
                                                .findViewById(R.id.overlay_jogo)
                                                .getVisibility());
                            });
            buscar(68280);
            InstrumentationRegistry.getInstrumentation()
                    .runOnMainSync(() -> assertNull(motor().getJulgamento(id(68250))));
            tocar();
            pontos(200, 2);
            buscar(68370);
            zoom(1.03f);
            buscar(68530);
            tocar();
            pontos(300, 3);
            buscar(68620);
            zoom(1);
            buscar(70000);
            zoom(1);
            buscar(71580);
            zoom(1);
            buscar(72530);
            tocar();
            pontos(400, 4);
            zoom(1);
        }
    }

    private void zoom(float esperado) {
        InstrumentationRegistry.getInstrumentation()
                .runOnMainSync(
                        () -> {
                            View cena = atividade.get().findViewById(R.id.cena_cafe);
                            assertEquals(esperado, cena.getScaleX(), .00001f);
                            assertEquals(esperado, cena.getScaleY(), .00001f);
                        });
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
