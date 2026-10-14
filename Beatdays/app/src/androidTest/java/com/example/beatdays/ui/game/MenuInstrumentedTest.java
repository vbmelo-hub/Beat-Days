package com.example.beatdays.ui.game;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.app.Instrumentation;
import android.graphics.Bitmap;
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
import com.example.beatdays.ui.about.SobreActivity;
import com.example.beatdays.ui.editor.EditorBatidasActivity;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicReference;

/** Exercita os gestos reais, os controles fixos e as transições dos menus. */
@RunWith(AndroidJUnit4.class)
public class MenuInstrumentedTest {
    private final Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
    private final AtomicReference<Activity> current = new AtomicReference<>();

    @Test
    public void aberturaEPausaMantemAcoesVisiveisERolagemNaoIniciaPartida() throws Exception {
        try (ActivityScenario<MainActivity> scenario =
                ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(current::set);
            idle();
            visibleButton(R.id.btn_acao_menu);
            visibleButton(R.id.btn_treinar);
            save("menus-inicio.png");
            drag(R.id.menu_scroll_area);
            drag(R.id.menu_marca_scroll);
            shown(R.id.overlay_jogo, true);
            title(R.string.nome_fase);
            tap(R.id.btn_acao_menu);
            shown(R.id.overlay_jogo, false);
            tap(R.id.btn_pausar);
            title(R.string.pausado);
            visibleButton(R.id.btn_acao_menu);
            visibleButton(R.id.btn_inicio);
            shown(R.id.painel_sincronia, false);
            save("menus-pausa.png");
            drag(R.id.menu_scroll_area);
            shown(R.id.overlay_jogo, true);
            title(R.string.pausado);
            scrollTo(R.id.btn_sincronia);
            tap(R.id.btn_sincronia);
            shown(R.id.painel_sincronia, true);
            scrollTo(R.id.btn_zerar_sincronia);
            tap(R.id.btn_zerar_sincronia);
            visibleButton(R.id.btn_acao_menu);
            save("menus-sincronia.png");
            scenario.recreate();
            scenario.onActivity(current::set);
            idle();
            shown(R.id.painel_sincronia, true);
            tap(R.id.btn_acao_menu);
            shown(R.id.overlay_jogo, false);
            tap(R.id.btn_pausar);
            shown(R.id.painel_sincronia, false);
            tap(R.id.btn_inicio);
            title(R.string.nome_fase);
            visibleButton(R.id.btn_treinar);
        }
    }

    @Test
    public void sobreAbreERetornaSemIniciarTurno() throws Exception {
        try (ActivityScenario<MainActivity> scenario =
                ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(current::set);
            idle();
            Instrumentation.ActivityMonitor monitor =
                    instrumentation.addMonitor(SobreActivity.class.getName(), null, false);
            try {
                tap(R.id.btn_sobre);
                Activity credits = instrumentation.waitForMonitorWithTimeout(monitor, 5000);
                assertNotNull("Créditos não abriram", credits);
                current.set(credits);
                idle();
                visibleButton(R.id.btn_voltar_sobre);
                save("menus-sobre.png");
                tap(R.id.btn_voltar_sobre);
                scenario.onActivity(current::set);
                idle();
                title(R.string.nome_fase);
                shown(R.id.overlay_jogo, true);
            } finally {
                instrumentation.removeMonitor(monitor);
            }
        }
    }

    @Test
    public void resultadoMantemNumerosRolaveisEAcoesAcessiveis() throws Exception {
        try (ActivityScenario<MainActivity> scenario =
                ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(current::set);
            idle();
            tap(R.id.btn_acao_menu);
            instrumentation.runOnMainSync(
                    () -> {
                        try {
                            Field field = MainActivity.class.getDeclaredField("audio");
                            field.setAccessible(true);
                            ((ControladorAudio) field.get(current.get())).definirPosicaoMs(79000);
                        } catch (ReflectiveOperationException e) {
                            throw new AssertionError(e);
                        }
                    });
            long deadline = SystemClock.uptimeMillis() + 8000;
            AtomicReference<Boolean> finished = new AtomicReference<>(false);
            while (!finished.get() && SystemClock.uptimeMillis() < deadline) {
                instrumentation.runOnMainSync(
                        () ->
                                finished.set(
                                        current.get()
                                                .findViewById(R.id.menu_resultados)
                                                .isShown()));
                SystemClock.sleep(50);
            }
            assertTrue("Resultado não apareceu após o fim do áudio", finished.get());
            idle();
            title(R.string.turno_completo);
            visibleButton(R.id.btn_acao_menu);
            visibleButton(R.id.btn_inicio);
            instrumentation.runOnMainSync(
                    () -> {
                        assertEquals(
                                "0",
                                ((TextView) current.get().findViewById(R.id.tv_menu_pontos))
                                        .getText()
                                        .toString());
                        assertEquals(
                                "0",
                                ((TextView) current.get().findViewById(R.id.tv_menu_combo))
                                        .getText()
                                        .toString());
                        assertTrue(
                                ((TextView) current.get().findViewById(R.id.tv_menu_miss))
                                        .getText()
                                        .toString()
                                        .endsWith("31"));
                    });
            save("menus-resultado.png");
            scrollTo(R.id.tv_pedidos_resultado);
            save("menus-resultado-detalhes.png");
            visibleButton(R.id.btn_acao_menu);
            tap(R.id.btn_acao_menu);
            shown(R.id.overlay_jogo, false);
        }
    }

    @Test
    public void treinoContinuaAcessivelPelaAbertura() {
        try (ActivityScenario<MainActivity> scenario =
                ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(current::set);
            idle();
            tap(R.id.btn_treinar);
            shown(R.id.overlay_jogo, false);
            tap(R.id.btn_pausar);
            title(R.string.pausado);
            tap(R.id.btn_inicio);
            title(R.string.nome_fase);
        }
    }

