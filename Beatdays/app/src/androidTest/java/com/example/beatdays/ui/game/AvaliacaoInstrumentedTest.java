package com.example.beatdays.ui.game;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
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
import com.example.beatdays.game.RoteiroRecepcao;
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
public class AvaliacaoInstrumentedTest {
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
        InstrumentationRegistry.getInstrumentation()
                .getTargetContext()
                .getSharedPreferences("beatdays_resultados", Context.MODE_PRIVATE)
                .edit()
                .clear()
                .commit();
    }

    @Test
    public void partidaCompletaAvaliaSalvaRecordeERecriacaoOuPartidaPiorNaoApagam()
            throws Exception {
        try (ActivityScenario<MainActivity> scenario =
                ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(atividade::set);
            aguardarLayout();
            verificarRecorde(0, false);
            tocar();
            buscar(21030);
            tocar();
            pontos(100, 1);
            assertEquals(0, recordeSalvo());
        }
        try (ActivityScenario<MainActivity> scenario =
                ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(atividade::set);
            aguardarLayout();
            verificarRecorde(0, false);
            tocar();
            buscar(12050);
            tocar();
            buscar(13030);
            tocar();
            for (int tempo : new int[] {13530, 17530, 18030, 18530}) {
                buscar(tempo);
                tocar();
            }
            pontos(0, 0);
            int capturas = 0;
            for (EntregaCafe e : RoteiroRecepcao.criarEntregas()) {
                if (e.tempoMs < RoteiroRecepcao.INICIO_PONTUACAO_MS) {
                    continue;
                }
                buscar((int) e.tempoMs + 30);
                tocar();
                capturas++;
            }
            assertEquals(31, capturas);
            pontos(3100, 31);
            finalizar();
            verificarAvaliacao(R.string.avaliacao_perfeito, 100);
            verificarRecorde(3100, true);
            assertEquals(3100, recordeSalvo());
            scenario.onActivity(
                    a ->
                            assertEquals(
                                    String.valueOf(3100),
                                    ((TextView) a.findViewById(R.id.tv_menu_pontos))
                                            .getText()
                                            .toString()));
            salvarImagem("beatdays-duplas-avaliacao-perfeito.png");
            scenario.recreate();
            scenario.onActivity(atividade::set);
            aguardarLayout();
            verificarAvaliacao(R.string.avaliacao_perfeito, 100);
            verificarRecorde(3100, true);
            assertEquals(3100, recordeSalvo());
        }
        try (ActivityScenario<MainActivity> scenario =
                ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(atividade::set);
            aguardarLayout();
            verificarRecorde(3100, false);
            scenario.onActivity(
                    a ->
                            assertEquals(
                                    View.GONE, a.findViewById(R.id.tv_avaliacao).getVisibility()));
            salvarImagem("beatdays-duplas-avaliacao-inicio-recorde.png");
            tocar();
            scenario.onActivity(
                    a -> {
                        assertEquals(View.GONE, a.findViewById(R.id.tv_avaliacao).getVisibility());
                        assertEquals(View.GONE, a.findViewById(R.id.tv_recorde).getVisibility());
                    });
            buscar(21030);
            tocar();
            pontos(100, 1);
            buscar(22050);
            tocar();
            buscar(22200);
            for (int i = 0; i < 15; i++) {
                tocar();
            }
            finalizar();
            verificarAvaliacao(R.string.avaliacao_tente, 100.0 / 32);
            verificarRecorde(3100, false);
            assertEquals(3100, recordeSalvo());
            scenario.onActivity(
                    a ->
                            assertEquals(
                                    String.valueOf(100),
                                    ((TextView) a.findViewById(R.id.tv_menu_pontos))
                                            .getText()
                                            .toString()));
            salvarImagem("beatdays-duplas-avaliacao-tente.png");
            scenario.recreate();
            scenario.onActivity(atividade::set);
            aguardarLayout();
            verificarAvaliacao(R.string.avaliacao_tente, 100.0 / 32);
            verificarRecorde(3100, false);
            tocar();
            pontos(0, 0);
            assertEquals(3100, recordeSalvo());
        }
    }

    private int recordeSalvo() {
        return InstrumentationRegistry.getInstrumentation()
                .getTargetContext()
                .getSharedPreferences("beatdays_resultados", Context.MODE_PRIVATE)
                .getInt("recorde_cafe", 0);
    }

    private void aguardarLayout() {
        long limite = SystemClock.uptimeMillis() + 5000;
        AtomicLong largura = new AtomicLong();
        while (SystemClock.uptimeMillis() < limite) {
            InstrumentationRegistry.getInstrumentation()
                    .runOnMainSync(
                            () -> {
                                View raiz = atividade.get().findViewById(R.id.tela_jogo);
                                largura.set(
                                        raiz.isShown() && raiz.getHeight() > 0
                                                ? raiz.getWidth()
                                                : 0);
                            });
            if (largura.get() > 0) {
                InstrumentationRegistry.getInstrumentation().waitForIdleSync();
                return;
            }
            SystemClock.sleep(30);
        }
        fail("Layout do jogo não ficou disponível.");
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

    private void verificarAvaliacao(int texto, double precisao) {
        InstrumentationRegistry.getInstrumentation()
                .runOnMainSync(
                        () -> {
                            MainActivity a = atividade.get();
                            TextView avaliacao = a.findViewById(R.id.tv_avaliacao);
                            assertEquals(View.VISIBLE, avaliacao.getVisibility());
                            assertEquals(
                                    a.getString(
                                            R.string.avaliacao_resumo,
                                            a.getString(texto),
                                            precisao),
                                    avaliacao.getText().toString());
                            dentroDaTela(avaliacao);
                            dentroDaTela(a.findViewById(R.id.tv_overlay_dica));
                        });
    }

    private void verificarRecorde(int valor, boolean novo) {
        InstrumentationRegistry.getInstrumentation()
                .runOnMainSync(
                        () -> {
                            MainActivity a = atividade.get();
                            TextView recorde = a.findViewById(R.id.tv_recorde);
                            if (valor == 0) {
                                assertEquals(View.GONE, recorde.getVisibility());
                            } else {
                                assertEquals(View.VISIBLE, recorde.getVisibility());
                                assertEquals(
                                        a.getString(
                                                novo
                                                        ? R.string.novo_recorde_cafe
                                                        : R.string.recorde_cafe,
                                                valor),
                                        recorde.getText().toString());
                                dentroDaTela(recorde);
                            }
                        });
    }

    private void dentroDaTela(View view) {
        Rect area = new Rect();
        view.getGlobalVisibleRect(area);
        int[] posicao = new int[2];
        view.getLocationOnScreen(posicao);
        assertEquals(view.getWidth(), area.width());
        assertEquals(view.getHeight(), area.height());
        assertEquals(posicao[0], area.left);
        assertEquals(posicao[1], area.top);
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
