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
import com.example.beatdays.ui.game.animation.AtendimentoCafe;
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
public class CelebracaoInstrumentedTest {
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
    public void pedidoCompletoAgradeceDepoisDeGuardarCongelaERestaura() throws Exception {
        try (ActivityScenario<MainActivity> scenario =
                ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(atividade::set);
            tocar();
            buscar(20000);
            buscar(21050);
            tocar();
            pegou(21000);
            pontos(100, 1);
            buscar(21220);
            reacao(false);
            buscar(21400);
            reacao(true);
            buscar(21570);
            reacao(true);
            salvarImagem("beatdays-celebracao-obrigada.png");
            tocar(R.id.btn_pausar);
            long pausa = posicao();
            Bitmap antes = areaCelebracao();
            SystemClock.sleep(200);
            Bitmap depois = areaCelebracao();
            assertTrue(antes.sameAs(depois));
            depois.recycle();
            scenario.recreate();
            scenario.onActivity(atividade::set);
            esperarPosicao((int) pausa);
            reacao(true);
            Bitmap restaurado = areaCelebracao();
            assertTrue("Agradecimento deve restaurar o mesmo quadro", antes.sameAs(restaurado));
            antes.recycle();
            restaurado.recycle();
            tocar(R.id.btn_pausar);
            buscar(21760);
            reacao(false);
            buscar(22100);
            reacao(false);
            buscar(24100);
            reacao(false);
            buscar(25050);
            tocar();
            buscar(25400);
            reacao(false);
            buscar(25800);
            reacao(false);
            buscar(26100);
            reacao(false);
            pontos(200, 0);
            InstrumentationRegistry.getInstrumentation()
                    .runOnMainSync(
                            () ->
                                    assertEquals(
                                            AtendimentoCafe.Desfecho.PARCIAL,
                                            ((CenaCafe)
                                                            atividade
                                                                    .get()
                                                                    .findViewById(R.id.cena_cafe))
                                                    .getAtendimento()
                                                    .getDesfecho()));
        }
    }

    @Test
    public void trioAgradeceSoAposUltimoItemEMissFisicoCancela() throws Exception {
        try (ActivityScenario<MainActivity> scenario =
                ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(atividade::set);
            tocar();
            buscar(55000);
            for (int n = 0; n < 3; n++) {
                buscar(57050 + n * 250);
                tocar();
                pegou(57000 + n * 250);
                reacao(false);
            }
            buscar(57800);
            reacao(false);
            buscar(58070);
            reacao(true);
            pontos(300, 3);
            salvarImagem("beatdays-celebracao-trio.png");
            tocar();
            buscar(58120);
            reacao(false);
            pontos(300, 0);
            InstrumentationRegistry.getInstrumentation()
                    .runOnMainSync(
                            () -> {
                                CenaCafe cena = atividade.get().findViewById(R.id.cena_cafe);
                                assertEquals(CoreografiaCafe.Cliente.ERRA, cena.getEstadoCliente());
                                assertEquals(CoreografiaCafe.Paleta.CINZA, cena.getPaleta());
                            });
            buscar(58300);
            reacao(false);
            buscar(59600);
            reacao(false);
        }
    }

    private void reacao(boolean esperada) {
        InstrumentationRegistry.getInstrumentation()
                .runOnMainSync(
                        () -> {
                            CenaCafe cena = atividade.get().findViewById(R.id.cena_cafe);
                            assertEquals(esperada, cena.getProgressoCelebracao() >= 0);
                            assertEquals(0, cena.getTranslationX(), 0);
                            assertEquals(0, cena.getTranslationY(), 0);
                            if (esperada) {
                                assertEquals(
                                        AtendimentoCafe.Desfecho.COMPLETO,
                                        cena.getAtendimento().getDesfecho());
                                assertEquals(
                                        AtendimentoCafe.Etapa.DESPEDIDA,
                                        cena.getAtendimento().getEtapa());
                                assertEquals(1, cena.getScaleX(), 0);
                                assertEquals(1, cena.getScaleY(), 0);
                                assertEquals(CoreografiaCafe.Paleta.NATURAL, cena.getPaleta());
                            }
                        });
    }

    private Bitmap areaCelebracao() {
        AtomicReference<Bitmap> imagem = new AtomicReference<>();
        InstrumentationRegistry.getInstrumentation()
                .runOnMainSync(
                        () -> {
                            View v = atividade.get().findViewById(R.id.efeitos_cafe);
                            Bitmap inteiro =
                                    Bitmap.createBitmap(
                                            v.getWidth(), v.getHeight(), Bitmap.Config.ARGB_8888);
                            v.draw(new Canvas(inteiro));
                            int top = Math.round(v.getHeight() * .14f),
                                    altura = Math.round(v.getHeight() * .18f);
                            imagem.set(
                                    Bitmap.createBitmap(
                                            inteiro,
                                            0,
                                            top,
                                            Math.round(v.getWidth() * .40f),
                                            altura));
                            inteiro.recycle();
                        });
        return imagem.get();
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