    @Test
    public void editorMantemControlesEAreaDePartitura() throws Exception {
        try (ActivityScenario<EditorBatidasActivity> scenario =
                ActivityScenario.launch(EditorBatidasActivity.class)) {
            scenario.onActivity(current::set);
            idle();
            visibleButton(R.id.btn_iniciar_musica);
            save("menus-editor.png");
            tap(R.id.btn_iniciar_musica);
            instrumentation.runOnMainSync(
                    () -> assertTrue(current.get().findViewById(R.id.btn_marcar).isEnabled()));
            tap(R.id.btn_marcar);
            instrumentation.runOnMainSync(
                    () ->
                            assertTrue(
                                    ((TextView) current.get().findViewById(R.id.tv_quantidade))
                                            .getText()
                                            .toString()
                                            .contains("1")));
        }
    }

    private void title(int string) {
        instrumentation.runOnMainSync(
                () ->
                        assertEquals(
                                current.get().getString(string),
                                ((TextView) current.get().findViewById(R.id.tv_overlay_titulo))
                                        .getText()
                                        .toString()));
    }

    private void shown(int id, boolean shown) {
        instrumentation.runOnMainSync(
                () ->
                        assertEquals(
                                current.get().getResources().getResourceEntryName(id),
                                shown,
                                current.get().findViewById(id).isShown()));
    }

    private void visibleButton(int id) {
        instrumentation.runOnMainSync(
                () -> {
                    View button = current.get().findViewById(id);
                    Rect visible = new Rect();
                    assertTrue("Botão fora da tela: " + id, button.getGlobalVisibleRect(visible));
                    assertTrue(
                            "Botão cortado verticalmente",
                            visible.height() >= button.getHeight() - 2);
                    assertTrue(
                            "Botão cortado horizontalmente",
                            visible.width() >= button.getWidth() - 2);
                    assertTrue(
                            "Área de toque inferior a 48dp",
                            visible.height() + 2
                                    >= 48 * button.getResources().getDisplayMetrics().density);
                    if (button instanceof TextView) {
                        TextView text = (TextView) button;
                        for (int line = 0; line < text.getLayout().getLineCount(); line++) {
                            assertEquals(
                                    "Texto truncado", 0, text.getLayout().getEllipsisCount(line));
                            assertTrue(
                                    "Texto mais largo que o botão",
                                    text.getLayout().getLineWidth(line)
                                            <= text.getWidth()
                                                    - text.getCompoundPaddingLeft()
                                                    - text.getCompoundPaddingRight()
                                                    + 2);
                        }
                        assertTrue(
                                "Texto cortado na altura",
                                text.getLayout().getHeight()
                                        <= text.getHeight()
                                                - text.getCompoundPaddingTop()
                                                - text.getCompoundPaddingBottom()
                                                + 2);
                    }
                });
    }

    private void tap(int id) {
        idle();
        instrumentation.runOnMainSync(
                () -> {
                    View target = current.get().findViewById(id);
                    Rect rect = new Rect();
                    assertTrue(target.getGlobalVisibleRect(rect));
                    int[] location = new int[2];
                    current.get().getWindow().getDecorView().getLocationOnScreen(location);
                    float x = rect.exactCenterX() - location[0],
                            y = rect.exactCenterY() - location[1];
                    long time = SystemClock.uptimeMillis();
                    for (int action : new int[] {MotionEvent.ACTION_DOWN, MotionEvent.ACTION_UP}) {
                        MotionEvent event =
                                MotionEvent.obtain(
                                        time, SystemClock.uptimeMillis(), action, x, y, 0);
                        current.get().dispatchTouchEvent(event);
                        event.recycle();
                    }
                });
        idle();
    }

    private void drag(int id) {
        instrumentation.runOnMainSync(
                () -> {
                    View target = current.get().findViewById(id);
                    Rect rect = new Rect();
                    assertTrue(target.getGlobalVisibleRect(rect));
                    int[] location = new int[2];
                    current.get().getWindow().getDecorView().getLocationOnScreen(location);
                    long time = SystemClock.uptimeMillis();
                    for (int step = 0; step < 8; step++) {
                        int action =
                                step == 0
                                        ? MotionEvent.ACTION_DOWN
                                        : step == 7
                                                ? MotionEvent.ACTION_UP
                                                : MotionEvent.ACTION_MOVE;
                        MotionEvent event =
                                MotionEvent.obtain(
                                        time,
                                        time + step * 20,
                                        action,
                                        rect.exactCenterX() - location[0],
                                        rect.bottom
                                                - 8
                                                - (rect.height() - 16) * step / 7f
                                                - location[1],
                                        0);
                        current.get().dispatchTouchEvent(event);
                        event.recycle();
                    }
                });
        SystemClock.sleep(700);
        idle();
    }

    private void scrollTo(int id) {
        instrumentation.runOnMainSync(
                () -> {
                    View target = current.get().findViewById(id);
                    target.requestRectangleOnScreen(
                            new Rect(0, 0, target.getWidth(), target.getHeight()), true);
                });
        idle();
    }

    private void idle() {
        instrumentation.waitForIdleSync();
        SystemClock.sleep(100);
    }

    private void save(String name) throws Exception {
        idle();
        Bitmap screenshot = instrumentation.getUiAutomation().takeScreenshot();
        assertNotNull(screenshot);
        File directory = current.get().getExternalFilesDir(null);
        try (FileOutputStream output = new FileOutputStream(new File(directory, name))) {
            screenshot.compress(Bitmap.CompressFormat.PNG, 100, output);
        }
        screenshot.recycle();
    }
}
