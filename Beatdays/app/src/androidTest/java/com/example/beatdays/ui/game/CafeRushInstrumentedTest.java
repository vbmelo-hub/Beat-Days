package com.example.beatdays.ui.game;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.app.Instrumentation;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.beatdays.MainActivity;
import com.example.beatdays.R;
import com.example.beatdays.audio.ControladorAudio;
import com.example.beatdays.ui.game.animation.CoreografiaCafe;
import com.example.beatdays.ui.game.view.CenaCafe;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(AndroidJUnit4.class)
public class CafeRushInstrumentedTest {
    private final Instrumentation instrumentacao = InstrumentationRegistry.getInstrumentation();
    private final AtomicReference<MainActivity> atividade = new AtomicReference<>();
    private long inicioToque;
    private int alvoToque;

    @Test
    public void demonstracaoNaoPontuaEAvisoNaoSubstituiCaptura() throws Exception {
        try (ActivityScenario<MainActivity> scenario =
                ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(atividade::set);
            scenario.onActivity(a -> assertEquals(80000, audio().getDuracaoMs(), 30));
            tocar();
            posicionarRelogio(10100);
            verificarPontos(0, 0);
            scenario.onActivity(
                    a -> {
                        CharSequence resposta =
                                a.findViewById(R.id.efeitos_cafe).getContentDescription();
                        assertTrue(resposta == null || resposta.length() == 0);
                    });
            alvoToque = R.id.tv_score;
            posicionarRelogio(12050);
            pressionar();
            soltar();
            scenario.onActivity(
                    a ->
                            assertEquals(
                                    a.getString(R.string.fora_do_ritmo),
                                    a.findViewById(R.id.efeitos_cafe).getContentDescription()));
            posicionarRelogio(13050);
            pressionar();
            soltar();
            verificarPontos(0, 0);
            scenario.onActivity(
                    a -> {
                        assertEquals(
                                "Recepção: tempo="
                                        + audio().getPosicaoAtualMs()
                                        + " resposta="
                                        + a.findViewById(R.id.efeitos_cafe).getContentDescription(),
                                1f,
                                a.findViewById(R.id.img_cliente_pegar).getAlpha(),
                                .001f);
                        assertEquals(0f, a.findViewById(R.id.img_cliente).getAlpha(), .001f);
                    });
        }
    }

    @Test
    public void capturasMultiplasPausaRecriacaoResultadoEReplay() throws Exception {
        try (ActivityScenario<MainActivity> scenario =
                ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(atividade::set);
            scenario.onActivity(
                    a -> {
                        View cena = a.findViewById(R.id.cena_cafe),
                                raiz = a.findViewById(R.id.tela_jogo);
                        assertEquals(
                                Configuration.ORIENTATION_LANDSCAPE,
                                a.getResources().getConfiguration().orientation);
                        assertEquals(raiz.getWidth(), cena.getWidth());
                        assertEquals(raiz.getHeight(), cena.getHeight());
                    });
            tocar();
            alvoToque = R.id.tv_score;
            gestoNoTempo(21000, MotionEvent.ACTION_DOWN);
            soltar();
            gestoNoTempo(25000, MotionEvent.ACTION_DOWN);
            soltar();
            gestoNoTempo(25500, MotionEvent.ACTION_DOWN);
            soltar();
            for (long tempo : new long[] {29500, 30000, 30500}) {
                gestoNoTempo(tempo, MotionEvent.ACTION_DOWN);
                soltar();
            }
            verificarFaixaDePontos(240, 600, 6);
            salvarImagem();
            tocarPause();
            long pausado = posicao();
            SystemClock.sleep(180);
            assertEquals(pausado, posicao(), 35);
            scenario.recreate();
            scenario.onActivity(atividade::set);
            SystemClock.sleep(150);
            verificarFaixaDePontos(240, 600, 6);
            scenario.onActivity(
                    a ->
                            assertEquals(
                                    View.VISIBLE,
                                    a.findViewById(R.id.overlay_jogo).getVisibility()));
            tocarPause();
            gestoNoTempo(33000, MotionEvent.ACTION_DOWN);
            soltar();
            verificarFaixaDePontos(280, 700, 7);
            scenario.moveToState(Lifecycle.State.CREATED);
            pausado = posicao();
            SystemClock.sleep(150);
            assertEquals(pausado, posicao(), 35);
            scenario.moveToState(Lifecycle.State.RESUMED);
            scenario.onActivity(atividade::set);
            scenario.onActivity(
                    a ->
                            assertEquals(
                                    View.VISIBLE,
                                    a.findViewById(R.id.overlay_jogo).getVisibility()));
            tocarPause();
            instrumentacao.runOnMainSync(() -> audio().definirPosicaoMs(79000));
            long limite = SystemClock.uptimeMillis() + 8000;
            AtomicReference<String> titulo = new AtomicReference<>("");
            while (SystemClock.uptimeMillis() < limite) {
                scenario.onActivity(
                        a ->
                                titulo.set(
                                        ((TextView) a.findViewById(R.id.tv_overlay_titulo))
                                                .getText()
                                                .toString()));
                if (titulo.get().equals(atividade.get().getString(R.string.turno_completo))) {
                    break;
                }
                SystemClock.sleep(30);
            }
            assertEquals(atividade.get().getString(R.string.turno_completo), titulo.get());
            tocar();
            SystemClock.sleep(150);
            verificarPontos(0, 0);
            assertTrue(posicao() < 2000);
        }
    }

