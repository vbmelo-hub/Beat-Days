package com.example.beatdays.ui.game;

import static org.junit.Assert.assertEquals;
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
import com.example.beatdays.ui.game.sprite.SpriteImageView;
import com.example.beatdays.ui.game.support.GestosTeste;
import com.example.beatdays.ui.game.support.PosesTeste;
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
public class PreparoInstrumentedTest {
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
    public void preparaLancaRepousaPreservaAlinhamentoEPausa() throws Exception {
        try (ActivityScenario<MainActivity> scenario =
                ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(atividade::set);
            tocar();
            buscar(4010);
            pose(CoreografiaCafe.Mika.ESPERA);
            estavel();
            buscar(4120);
            pose(CoreografiaCafe.Mika.PREPARA);
            estavel();
            salvarImagem("beatdays-mika-preparo.png");
            tocar(R.id.btn_pausar);
            long pausa = posicao();
            SystemClock.sleep(150);
            assertEquals(pausa, posicao(), 30);
            scenario.recreate();
            scenario.onActivity(atividade::set);
            SystemClock.sleep(200);
            pose(CoreografiaCafe.Mika.PREPARA);
            estavel();
            scenario.onActivity(
                    a ->
                            assertEquals(
                                    View.VISIBLE,
                                    a.findViewById(R.id.overlay_jogo).getVisibility()));
            tocar(R.id.btn_pausar);
            buscar(4280);
            pose(CoreografiaCafe.Mika.LANCAR);
            estavel();
            salvarImagem("beatdays-mika-lancamento.png");
            buscar(4600);
            pose(CoreografiaCafe.Mika.ESPERA);
            estavel();
            salvarImagem("beatdays-mika-repouso.png");
            pontos(0, 0);
            buscar(12120);
            pose(CoreografiaCafe.Mika.PREPARA);
            estavel();
            buscar(12280);
            pose(CoreografiaCafe.Mika.LANCAR);
            buscar(12620);
            pose(CoreografiaCafe.Mika.PREPARA);
            estavel();
            buscar(12780);
            pose(CoreografiaCafe.Mika.LANCAR);
            buscar(13050);
            tocar();
            pegou(13000);
            buscar(13550);
            tocar();
            pegou(13500);
            pontos(0, 0);
            buscar(21050);
            tocar();
            pegou(21000);
            pontos(100, 1);
        }
    }

    private void pose(CoreografiaCafe.Mika estado) {
        InstrumentationRegistry.getInstrumentation()
                .runOnMainSync(
                        () -> {
                            PosesTeste.mika(atividade.get());
                            CenaCafe cena = atividade.get().findViewById(R.id.cena_cafe);
                            assertEquals(estado, cena.getEstadoMika());
                            SpriteImageView idle = atividade.get().findViewById(R.id.img_barista);
                            int[] ids = {
                                R.id.img_barista, R.id.img_barista_preparar, R.id.img_barista_lancar
                            };
                            CoreografiaCafe.Mika[] estados = {
                                CoreografiaCafe.Mika.ESPERA,
                                CoreografiaCafe.Mika.PREPARA,
                                CoreografiaCafe.Mika.LANCAR
                            };
                            float ancora = idle.getLeft() + idle.getWidth() * idle.getAncoraPes();
                            for (int i = 0; i < ids.length; i++) {
                                SpriteImageView sprite = atividade.get().findViewById(ids[i]);
                                assertTrue(sprite.getAlpha() >= 0 && sprite.getAlpha() <= 1);
                                if (estado == CoreografiaCafe.Mika.LANCAR) {
                                    assertEquals(
                                            estado == estados[i] ? 1f : 0f, sprite.getAlpha(), 0);
                                }
                                assertEquals(idle.getTop(), sprite.getTop());
                                assertEquals(idle.getBottom(), sprite.getBottom());
                                assertEquals(
                                        ancora,
                                        sprite.getLeft()
                                                + sprite.getWidth() * sprite.getAncoraPes(),
                                        1f);
                                assertTrue(
                                        cena.indexOfChild(sprite)
                                                < cena.indexOfChild(
                                                        atividade
                                                                .get()
                                                                .findViewById(
                                                                        R.id.img_balcao_frente)));
                            }
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
