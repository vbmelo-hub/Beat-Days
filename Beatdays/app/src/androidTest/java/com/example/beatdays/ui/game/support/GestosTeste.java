package com.example.beatdays.ui.game.support;

import android.app.Activity;
import android.app.Instrumentation;
import android.graphics.Rect;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;

import androidx.test.platform.app.InstrumentationRegistry;

import com.example.beatdays.R;

/** Toques físicos nos controles e na cena durante os testes. */
public final class GestosTeste {
    private GestosTeste() {}

    public static void tocar(Activity activity, int id) {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        boolean ajuste =
                id == R.id.btn_adiantar_cena
                        || id == R.id.btn_atrasar_cena
                        || id == R.id.btn_zerar_sincronia
                        || id == R.id.btn_recomecar_ajuste;
        if (ajuste) {
            instrumentation.runOnMainSync(
                    () -> {
                        if (activity.findViewById(R.id.painel_sincronia).getVisibility()
                                != View.VISIBLE) {
                            activity.findViewById(R.id.btn_sincronia).performClick();
                        }
                    });
        }
        long limite = SystemClock.uptimeMillis() + 4000;
        boolean[] pronto = {false};
        while (!pronto[0] && SystemClock.uptimeMillis() < limite) {
            instrumentation.runOnMainSync(
                    () -> {
                        View target = activity.findViewById(id == 0 ? R.id.tela_jogo : id);
                        pronto[0] =
                                target != null
                                        && target.isShown()
                                        && target.getWidth() > 0
                                        && target.getHeight() > 0;
                    });
            if (!pronto[0]) {
                SystemClock.sleep(20);
            }
        }
        if (!pronto[0]) {
            throw new AssertionError("Controle não apresentado: " + id);
        }
        if (ajuste) {
            instrumentation.runOnMainSync(
                    () -> {
                        View target = activity.findViewById(id);
                        target.requestRectangleOnScreen(
                                new Rect(0, 0, target.getWidth(), target.getHeight()), true);
                    });
            instrumentation.waitForIdleSync();
        }
        instrumentation.runOnMainSync(
                () -> {
                    View target = activity.findViewById(id == 0 ? R.id.tela_jogo : id);
                    Rect rect = new Rect();
                    if (!target.getGlobalVisibleRect(rect)) {
                        throw new AssertionError("Controle fora da tela: " + id);
                    }
                    int[] origin = new int[2];
                    activity.getWindow().getDecorView().getLocationOnScreen(origin);
                    float x =
                            (id == 0 ? rect.left + rect.width() * .85f : rect.exactCenterX())
                                    - origin[0];
                    float y =
                            (id == 0 ? rect.top + rect.height() * .82f : rect.exactCenterY())
                                    - origin[1];
                    long time = SystemClock.uptimeMillis();
                    for (int action : new int[] {MotionEvent.ACTION_DOWN, MotionEvent.ACTION_UP}) {
                        MotionEvent event =
                                MotionEvent.obtain(
                                        time, SystemClock.uptimeMillis(), action, x, y, 0);
                        activity.dispatchTouchEvent(event);
                        event.recycle();
                    }
                });
        instrumentation.waitForIdleSync();
    }
}
