package com.example.beatdays.ui.game.support;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.app.Activity;

import com.example.beatdays.R;

/** Visibilidade total de cada personagem durante as transições. */
public final class PosesTeste {
    private PosesTeste() {}

    public static void cliente(Activity activity) {
        mistura(
                activity,
                new int[] {
                    R.id.img_cliente,
                    R.id.img_cliente_receber,
                    R.id.img_cliente_pegar,
                    R.id.img_cliente_guardar,
                    R.id.img_cliente_erro,
                    R.id.img_cliente_desapontado,
                    R.id.img_cliente_caminhar_a,
                    R.id.img_cliente_caminhar_b
                });
    }

    public static void mika(Activity activity) {
        mistura(
                activity,
                new int[] {
                    R.id.img_barista,
                    R.id.img_barista_preparar,
                    R.id.img_barista_lancar,
                    R.id.img_barista_despedida
                });
    }

    private static void mistura(Activity activity, int[] ids) {
        float total = 0;
        for (int id : ids) {
            float alpha = activity.findViewById(id).getAlpha();
            assertTrue(alpha >= 0 && alpha <= 1);
            total += alpha;
        }
        assertEquals("A transição não deve ocultar nem duplicar o personagem", 1, total, .0001f);
    }
}
