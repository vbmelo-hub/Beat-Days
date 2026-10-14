package com.example.beatdays.ui.game;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.media.SoundPool;
import android.os.SystemClock;
import android.view.View;
import android.widget.TextView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.beatdays.MainActivity;
import com.example.beatdays.R;
import com.example.beatdays.audio.ControladorAudio;
import com.example.beatdays.audio.SonsCafe;
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
public class FoleyInstrumentedTest {
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
    public void quatroSonsCarregamEReproduzemSemSomDeAcertoParaMiss() throws Exception {
        AtomicReference<SonsCafe> sons = new AtomicReference<>();
        InstrumentationRegistry.getInstrumentation()
                .runOnMainSync(
                        () ->
                                sons.set(
                                        new SonsCafe(
                                                InstrumentationRegistry.getInstrumentation()
                                                        .getTargetContext())));
        try {
            AtomicLong prontos = new AtomicLong();
            long limite = SystemClock.uptimeMillis() + 4000;
            while (SystemClock.uptimeMillis() < limite && prontos.get() < 7) {
                InstrumentationRegistry.getInstrumentation()
                        .runOnMainSync(
                                () -> {
                                    try {
                                        Field f = SonsCafe.class.getDeclaredField("prontos");
                                        f.setAccessible(true);
                                        prontos.set(((java.util.Set<?>) f.get(sons.get())).size());
                                    } catch (ReflectiveOperationException e) {
                                        throw new AssertionError(e);
                                    }
                                });
                if (prontos.get() < 7) {
                    SystemClock.sleep(30);
                }
            }
            assertEquals(7, prontos.get());
            InstrumentationRegistry.getInstrumentation()
                    .runOnMainSync(
                            () -> {
                                try {
                                    Field f = SonsCafe.class.getDeclaredField("pool");
                                    f.setAccessible(true);
                                    SoundPool pool = (SoundPool) f.get(sons.get());
                                    for (EntregaCafe.Alimento alimento :
                                            EntregaCafe.Alimento.values()) {
                                        int stream = sons.get().acerto(Julgamento.GREAT, alimento);
                                        assertTrue(
                                                "SoundPool não reproduziu " + alimento, stream > 0);
                                        pool.stop(stream);
                                    }
                                    assertEquals(
                                            0,
                                            sons.get()
                                                    .acerto(
                                                            Julgamento.MISS,
                                                            EntregaCafe.Alimento.CAFE));
                                    assertEquals(
                                            0, sons.get().acerto(null, EntregaCafe.Alimento.CAFE));
                                    assertEquals(0, sons.get().acerto(Julgamento.PERFECT, null));
                                } catch (ReflectiveOperationException e) {
                                    throw new AssertionError(e);
                                }
                            });
        } finally {
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> sons.get().liberar());
        }
    }

    @Test
    public void contatoExigeEntradaEPausaReconstroiMesmoQuadroSemAlterarPontos() throws Exception {
        try (ActivityScenario<MainActivity> scenario =
                ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(atividade::set);
            tocar();
            buscar(20000);
            buscar(21000);
            InstrumentationRegistry.getInstrumentation()
                    .runOnMainSync(() -> assertNull(motor().getJulgamento(id(21000))));
            pontos(0, 0);
            buscar(21030);
            tocar();
            pontos(100, 1);
            buscar(21080);
            salvarImagem("beatdays-foley-contato.png");
            tocar(R.id.btn_pausar);
            long pausa = posicao();
            Bitmap antes = quadro();
            SystemClock.sleep(200);
            Bitmap parado = quadro();
            assertTrue(antes.sameAs(parado));
            parado.recycle();
            scenario.recreate();
            scenario.onActivity(atividade::set);
            esperarPosicao((int) pausa);
            Bitmap restaurado = quadro();
            assertTrue("Contato deve preservar o quadro na recriação", antes.sameAs(restaurado));
            antes.recycle();
            restaurado.recycle();
            pontos(100, 1);
            buscar(21200);
            Bitmap guardando = quadro();
            guardando.recycle();
            InstrumentationRegistry.getInstrumentation()
                    .runOnMainSync(
                            () ->
                                    assertEquals(
                                            1,
                                            ((EfeitosCafeView)
                                                            atividade
                                                                    .get()
                                                                    .findViewById(
                                                                            R.id.efeitos_cafe))
                                                    .getAlimentosEmGuarda()));
        }
    }

    private Bitmap quadro() {
        AtomicReference<Bitmap> captura = new AtomicReference<>();
        InstrumentationRegistry.getInstrumentation()
                .runOnMainSync(
                        () -> {
                            View efeitos = atividade.get().findViewById(R.id.efeitos_cafe);
                            Bitmap bitmap =
                                    Bitmap.createBitmap(
                                            efeitos.getWidth(),
                                            efeitos.getHeight(),
                                            Bitmap.Config.ARGB_8888);
                            efeitos.draw(new Canvas(bitmap));
                            captura.set(bitmap);
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