    @Test
    public void lancamentoRecepcaoErroZoomNosTriosECameraEstavel() throws Exception {
        try (ActivityScenario<MainActivity> scenario =
                ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(atividade::set);
            tocar();
            alvoToque = R.id.tv_score;
            posicionarRelogio(12280);
            scenario.onActivity(
                    a -> {
                        CenaCafe cena = a.findViewById(R.id.cena_cafe);
                        assertEquals(CoreografiaCafe.Mika.LANCAR, cena.getEstadoMika());
                        assertEquals(1f, a.findViewById(R.id.img_barista_lancar).getAlpha(), .001f);
                        assertEquals(0f, a.findViewById(R.id.img_barista).getAlpha(), .001f);
                    });
            salvarImagem("beatdays-animacao-lancar.png");
            posicionarRelogio(13050);
            pressionar();
            soltar();
            scenario.onActivity(
                    a ->
                            assertEquals(
                                    CoreografiaCafe.Cliente.PEGA,
                                    ((CenaCafe) a.findViewById(R.id.cena_cafe))
                                            .getEstadoCliente()));
            salvarImagem("beatdays-animacao-pegar.png");
            posicionarRelogio(17800);
            scenario.onActivity(
                    a -> {
                        assertEquals(
                                CoreografiaCafe.Cliente.ERRA,
                                ((CenaCafe) a.findViewById(R.id.cena_cafe)).getEstadoCliente());
                        assertEquals(
                                CoreografiaCafe.Paleta.CINZA,
                                ((CenaCafe) a.findViewById(R.id.cena_cafe)).getPaleta());
                        assertEquals(1f, a.findViewById(R.id.img_cliente_erro).getAlpha(), .001f);
                        assertEquals(
                                0f, a.findViewById(R.id.img_cliente_receber).getAlpha(), .001f);
                    });
            salvarImagem("beatdays-trios-miss.png");
            posicionarRelogio(18090);
            scenario.onActivity(
                    a -> {
                        CenaCafe cena = a.findViewById(R.id.cena_cafe);
                        assertEquals(CoreografiaCafe.Cliente.ERRA, cena.getEstadoCliente());
                        assertEquals(CoreografiaCafe.Paleta.CINZA, cena.getPaleta());
                        assertEquals(1f, a.findViewById(R.id.img_cliente_erro).getAlpha(), 0);
                    });
            salvarImagem("beatdays-resposta-miss.png");
            posicionarRelogio(25050);
            pressionar();
            soltar();
            scenario.onActivity(
                    a -> {
                        CenaCafe cena = a.findViewById(R.id.cena_cafe);
                        assertEquals(1f, cena.getScaleX(), 0);
                        assertEquals(1f, cena.getScaleY(), 0);
                        assertEquals(0f, cena.getTranslationX(), 0);
                        assertEquals(0f, cena.getTranslationY(), 0);
                        assertEquals(CoreografiaCafe.Paleta.NATURAL, cena.getPaleta());
                    });
            salvarImagem("beatdays-camera-estavel.png");
            for (int tempo : new int[] {25100, 25250, 25450, 25500}) {
                posicionarRelogio(tempo);
                scenario.onActivity(
                        a -> {
                            View cena = a.findViewById(R.id.cena_cafe);
                            assertEquals(1f, cena.getScaleX(), 0);
                            assertEquals(1f, cena.getScaleY(), 0);
                            assertEquals(0f, cena.getTranslationX(), 0);
                            assertEquals(0f, cena.getTranslationY(), 0);
                        });
            }
            posicionarRelogio(55000);
            int[] tempos = {56120, 56370, 56620};
            String[] nomes = {"mel", "coral", "petroleo"};
            CoreografiaCafe.Paleta[] cores = {
                CoreografiaCafe.Paleta.MEL,
                CoreografiaCafe.Paleta.CORAL,
                CoreografiaCafe.Paleta.PETROLEO
            };
            for (int i = 0; i < tempos.length; i++) {
                posicionarRelogio(tempos[i]);
                final CoreografiaCafe.Paleta cor = cores[i];
                final float escala = 1f + (i + 1) * .03f;
                scenario.onActivity(
                        a -> {
                            CenaCafe cena = a.findViewById(R.id.cena_cafe);
                            View hud = a.findViewById(R.id.hud);
                            assertEquals(cor, cena.getPaleta());
                            assertEquals(escala, cena.getScaleX(), .00001f);
                            assertEquals(0f, cena.getTranslationX(), 0);
                            assertEquals(0f, cena.getTranslationY(), 0);
                            assertEquals(1f, hud.getScaleX(), 0);
                            assertEquals(0f, hud.getTranslationX(), 0);
                            assertEquals(1f, hud.getAlpha(), 0);
                        });
                salvarImagem("beatdays-resposta-bip-" + (i + 1) + ".png");
            }
            CoreografiaCafe.Paleta[] respostas = {
                CoreografiaCafe.Paleta.PESSEGO,
                CoreografiaCafe.Paleta.LILAS,
                CoreografiaCafe.Paleta.SALVIA
            };
            for (int i = 0; i < 3; i++) {
                posicionarRelogio(57050 + i * 250);
                pressionar();
                soltar();
                posicionarRelogio(57120 + i * 250);
                final CoreografiaCafe.Paleta cor = respostas[i];
                final float escala = 1f + (2 - i) * .03f;
                scenario.onActivity(
                        a -> {
                            CenaCafe cena = a.findViewById(R.id.cena_cafe);
                            assertEquals(CoreografiaCafe.Cliente.PEGA, cena.getEstadoCliente());
                            assertEquals(cor, cena.getPaleta());
                            assertEquals(escala, cena.getScaleX(), .00001f);
                        });
                salvarImagem("beatdays-resposta-toque-" + (i + 1) + ".png");
            }
            AtomicReference<Float> zoom = new AtomicReference<>();
            scenario.onActivity(a -> zoom.set(a.findViewById(R.id.cena_cafe).getScaleX()));
            tocarPause();
            long pausado = posicao();
            SystemClock.sleep(200);
            scenario.onActivity(
                    a -> {
                        assertEquals(zoom.get(), a.findViewById(R.id.cena_cafe).getScaleX(), 0);
                        assertEquals(
                                CoreografiaCafe.Paleta.SALVIA,
                                ((CenaCafe) a.findViewById(R.id.cena_cafe)).getPaleta());
                    });
            assertEquals(pausado, posicao(), 35);
            scenario.recreate();
            scenario.onActivity(atividade::set);
            SystemClock.sleep(300);
            scenario.onActivity(
                    a -> {
                        assertEquals(
                                View.VISIBLE, a.findViewById(R.id.overlay_jogo).getVisibility());
                        assertEquals(
                                CoreografiaCafe.Paleta.SALVIA,
                                ((CenaCafe) a.findViewById(R.id.cena_cafe)).getPaleta());
                    });
            posicionarRelogio(58200);
            scenario.onActivity(
                    a -> {
                        CenaCafe cena = a.findViewById(R.id.cena_cafe);
                        assertEquals(1f, cena.getScaleX(), 0);
                        assertEquals(CoreografiaCafe.Paleta.NATURAL, cena.getPaleta());
                    });
        }
    }

