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
import com.example.beatdays.ui.game.animation.CoreografiaCafe;
import com.example.beatdays.ui.game.animation.ReacaoMika;
import com.example.beatdays.ui.game.support.GestosTeste;
import com.example.beatdays.ui.game.view.CenaCafe;

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
public class ReacaoMikaInstrumentedTest {
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
    public void aprovaCapturaPreservaCenaNaPausaEReageAoMissSemRenovarPenalidade()
            throws Exception {
        try (ActivityScenario<MainActivity> scenario =
                ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(atividade::set);
            tocar();
            buscar(20000);
            buscar(23400);
            buscar(24900);
            reacao(ReacaoMika.Tipo.NENHUMA);
            buscar(25030);
            tocar();
            reacao(ReacaoMika.Tipo.NENHUMA);
            buscar(25110);
            pontos(100, 1);
            reacao(ReacaoMika.Tipo.APROVA);
            SystemClock.sleep(200);
            salvarImagem("beatdays-mika-aprovacao.png");
            tocar(R.id.btn_pausar);
            long pausa = posicao();
            SystemClock.sleep(200);
            Bitmap antes = quadro();
            scenario.recreate();
            scenario.onActivity(atividade::set);
            esperarPosicao((int) pausa);
            reacao(ReacaoMika.Tipo.APROVA);
            Bitmap depois = quadro();
            assertTrue("Cena deve preservar a reação da Mika", antes.sameAs(depois));
            antes.recycle();
            depois.recycle();
            pontos(100, 1);
            InstrumentationRegistry.getInstrumentation()
                    .runOnMainSync(
                            () -> {
                                atividade.get().findViewById(R.id.btn_pausar).performClick();
                                audio().pausar();
                            });
            buscar(25250);
            reacao(ReacaoMika.Tipo.NENHUMA);
            buscar(25750);
            buscar(25900);
            tocar();
            buscar(26030);
            reacao(ReacaoMika.Tipo.SUSTO);
            salvarImagem("beatdays-mika-susto.png");
            long limite = motor().getBloqueadoAteMs();
            buscar(26120);
            tocar();
            assertEquals(limite, motor().getBloqueadoAteMs());
            buscar(26430);
            reacao(ReacaoMika.Tipo.SUSTO);
            buscar(26480);
            reacao(ReacaoMika.Tipo.NENHUMA);
            pontos(100, 0);
        }
    }

    @Test
    public void trioRapidoNaoInterrompeLancamentoENaoAcrescentaMovimentoNaCamera()
            throws Exception {
        try (ActivityScenario<MainActivity> scenario =
                ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(atividade::set);
            tocar();
            buscar(66000);
            buscar(67950);
            buscar(68030);
            tocar();
            pontos(100, 1);
            reacao(ReacaoMika.Tipo.NENHUMA);
            InstrumentationRegistry.getInstrumentation()
                    .runOnMainSync(
                            () ->
                                    assertEquals(
                                            CoreografiaCafe.Mika.LANCAR,
                                            ((CenaCafe)
                                                            atividade
                                                                    .get()
                                                                    .findViewById(R.id.cena_cafe))
                                                    .getEstadoMika()));
            buscar(68110);
            reacao(ReacaoMika.Tipo.APROVA);
            for (int n = 1; n < 3; n++) {
                buscar(68030 + n * 250);
                tocar();
                pontos((n + 1) * 100, n + 1);
                reacao(ReacaoMika.Tipo.APROVA);
            }
            salvarImagem("beatdays-mika-trio.png");
            buscar(68760);
            reacao(ReacaoMika.Tipo.NENHUMA);
            pontos(300, 3);
        }
    }

    private void reacao(ReacaoMika.Tipo tipo) {
        InstrumentationRegistry.getInstrumentation()
                .runOnMainSync(
                        () -> {
                            CenaCafe cena = atividade.get().findViewById(R.id.cena_cafe);
                            assertEquals(tipo, cena.getReacaoMika().getTipo());
                            assertEquals(0, cena.getTranslationX(), 0);
                            assertEquals(0, cena.getTranslationY(), 0);
                            CoreografiaCafe esperada = new CoreografiaCafe();
                            esperada.atualizar(audio().getPosicaoAtualMs(), motor(), -10000, null);
                            assertEquals(esperada.getZoom(), cena.getScaleX(), .001f);
                            assertEquals(cena.getScaleX(), cena.getScaleY(), 0);
                        });
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
