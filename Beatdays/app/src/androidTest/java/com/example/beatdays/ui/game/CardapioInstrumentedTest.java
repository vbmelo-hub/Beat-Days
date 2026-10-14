package com.example.beatdays.ui.game;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import com.example.beatdays.model.EntregaCafe;
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
public class CardapioInstrumentedTest {
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
    public void spritesNovosAparecemEmVooENasCapturasComTimingOriginal() throws Exception {
        try (ActivityScenario<MainActivity> scenario =
                ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(atividade::set);
            scenario.onActivity(
                    a -> {
                        EfeitosCafeView efeitos = a.findViewById(R.id.efeitos_cafe);
                        try {
                            Field f = EfeitosCafeView.class.getDeclaredField("alimentos");
                            f.setAccessible(true);
                            Bitmap[] sprites = (Bitmap[]) f.get(efeitos);
                            assertEquals(4, sprites.length);
                            int[] recursos = {
                                R.drawable.cafe,
                                R.drawable.croissant,
                                R.drawable.panqueca,
                                R.drawable.cupcake
                            };
                            for (EntregaCafe.Alimento tipo : EntregaCafe.Alimento.values()) {
                                Bitmap esperado =
                                        BitmapFactory.decodeResource(
                                                a.getResources(), recursos[tipo.ordinal()]);
                                assertNotNull(sprites[tipo.ordinal()]);
                                assertTrue(sprites[tipo.ordinal()].sameAs(esperado));
                                assertTrue(sprites[tipo.ordinal()].hasAlpha());
                                assertTrue(
                                        "Margem opaca em " + tipo,
                                        (sprites[tipo.ordinal()].getPixel(0, 0) >>> 24) <= 16);
                                assertTrue(
                                        (sprites[tipo.ordinal()].getPixel(
                                                                sprites[tipo.ordinal()].getWidth()
                                                                        / 2,
                                                                sprites[tipo.ordinal()].getHeight()
                                                                        / 2)
                                                        >>> 24)
                                                > 128);
                                esperado.recycle();
                            }
                        } catch (ReflectiveOperationException e) {
                            throw new AssertionError(e);
                        }
                    });
            tocar();
            buscar(9400);
            salvarImagem("beatdays-cardapio-demo.png");
            buscar(29000);
            salvarImagem("beatdays-cardapio-panqueca-em-voo.png");
            buscar(29550);
            tocar();
            pontos(100, 1);
            salvarImagem("beatdays-cardapio-panqueca-capturada.png");
            buscar(30050);
            tocar();
            pontos(200, 2);
            salvarImagem("beatdays-cardapio-cupcake-capturado.png");
            buscar(30550);
            tocar();
            pontos(300, 3);
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
                            assertTrue(efeitos.getAlimentosEmVoo() > 0);
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
}