    @Test
    public void trioLentoSeparaAvisosEZoomOutDependeDoToque() throws Exception {
        try (ActivityScenario<MainActivity> scenario =
                ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(atividade::set);
            tocar();
            alvoToque = R.id.tv_score;
            posicionarRelogio(15500);
            for (int n = 0; n < 3; n++) {
                posicionarRelogio(16120 + n * 500);
                final float escala = 1f + (n + 1) * .03f;
                scenario.onActivity(
                        a ->
                                assertEquals(
                                        escala,
                                        a.findViewById(R.id.cena_cafe).getScaleX(),
                                        .00001f));
            }
            salvarImagem("beatdays-turnos-lento-bip-3.png");
            posicionarRelogio(17350);
            scenario.onActivity(
                    a -> assertEquals(1.09f, a.findViewById(R.id.cena_cafe).getScaleX(), .00001f));
            posicionarRelogio(17580);
            scenario.onActivity(
                    a -> assertEquals(1.09f, a.findViewById(R.id.cena_cafe).getScaleX(), .00001f));
            pressionar();
            soltar();
            posicionarRelogio(17690);
            scenario.onActivity(
                    a -> assertEquals(1.06f, a.findViewById(R.id.cena_cafe).getScaleX(), .00001f));
            salvarImagem("beatdays-turnos-lento-toque-1.png");
            posicionarRelogio(18100);
            scenario.onActivity(
                    a -> assertEquals(1.06f, a.findViewById(R.id.cena_cafe).getScaleX(), .00001f));
            pressionar();
            soltar();
            posicionarRelogio(18210);
            scenario.onActivity(
                    a -> assertEquals(1.03f, a.findViewById(R.id.cena_cafe).getScaleX(), .00001f));
            tocarPause();
            scenario.recreate();
            scenario.onActivity(atividade::set);
            SystemClock.sleep(300);
            scenario.onActivity(
                    a -> assertEquals(1.03f, a.findViewById(R.id.cena_cafe).getScaleX(), .00001f));
            tocarPause();
            posicionarRelogio(18590);
            pressionar();
            soltar();
            posicionarRelogio(18700);
            scenario.onActivity(
                    a -> assertEquals(1f, a.findViewById(R.id.cena_cafe).getScaleX(), 0));
            posicionarRelogio(55000);
            posicionarRelogio(56620);
            for (int t : new int[] {57050, 57300, 57690}) {
                posicionarRelogio(t);
                scenario.onActivity(
                        a ->
                                assertEquals(
                                        1.09f,
                                        a.findViewById(R.id.cena_cafe).getScaleX(),
                                        .00001f));
            }
            salvarImagem("beatdays-turnos-sem-toque.png");
            posicionarRelogio(57920);
            scenario.onActivity(
                    a -> assertEquals(1f, a.findViewById(R.id.cena_cafe).getScaleX(), 0));
            salvarImagem("beatdays-turnos-retorno.png");
        }
    }

