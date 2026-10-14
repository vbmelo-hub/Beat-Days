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
public class LancamentoInstrumentedTest {
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
    public void preparoApareceAntesDoVooCongelaERestauraSemJulgar() throws Exception {
        try (ActivityScenario<MainActivity> scenario =
                ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(atividade::set);
            tocar();
            buscar(20000);
            buscar(20040);
            alimentos(0, 0);
            buscar(20120);
            alimentos(1, 0);
            mika(CoreografiaCafe.Mika.PREPARA);
            pontos(0, 0);
            semJulgamento(21000);
            salvarImagem("beatdays-lancamento-preparo.png");
            tocar(R.id.btn_pausar);
            Bitmap antes = areaPreparo();
            long pausa = posicao();
            SystemClock.sleep(200);
            Bitmap depois = areaPreparo();
            assertTrue(antes.sameAs(depois));
            depois.recycle();
            assertEquals(pausa, posicao(), 30);
            scenario.recreate();
            scenario.onActivity(atividade::set);
            esperarPosicao((int) pausa);
            Bitmap restaurado = areaPreparo();
            assertTrue(
                    "Preparo deve reconstruir o mesmo quadro em "
                            + pausa
                            + ", relógio="
                            + posicao(),
                    antes.sameAs(restaurado));
            antes.recycle();
            restaurado.recycle();
            alimentos(1, 0);
            semJulgamento(21000);
            tocar(R.id.btn_pausar);
            buscar(20240);
            alimentos(1, 0);
            buscar(20260);
            alimentos(0, 1);
            mika(CoreografiaCafe.Mika.LANCAR);
            buscar(20500);
            alimentos(0, 1);
            salvarImagem("beatdays-lancamento-voo.png");
            buscar(21050);
            tocar();
            pegou(21000);
            alimentos(0, 0);
            pontos(100, 1);
        }
    }

    @Test
    public void trioRapidoPreparaCadaItemEnquantoAnteriorSegueVoando() throws Exception {
        try (ActivityScenario<MainActivity> scenario =
                ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(atividade::set);
            tocar();
            buscar(55000);
            buscar(56120);
            alimentos(1, 0);
            mika(CoreografiaCafe.Mika.PREPARA);
            buscar(56280);
            alimentos(0, 1);
            mika(CoreografiaCafe.Mika.LANCAR);
            buscar(56360);
            alimentos(1, 1);
            mika(CoreografiaCafe.Mika.PREPARA);
            buscar(56530);
            alimentos(0, 2);
            mika(CoreografiaCafe.Mika.LANCAR);
            buscar(56620);
            alimentos(1, 2);
            mika(CoreografiaCafe.Mika.PREPARA);
            salvarImagem("beatdays-lancamento-trio.png");
            buscar(56780);
            alimentos(0, 3);
            semJulgamento(57000);
            semJulgamento(57250);
            semJulgamento(57500);
            for (int n = 0; n < 3; n++) {
                buscar(57050 + n * 250);
                tocar();
                pegou(57000 + n * 250);
            }
            pontos(300, 3);
            alimentos(0, 0);
        }
    }

    private void mika(CoreografiaCafe.Mika estado) {
        InstrumentationRegistry.getInstrumentation()
                .runOnMainSync(
                        () -> {
                            CenaCafe cena = atividade.get().findViewById(R.id.cena_cafe);
                            assertEquals(estado, cena.getEstadoMika());
                            assertEquals(0, cena.getTranslationX(), 0);
                            assertEquals(0, cena.getTranslationY(), 0);
                        });
    }

    private void semJulgamento(long alvo) {
        InstrumentationRegistry.getInstrumentation()
                .runOnMainSync(() -> assertNull(motor().getJulgamento(id(alvo))));
    }

    private void alimentos(int preparo, int voo) {
        InstrumentationRegistry.getInstrumentation()
                .runOnMainSync(
                        () -> {
                            EfeitosCafeView efeitos =
                                    atividade.get().findViewById(R.id.efeitos_cafe);
                            Bitmap b =
                                    Bitmap.createBitmap(
                                            efeitos.getWidth(),
                                            efeitos.getHeight(),
                                            Bitmap.Config.ARGB_8888);
                            efeitos.draw(new Canvas(b));
                            b.recycle();
                            assertEquals(preparo, efeitos.getAlimentosEmPreparo());
                            assertEquals(voo, efeitos.getAlimentosEmVoo());
                        });
    }

    private Bitmap areaPreparo() {
        AtomicReference<Bitmap> imagem = new AtomicReference<>();
        InstrumentationRegistry.getInstrumentation()
                .runOnMainSync(
                        () -> {
                            View v = atividade.get().findViewById(R.id.efeitos_cafe);
                            Bitmap inteiro =
                                    Bitmap.createBitmap(
                                            v.getWidth(), v.getHeight(), Bitmap.Config.ARGB_8888);
                            v.draw(new Canvas(inteiro));
                            int top = Math.round(v.getHeight() * .22f),
                                    altura = Math.round(v.getHeight() * .22f);
                            int esquerda = Math.round(v.getWidth() * .55f),
                                    largura = Math.round(v.getWidth() * .25f);
                            imagem.set(
                                    Bitmap.createBitmap(inteiro, esquerda, top, largura, altura));
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
