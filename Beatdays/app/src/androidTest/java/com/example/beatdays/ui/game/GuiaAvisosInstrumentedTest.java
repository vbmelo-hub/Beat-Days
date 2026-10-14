package com.example.beatdays.ui.game;

import static org.junit.Assert.assertEquals;
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
import com.example.beatdays.model.EntregaCafe;
import com.example.beatdays.model.Julgamento;
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
public class GuiaAvisosInstrumentedTest {
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
    public void relogioETrioContamAvisosPreservamCenaNaPausaESaemAntesDaCaptura() throws Exception {
        try (ActivityScenario<MainActivity> scenario =
                ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(atividade::set);
            tocar();
            buscar(12200);
            legenda(R.string.guia_relogio, 1, 2);
            pontos(0, 0);
            InstrumentationRegistry.getInstrumentation()
                    .runOnMainSync(() -> assertNull(motor().getJulgamento(id(13000))));
            tocar(R.id.btn_pausar);
            long pausa = posicao();
            SystemClock.sleep(200);
            Bitmap antes = quadro();
            scenario.recreate();
            scenario.onActivity(atividade::set);
            esperarPosicao((int) pausa);
            legenda(R.string.guia_relogio, 1, 2);
            Bitmap depois = quadro();
            assertTrue("Aviso e legenda devem preservar o quadro na pausa", antes.sameAs(depois));
            antes.recycle();
            depois.recycle();
            InstrumentationRegistry.getInstrumentation()
                    .runOnMainSync(
                            () -> {
                                atividade.get().findViewById(R.id.btn_pausar).performClick();
                                audio().pausar();
                            });
            buscar(12600);
            legenda(R.string.guia_relogio, 2, 2);
            salvarImagem("beatdays-guia-relogio.png");
            buscar(12950);
            vazia();
            buscar(13030);
            tocar();
            pontos(0, 0);
            InstrumentationRegistry.getInstrumentation()
                    .runOnMainSync(
                            () ->
                                    assertEquals(
                                            Julgamento.PERFECT, motor().getJulgamento(id(13000))));
            buscar(14000);
            for (int n = 0; n < 3; n++) {
                buscar(16200 + n * 500);
                legenda(R.string.guia_cronometro, n + 1, 3);
            }
            salvarImagem("beatdays-guia-cronometro.png");
            buscar(17450);
            vazia();
            pontos(0, 0);
        }
    }

    @Test
    public void mikaApareceNaDemonstracaoEFasePontuadaNaoTemLegenda() throws Exception {
        try (ActivityScenario<MainActivity> scenario =
                ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(atividade::set);
            vazia();
            tocar();
            buscar(4200);
            legenda(R.string.guia_mika, 1, 1);
            salvarImagem("beatdays-guia-mika.png");
            buscar(4500);
            vazia();
            buscar(8200);
            legenda(R.string.guia_cronometro, 1, 3);
            buscar(20000);
            vazia();
            buscar(24100);
            vazia();
            pontos(0, 0);
            InstrumentationRegistry.getInstrumentation()
                    .runOnMainSync(
                            () -> {
                                assertEquals(
                                        EntregaCafe.Aviso.RELOGIO,
                                        ((EfeitosCafeView)
                                                        atividade
                                                                .get()
                                                                .findViewById(R.id.efeitos_cafe))
                                                .getAvisoAtivo());
                                assertNull(motor().getJulgamento(id(25000)));
                            });
        }
    }

    private void legenda(int recurso, int numero, int quantidade) {
        InstrumentationRegistry.getInstrumentation()
                .runOnMainSync(
                        () ->
                                assertEquals(
                                        atividade.get().getString(recurso, numero, quantidade),
                                        ((EfeitosCafeView)
                                                        atividade
                                                                .get()
                                                                .findViewById(R.id.efeitos_cafe))
                                                .getLegendaAviso()));
    }

    private void vazia() {
        InstrumentationRegistry.getInstrumentation()
                .runOnMainSync(
                        () ->
                                assertEquals(
                                        "",
                                        ((EfeitosCafeView)
                                                        atividade
                                                                .get()
                                                                .findViewById(R.id.efeitos_cafe))
                                                .getLegendaAviso()));
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