    @Test
    public void missBloqueiaSpamPreservaPenalidadeAoPausarELiberaAcertosRapidos() throws Exception {
        try (ActivityScenario<MainActivity> scenario =
                ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(atividade::set);
            tocar();
            alvoToque = R.id.tv_score;
            posicionarRelogio(20400);
            posicionarRelogio(20770);
            pressionar();
            soltar();
            AtomicLong prazo = new AtomicLong();
            scenario.onActivity(
                    a -> {
                        prazo.set(motor().getBloqueadoAteMs());
                        assertTrue(motor().estaEmRecuperacao(21000));
                        assertEquals(
                                CoreografiaCafe.Cliente.ERRA,
                                ((CenaCafe) a.findViewById(R.id.cena_cafe)).getEstadoCliente());
                    });
            posicionarRelogio(21000);
            for (int n = 0; n < 15; n++) {
                pressionar();
                soltar();
            }
            verificarPontos(0, 0);
            scenario.onActivity(
                    a -> {
                        assertEquals(prazo.get(), motor().getBloqueadoAteMs());
                        assertNull(motor().getJulgamento(idEntrega(21000)));
                        assertEquals(
                                CoreografiaCafe.Paleta.CINZA,
                                ((CenaCafe) a.findViewById(R.id.cena_cafe)).getPaleta());
                    });
            tocarPause();
            long parado = posicao();
            SystemClock.sleep(200);
            assertEquals(parado, posicao(), 35);
            scenario.recreate();
            scenario.onActivity(atividade::set);
            SystemClock.sleep(300);
            scenario.onActivity(
                    a -> {
                        assertEquals(prazo.get(), motor().getBloqueadoAteMs());
                        assertEquals(
                                CoreografiaCafe.Cliente.ERRA,
                                ((CenaCafe) a.findViewById(R.id.cena_cafe)).getEstadoCliente());
                        assertEquals(
                                CoreografiaCafe.Paleta.CINZA,
                                ((CenaCafe) a.findViewById(R.id.cena_cafe)).getPaleta());
                    });
            tocarPause();
            posicionarRelogio(21200);
            pressionar();
            soltar();
            verificarPontos(0, 0);
            posicionarRelogio(21400);
            scenario.onActivity(
                    a -> assertFalse(motor().estaEmRecuperacao(audio().getPosicaoAtualMs())));
            posicionarRelogio(25050);
            pressionar();
            soltar();
            verificarFaixaDePontos(40, 100, 1);
            posicionarRelogio(55000);
            for (int n = 0; n < 3; n++) {
                posicionarRelogio(57050 + n * 250);
                pressionar();
                soltar();
            }
            verificarFaixaDePontos(160, 400, 3);
            scenario.onActivity(
                    a -> {
                        for (int indice = 0; indice < 3; indice++) {
                            int id = idEntrega(57000 + indice * 250);
                            assertNotNull(motor().getJulgamento(id));
                            assertNotEquals(
                                    com.example.beatdays.model.Julgamento.MISS,
                                    motor().getJulgamento(id));
                        }
                    });
        }
    }

