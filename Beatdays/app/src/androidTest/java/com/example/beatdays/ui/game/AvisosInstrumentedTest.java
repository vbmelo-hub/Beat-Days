package com.example.beatdays.ui.game;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.beatdays.MainActivity;
import com.example.beatdays.R;
import com.example.beatdays.audio.ControladorAudio;
import com.example.beatdays.game.MotorRecepcao;
import com.example.beatdays.model.EntregaCafe;
import com.example.beatdays.model.Julgamento;
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
public class AvisosInstrumentedTest {
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
    public void mikaAnunciaUmaRelogioDuasETimerTresSemAcertoAutomatico() throws Exception {
        try (ActivityScenario<MainActivity> scenario =
                ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(atividade::set);
            scenario.onActivity(a -> assertEquals(80000, audio().getDuracaoMs(), 30));
            salvarImagem("beatdays-duplas-avisos-inicio.png");
            tocar();
            buscar(4100);
            aviso(EntregaCafe.Aviso.MIKA, idEntrega(5000));
            pontos(0, 0);
            buscar(8100);
            aviso(EntregaCafe.Aviso.TIMER, idEntrega(9500));
            pontos(0, 0);
            buscar(12100);
            aviso(EntregaCafe.Aviso.RELOGIO, idEntrega(13000));
            cameraEstavel();
            pontos(0, 0);
            salvarImagem("beatdays-duplas-avisos-relogio-treino.png");
            buscar(12400);
            aviso(null, idEntrega(13000));
            cameraEstavel();
            buscar(13050);
            tocar();
            captura(idEntrega(13000));
            pontos(0, 0);
            buscar(13550);
            tocar();
            captura(idEntrega(13500));
            pontos(0, 0);
            buscar(20100);
            aviso(EntregaCafe.Aviso.MIKA, idEntrega(21000));
            cameraEstavel();
            buscar(21050);
            tocar();
            captura(idEntrega(21000));
            pontos(100, 1);
            buscar(24100);
            aviso(EntregaCafe.Aviso.RELOGIO, idEntrega(25000));
            cameraEstavel();
            pontos(100, 1);
            salvarImagem("beatdays-duplas-avisos-relogio.png");
            buscar(24400);
            aviso(null, idEntrega(25000));
            cameraEstavel();
            salvarImagem("beatdays-duplas-avisos-relogio-repouso.png");
            buscar(25050);
            tocar();
            captura(idEntrega(25000));
            pontos(200, 2);
            buscar(25550);
            tocar();
            captura(idEntrega(25500));
            pontos(300, 3);
            buscar(28120);
            aviso(EntregaCafe.Aviso.TIMER, idEntrega(29500));
            scenario.onActivity(
                    a -> assertEquals(1.03f, a.findViewById(R.id.cena_cafe).getScaleX(), .0001f));
            buscar(29550);
            tocar();
            captura(idEntrega(29500));
            pontos(400, 4);
            buscar(30050);
            tocar();
            captura(idEntrega(30000));
            pontos(500, 5);
            buscar(30550);
            tocar();
            captura(idEntrega(30500));
            pontos(600, 6);
        }
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

    private void aviso(EntregaCafe.Aviso esperado, int entrega) {
        InstrumentationRegistry.getInstrumentation()
                .runOnMainSync(
                        () -> {
                            EfeitosCafeView efeitos =
                                    atividade.get().findViewById(R.id.efeitos_cafe);
                            assertEquals(esperado, efeitos.getAvisoAtivo());
                            assertNull(motor().getJulgamento(entrega));
                            Bitmap alvo =
                                    Bitmap.createBitmap(
                                            efeitos.getWidth(),
                                            efeitos.getHeight(),
                                            Bitmap.Config.ARGB_8888);
                            efeitos.draw(new Canvas(alvo));
                            float u =
                                    Math.min(efeitos.getWidth() / 900f, efeitos.getHeight() / 400f);
                            int pixel =
                                    alvo.getPixel(
                                            Math.round(efeitos.getWidth() * .45f + 10 * u),
                                            Math.round(efeitos.getHeight() * .095f - 5 * u));
                            assertEquals(0, android.graphics.Color.alpha(pixel));
                            alvo.recycle();
                        });
    }

    private void captura(int entrega) {
        InstrumentationRegistry.getInstrumentation()
                .runOnMainSync(
                        () -> assertEquals(Julgamento.PERFECT, motor().getJulgamento(entrega)));
    }

    private void cameraEstavel() {
        InstrumentationRegistry.getInstrumentation()
                .runOnMainSync(
                        () -> {
                            View cena = atividade.get().findViewById(R.id.cena_cafe);
                            assertEquals(1f, cena.getScaleX(), .0001f);
                            assertEquals(1f, cena.getScaleY(), .0001f);
                            assertEquals(0f, cena.getTranslationX(), 0);
                            assertEquals(0f, cena.getTranslationY(), 0);
                        });
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

    private void tocar() {
        InstrumentationRegistry.getInstrumentation()
                .runOnMainSync(
                        () -> {
                            MainActivity a = atividade.get();
                            View raiz = a.findViewById(R.id.tela_jogo);
                            long agora = SystemClock.uptimeMillis();
                            for (int acao :
                                    new int[] {MotionEvent.ACTION_DOWN, MotionEvent.ACTION_UP}) {
                                MotionEvent e =
                                        MotionEvent.obtain(
                                                agora,
                                                SystemClock.uptimeMillis(),
                                                acao,
                                                raiz.getWidth() * .85f,
                                                raiz.getHeight() * .82f,
                                                0);
                                a.dispatchTouchEvent(e);
                                e.recycle();
                            }
                        });
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
        InstrumentationRegistry.getInstrumentation()
                .runOnMainSync(
                        () -> {
                            EfeitosCafeView efeitos =
                                    atividade.get().findViewById(R.id.efeitos_cafe);
                            Bitmap alvo =
                                    Bitmap.createBitmap(
                                            efeitos.getWidth(),
                                            efeitos.getHeight(),
                                            Bitmap.Config.ARGB_8888);
                            efeitos.draw(new Canvas(alvo));
                            alvo.recycle();
                        });
        Bitmap bitmap =
                InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
        assertNotNull(bitmap);
        File destino = new File(atividade.get().getExternalFilesDir(null), nome);
        try (FileOutputStream stream = new FileOutputStream(destino)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        }
        bitmap.recycle();
    }

    private int idEntrega(long alvo) {
        for (EntregaCafe e : com.example.beatdays.game.RoteiroRecepcao.criarEntregas()) {
            if (e.tempoMs == alvo) {
                return e.id;
            }
        }
        throw new AssertionError("Captura não encontrada em " + alvo);
    }
}
