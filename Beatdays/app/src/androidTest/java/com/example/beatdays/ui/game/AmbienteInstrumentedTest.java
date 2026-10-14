package com.example.beatdays.ui.game;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
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
import com.example.beatdays.ui.game.animation.CoreografiaCafe;
import com.example.beatdays.ui.game.support.GestosTeste;
import com.example.beatdays.ui.game.view.CenaCafe;
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
public class AmbienteInstrumentedTest {
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
    public void ambienteAvancaCongelaNaPausaERetomaSemInterceptarCaptura() throws Exception {
        try (ActivityScenario<MainActivity> scenario =
                ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(atividade::set);
            tocar();
            buscar(19000);
            buscar(20100);
            scenario.onActivity(
                    a -> {
                        CenaCafe cena = a.findViewById(R.id.cena_cafe);
                        View ambiente = a.findViewById(R.id.ambiente_cafe),
                                fundo = a.findViewById(R.id.img_fundo_cafe);
                        assertFalse(ambiente.isClickable());
                        assertFalse(ambiente.isFocusable());
                        assertEquals(fundo.getLeft(), ambiente.getLeft());
                        assertEquals(fundo.getTop(), ambiente.getTop());
                        assertEquals(fundo.getWidth(), ambiente.getWidth());
                        assertEquals(fundo.getHeight(), ambiente.getHeight());
                        assertTrue(cena.indexOfChild(ambiente) > cena.indexOfChild(fundo));
                        assertTrue(
                                cena.indexOfChild(ambiente)
                                        < cena.indexOfChild(a.findViewById(R.id.img_barista)));
                        assertTrue(
                                cena.indexOfChild(ambiente)
                                        < cena.indexOfChild(
                                                a.findViewById(R.id.img_balcao_frente)));
                    });
            Bitmap inicio = imagem(R.id.ambiente_cafe);
            assertTrue(temVapor(inicio));
            buscar(20400);
            Bitmap depois = imagem(R.id.ambiente_cafe);
            assertFalse("Vapor deve avançar junto da trilha", inicio.sameAs(depois));
            inicio.recycle();
            depois.recycle();
            tocar(R.id.btn_pausar);
            long pausa = posicao();
            Bitmap vapor = imagem(R.id.ambiente_cafe), relogio = imagem(R.id.efeitos_cafe);
            SystemClock.sleep(220);
            assertEquals(pausa, posicao(), 30);
            Bitmap congelado = imagem(R.id.ambiente_cafe), ponteiros = imagem(R.id.efeitos_cafe);
            assertTrue(vapor.sameAs(congelado));
            assertTrue(relogio.sameAs(ponteiros));
            congelado.recycle();
            ponteiros.recycle();
            scenario.recreate();
            scenario.onActivity(atividade::set);
            SystemClock.sleep(220);
            assertEquals(pausa, posicao(), 30);
            Bitmap restaurado = imagem(R.id.ambiente_cafe);
            assertTrue("Vapor deve reconstruir o mesmo quadro", vapor.sameAs(restaurado));
            vapor.recycle();
            relogio.recycle();
            restaurado.recycle();
            tocar(R.id.btn_pausar);
            buscar(21050);
            tocar();
            pegou(21000);
            pontos(100, 1);
            salvarImagem("beatdays-ambiente-captura.png");
            buscar(24100);
            aviso(1);
            estavel();
            salvarImagem("beatdays-ambiente-relogio.png");
            buscar(25050);
            tocar();
            pegou(25000);
            pontos(200, 2);
        }
    }

    private Bitmap imagem(int id) {
        AtomicReference<Bitmap> imagem = new AtomicReference<>();
        InstrumentationRegistry.getInstrumentation()
                .runOnMainSync(
                        () -> {
                            View v = atividade.get().findViewById(id);
                            Bitmap b =
                                    Bitmap.createBitmap(
                                            v.getWidth(), v.getHeight(), Bitmap.Config.ARGB_8888);
                            v.draw(new Canvas(b));
                            imagem.set(b);
                        });
        return imagem.get();
    }

    private boolean temVapor(Bitmap b) {
        int[] pixels = new int[b.getWidth() * b.getHeight()];
        b.getPixels(pixels, 0, b.getWidth(), 0, 0, b.getWidth(), b.getHeight());
        int visiveis = 0;
        for (int pixel : pixels) {
            if ((pixel >>> 24) > 0) {
                visiveis++;
            }
        }
        return visiveis > 0 && visiveis < pixels.length / 100;
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

    private void pegou(long tempo) {
        InstrumentationRegistry.getInstrumentation()
                .runOnMainSync(
                        () -> assertEquals(Julgamento.PERFECT, motor().getJulgamento(id(tempo))));
    }

    private void aviso(int numero) {
        InstrumentationRegistry.getInstrumentation()
                .runOnMainSync(
                        () -> {
                            EfeitosCafeView efeitos =
                                    atividade.get().findViewById(R.id.efeitos_cafe);
                            assertEquals(EntregaCafe.Aviso.RELOGIO, efeitos.getAvisoAtivo());
                            assertEquals(numero, efeitos.getNumeroAvisoAtivo());
                        });
    }

    private void estavel() {
        InstrumentationRegistry.getInstrumentation()
                .runOnMainSync(
                        () -> {
                            CenaCafe cena = atividade.get().findViewById(R.id.cena_cafe);
                            assertEquals(1, cena.getScaleX(), 0);
                            assertEquals(1, cena.getScaleY(), 0);
                            assertEquals(0, cena.getTranslationX(), 0);
                            assertEquals(0, cena.getTranslationY(), 0);
                            assertEquals(CoreografiaCafe.Paleta.NATURAL, cena.getPaleta());
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
                            if (nome.contains("dois-em-voo")) {
                                assertEquals(2, efeitos.getAlimentosEmVoo());
                            }
                            alvo.recycle();
                        });
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        SystemClock.sleep(150);
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