    @Test
    public void resultadoContaToquesErradosAcertosEPerdasSemDuplicarSpam() throws Exception {
        try (ActivityScenario<MainActivity> scenario =
                ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(atividade::set);
            tocar();
            alvoToque = R.id.tv_score;
            posicionarRelogio(12050);
            pressionar();
            soltar();
            posicionarRelogio(13050);
            pressionar();
            soltar();
            verificarJulgamentos(0, 0, 0, 0);
            posicionarRelogio(20400);
            posicionarRelogio(21050);
            pressionar();
            soltar();
            verificarJulgamentos(1, 0, 0, 0);
            posicionarRelogio(22000);
            pressionar();
            soltar();
            for (int n = 0; n < 15; n++) {
                pressionar();
                soltar();
            }
            verificarJulgamentos(1, 0, 0, 1);
            tocarPause();
            scenario.recreate();
            scenario.onActivity(atividade::set);
            verificarJulgamentos(1, 0, 0, 1);
            tocarPause();
            posicionarRelogio(25100);
            pressionar();
            soltar();
            posicionarRelogio(29670);
            pressionar();
            soltar();
            verificarJulgamentos(1, 1, 1, 2);
            verificarPontos(210, 1);
            posicionarRelogio(79000);
            verificarJulgamentos(1, 1, 1, 29);
            instrumentacao.runOnMainSync(() -> audio().continuar());
            long limite = SystemClock.uptimeMillis() + 8000;
            AtomicReference<String> titulo = new AtomicReference<>("");
            while (SystemClock.uptimeMillis() < limite) {
                scenario.onActivity(
                        a ->
                                titulo.set(
                                        ((TextView) a.findViewById(R.id.tv_overlay_titulo))
                                                .getText()
                                                .toString()));
                if (titulo.get().equals(atividade.get().getString(R.string.turno_completo))) {
                    break;
                }
                SystemClock.sleep(30);
            }
            assertEquals(atividade.get().getString(R.string.turno_completo), titulo.get());
            scenario.onActivity(
                    a ->
                            assertEquals(
                                    String.valueOf(210),
                                    ((TextView) a.findViewById(R.id.tv_menu_pontos))
                                            .getText()
                                            .toString()));
            salvarImagem("beatdays-resultado-detalhado.png");
            scenario.recreate();
            scenario.onActivity(atividade::set);
            verificarJulgamentos(1, 1, 1, 29);
            scenario.onActivity(
                    a ->
                            assertEquals(
                                    String.valueOf(210),
                                    ((TextView) a.findViewById(R.id.tv_menu_pontos))
                                            .getText()
                                            .toString()));
            tocar();
            verificarJulgamentos(0, 0, 0, 0);
            verificarPontos(0, 0);
        }
    }

