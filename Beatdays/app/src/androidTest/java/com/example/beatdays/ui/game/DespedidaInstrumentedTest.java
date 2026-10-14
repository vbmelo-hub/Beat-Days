package com.example.beatdays.ui.game;

import static org.junit.Assert.assertArrayEquals;
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
public class DespedidaInstrumentedTest {
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
    public void despedidaResumePedidosAcertosPausaRecriacaoEResultadoSemJulgarToques()
            throws Exception {
        try (ActivityScenario<MainActivity> scenario =
                ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(atividade::set);
            tocar();
            for (EntregaCafe e : RoteiroRecepcao.criarEntregas()) {
                if (e.tempoMs >= RoteiroRecepcao.INICIO_PONTUACAO_MS) {
                    buscar((int) e.tempoMs + 30);
                    tocar();
                    pegou(e.tempoMs);
                }
            }
            pontos(3100, 31);
            buscar(74500);
            buscar(74950);
            scenario.onActivity(
                    a -> {
                        assertEquals(
                                CoreografiaCafe.Mika.ESPERA,
                                ((CenaCafe) a.findViewById(R.id.cena_cafe)).getEstadoMika());
                        assertEquals(0, a.findViewById(R.id.img_barista_despedida).getAlpha(), 0);
                        assertEquals(
                                "",
                                ((EfeitosCafeView) a.findViewById(R.id.efeitos_cafe))
                                        .getMensagemFechamento());
                    });
            buscar(75500);
            fechamento(14);
            estavel();
            salvarImagem("beatdays-despedida-completo.png");
            int[] julgamentos = motor().salvar();
            tocar();
            assertArrayEquals(julgamentos, motor().salvar());
            pontos(3100, 31);
            tocar(R.id.btn_pausar);
            long pausa = posicao();
            AtomicReference<Float> giro = new AtomicReference<>();
            scenario.onActivity(
                    a -> giro.set(a.findViewById(R.id.img_barista_despedida).getRotation()));
            SystemClock.sleep(200);
            assertEquals(pausa, posicao(), 30);
            scenario.onActivity(
                    a ->
                            assertEquals(
                                    giro.get(),
                                    a.findViewById(R.id.img_barista_despedida).getRotation(),
                                    0));
            scenario.recreate();
            scenario.onActivity(atividade::set);
            SystemClock.sleep(200);
            despedida();
            pontos(3100, 31);
            tocar(R.id.btn_pausar);
            buscar(75750);
            fechamento(14);
            finalizar();
            resultado(14);
            salvarImagem("beatdays-despedida-resultado-completo.png");
            scenario.recreate();
            scenario.onActivity(atividade::set);
            SystemClock.sleep(200);
            resultado(14);
            tocar();
            buscar(2000);
            pontos(0, 0);
            scenario.onActivity(
                    a -> {
                        assertEquals(
                                View.GONE,
                                a.findViewById(R.id.tv_pedidos_resultado).getVisibility());
                        assertEquals(0, a.findViewById(R.id.img_barista_despedida).getAlpha(), 0);
                        assertEquals(
                                "",
                                ((EfeitosCafeView) a.findViewById(R.id.efeitos_cafe))
                                        .getMensagemFechamento());
                    });
        }
    }

    @Test
    public void nenhumAlimentoRecebidoMostraZeroPedidosCompletos() throws Exception {
        try (ActivityScenario<MainActivity> scenario =
                ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(atividade::set);
            tocar();
            buscar(74000);
            buscar(75600);
            fechamento(0);
            pontos(0, 0);
            estavel();
            salvarImagem("beatdays-despedida-sem-acertos.png");
            int[] julgamentos = motor().salvar();
            tocar();
            assertArrayEquals(julgamentos, motor().salvar());
            finalizar();
            resultado(0);
            scenario.onActivity(
                    a ->
                            assertEquals(
                                    String.valueOf(0),
                                    ((TextView) a.findViewById(R.id.tv_menu_pontos))
                                            .getText()
                                            .toString()));
            salvarImagem("beatdays-despedida-resultado-sem-acertos.png");
        }
    }

    private void despedida() {
        InstrumentationRegistry.getInstrumentation()
                .runOnMainSync(
                        () -> {
                            MainActivity a = atividade.get();
                            CenaCafe cena = a.findViewById(R.id.cena_cafe);
                            assertEquals(CoreografiaCafe.Mika.DESPEDIDA, cena.getEstadoMika());
                            SpriteImageView idle = a.findViewById(R.id.img_barista);
                            float ancora = idle.getLeft() + idle.getWidth() * idle.getAncoraPes();
                            for (int recurso :
                                    new int[] {
                                        R.id.img_barista,
                                        R.id.img_barista_preparar,
                                        R.id.img_barista_lancar,
                                        R.id.img_barista_despedida
                                    }) {
                                SpriteImageView sprite = a.findViewById(recurso);
                                assertEquals(
                                        recurso == R.id.img_barista_despedida ? 1 : 0,
                                        sprite.getAlpha(),
                                        0);
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
                                                        a.findViewById(R.id.img_balcao_frente)));
                            }
                        });
    }

    private void fechamento(int completos) {
        despedida();
        InstrumentationRegistry.getInstrumentation()
                .runOnMainSync(
                        () -> {
                            MainActivity a = atividade.get();
                            EfeitosCafeView efeitos = a.findViewById(R.id.efeitos_cafe);
                            assertEquals(
                                    a.getString(R.string.fechamento_pedidos, completos, 14),
                                    efeitos.getMensagemFechamento());
                            assertEquals(
                                    View.GONE, a.findViewById(R.id.tv_instrucao).getVisibility());
                            assertEquals(
                                    View.GONE, a.findViewById(R.id.overlay_jogo).getVisibility());
                        });
    }

    private void resultado(int completos) {
        InstrumentationRegistry.getInstrumentation()
                .runOnMainSync(
                        () -> {
                            MainActivity a = atividade.get();
                            assertEquals(
                                    "",
                                    ((EfeitosCafeView) a.findViewById(R.id.efeitos_cafe))
                                            .getMensagemFechamento());
                            assertEquals(
                                    View.VISIBLE,
                                    a.findViewById(R.id.tv_pedidos_resultado).getVisibility());
                            assertEquals(
                                    a.getString(R.string.fechamento_pedidos, completos, 14),
                                    ((TextView) a.findViewById(R.id.tv_pedidos_resultado))
                                            .getText()
                                            .toString());
                            assertEquals(
                                    a.getString(R.string.turno_completo),
                                    ((TextView) a.findViewById(R.id.tv_overlay_titulo))
                                            .getText()
                                            .toString());
                        });
    }

    private void finalizar() {
        buscar(79000);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> audio().continuar());
        long limite = SystemClock.uptimeMillis() + 8000;
        AtomicReference<String> titulo = new AtomicReference<>("");
        while (SystemClock.uptimeMillis() < limite) {
            InstrumentationRegistry.getInstrumentation()
                    .runOnMainSync(
                            () ->
                                    titulo.set(
                                            ((TextView)
                                                            atividade
                                                                    .get()
                                                                    .findViewById(
                                                                            R.id.tv_overlay_titulo))
                                                    .getText()
                                                    .toString()));
            if (titulo.get().equals(atividade.get().getString(R.string.turno_completo))) {
                InstrumentationRegistry.getInstrumentation().waitForIdleSync();
                return;
            }
            SystemClock.sleep(30);
        }
        fail("A fase não terminou pelo fim do áudio.");
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
