package com.example.beatdays.ui.common;

import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

public final class TelaImersiva {
    private TelaImersiva() {}

    public static void configurar(AppCompatActivity activity, View raiz) {
        WindowCompat.setDecorFitsSystemWindows(activity.getWindow(), false);
        int esquerda = raiz.getPaddingLeft(), topo = raiz.getPaddingTop();
        int direita = raiz.getPaddingRight(), base = raiz.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(
                raiz,
                (view, insets) -> {
                    Insets seguro =
                            insets.getInsets(
                                    WindowInsetsCompat.Type.displayCutout()
                                            | WindowInsetsCompat.Type.systemBars());
                    view.setPadding(
                            esquerda + seguro.left,
                            topo + seguro.top,
                            direita + seguro.right,
                            base + seguro.bottom);
                    return insets;
                });
        ViewCompat.requestApplyInsets(raiz);
        ocultarBarras(activity);
    }

    /** Somente a interface respeita recortes; o cenário ocupa toda a janela. */
    public static void configurarCena(AppCompatActivity activity, View interfaceSegura) {
        WindowCompat.setDecorFitsSystemWindows(activity.getWindow(), false);
        ViewCompat.setOnApplyWindowInsetsListener(
                interfaceSegura,
                (view, insets) -> {
                    Insets seguro =
                            insets.getInsets(
                                    WindowInsetsCompat.Type.displayCutout()
                                            | WindowInsetsCompat.Type.systemBars());
                    view.setPadding(seguro.left, seguro.top, seguro.right, seguro.bottom);
                    return insets;
                });
        ViewCompat.requestApplyInsets(interfaceSegura);
        ocultarBarras(activity);
    }

    public static void ocultarBarras(AppCompatActivity activity) {
        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(
                        activity.getWindow(), activity.getWindow().getDecorView());
        controller.setSystemBarsBehavior(
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        controller.hide(WindowInsetsCompat.Type.systemBars());
    }
}