    private void verificarJulgamentos(int perfects, int greats, int goods, int misses) {
        instrumentacao.runOnMainSync(
                () -> {
                    String contexto = "tempo=" + audio().getPosicaoAtualMs() + " ";
                    assertEquals(contexto + "PERFECT", perfects, contador("perfects"));
                    assertEquals(contexto + "GREAT", greats, contador("greats"));
                    assertEquals(contexto + "GOOD", goods, contador("goods"));
                    assertEquals(contexto + "MISS", misses, contador("misses"));
                });
    }

    private int contador(String nome) {
        try {
            Field f = MainActivity.class.getDeclaredField(nome);
            f.setAccessible(true);
            return f.getInt(atividade.get());
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    private com.example.beatdays.game.MotorRecepcao motor() {
        try {
            Field f = MainActivity.class.getDeclaredField("motor");
            f.setAccessible(true);
            return (com.example.beatdays.game.MotorRecepcao) f.get(atividade.get());
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
        AtomicLong valor = new AtomicLong();
        instrumentacao.runOnMainSync(() -> valor.set(audio().getPosicaoAtualMs()));
        return valor.get();
    }

    private void posicionarRelogio(int tempo) {
        instrumentacao.runOnMainSync(
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

    private void pressionar() {
        inicioToque = SystemClock.uptimeMillis();
        enviar(MotionEvent.ACTION_DOWN);
    }

    private void soltar() {
        enviar(MotionEvent.ACTION_UP);
    }

    private void tocar() {
        pressionar();
        SystemClock.sleep(20);
        soltar();
    }

    private void tocarPause() {
        int anterior = alvoToque;
        alvoToque = R.id.btn_pausar;
        tocar();
        alvoToque = anterior;
    }

    private void gestoNoTempo(long alvo, int acao) {
        long limite = SystemClock.uptimeMillis() + 35000;
        AtomicLong executado = new AtomicLong(-1);
        while (SystemClock.uptimeMillis() < limite && executado.get() < 0) {
            instrumentacao.runOnMainSync(
                    () -> {
                        long atual = audio().getPosicaoAtualMs();
                        if (atual >= alvo) {
                            assertTrue(
                                    "Entrada atrasada: " + atual + " / " + alvo,
                                    atual - alvo <= 200);
                            if (alvo == 29500) {
                                assertTrue(
                                        "O triplo precisa de alimentos independentes em voo",
                                        ((com.example.beatdays.ui.game.view.EfeitosCafeView)
                                                                atividade
                                                                        .get()
                                                                        .findViewById(
                                                                                R.id.efeitos_cafe))
                                                        .getAlimentosEmVoo()
                                                >= 2);
                            }
                            if (acao == MotionEvent.ACTION_DOWN) {
                                inicioToque = SystemClock.uptimeMillis();
                            }
                            enviarNaUi(acao);
                            executado.set(atual);
                        }
                    });
            if (executado.get() < 0) {
                SystemClock.sleep(5);
            }
        }
        assertTrue("A reprodução não alcançou o gesto em " + alvo, executado.get() >= 0);
    }

    private void enviar(int acao) {
        instrumentacao.runOnMainSync(() -> enviarNaUi(acao));
    }

    private void enviarNaUi(int acao) {
        View raiz = atividade.get().findViewById(R.id.tela_jogo);
        float x = raiz.getWidth() * .85f, y = raiz.getHeight() * .82f;
        if (alvoToque != 0) {
            View alvo = atividade.get().findViewById(alvoToque);
            Rect area = new Rect(0, 0, alvo.getWidth(), alvo.getHeight());
            ((android.view.ViewGroup) raiz).offsetDescendantRectToMyCoords(alvo, area);
            x = area.centerX();
            y = area.centerY();
        }
        MotionEvent evento =
                MotionEvent.obtain(inicioToque, SystemClock.uptimeMillis(), acao, x, y, 0);
        atividade.get().dispatchTouchEvent(evento);
        evento.recycle();
    }

    private void salvarImagem() throws Exception {
        salvarImagem("beatdays-animacao-jogando.png");
    }

    private void salvarImagem(String nome) throws Exception {
        Bitmap bitmap = instrumentacao.getUiAutomation().takeScreenshot();
        assertNotNull(bitmap);
        File destino = new File(instrumentacao.getTargetContext().getExternalFilesDir(null), nome);
        try (FileOutputStream stream = new FileOutputStream(destino)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        }
        bitmap.recycle();
    }

    private void verificarFaixaDePontos(int minimo, int maximo, int combo) {
        instrumentacao.runOnMainSync(
                () -> {
                    MainActivity a = atividade.get();
                    String texto = ((TextView) a.findViewById(R.id.tv_score)).getText().toString();
                    int pontos = Integer.parseInt(texto.replaceAll("[^0-9]", ""));
                    assertTrue(
                            "Pontuação fora da faixa: " + pontos,
                            pontos >= minimo && pontos <= maximo);
                    assertEquals(
                            a.getString(R.string.combo, combo),
                            ((TextView) a.findViewById(R.id.tv_combo)).getText().toString());
                });
    }

    private void verificarPontos(int score, int combo) {
        instrumentacao.runOnMainSync(
                () -> {
                    MainActivity a = atividade.get();
                    assertEquals(
                            a.getString(R.string.score, score),
                            ((TextView) a.findViewById(R.id.tv_score)).getText().toString());
                    assertEquals(
                            a.getString(R.string.combo, combo),
                            ((TextView) a.findViewById(R.id.tv_combo)).getText().toString());
                });
    }

    private int idEntrega(long alvo) {
        for (com.example.beatdays.model.EntregaCafe e :
                com.example.beatdays.game.RoteiroRecepcao.criarEntregas()) {
            if (e.tempoMs == alvo) {
                return e.id;
            }
        }
        throw new AssertionError("Captura não encontrada em " + alvo);
    }
}
