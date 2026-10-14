package com.example.beatdays.data;

import android.content.Context;
import android.content.SharedPreferences;

/** Recorde local e ajuste de sincronia. */
public final class PreferenciasJogo {
    private final SharedPreferences resultados, ajustes, legado;

    public PreferenciasJogo(Context context) {
        Context app = context.getApplicationContext();
        resultados = app.getSharedPreferences("beatdays_resultados", Context.MODE_PRIVATE);
        ajustes = app.getSharedPreferences("beatdays_ajustes", Context.MODE_PRIVATE);
        legado = app.getSharedPreferences("beatdays_progressao", Context.MODE_PRIVATE);
    }

    public int getRecorde() {
        return Math.max(resultados.getInt("recorde_cafe", 0), legado.getInt("score_0", 0));
    }

    public void salvarRecorde(int valor) {
        resultados.edit().putInt("recorde_cafe", valor).apply();
    }

    public int getAjusteMs() {
        return ajustes.getInt("atraso_cena_ms", 0);
    }

    public void salvarAjusteMs(int valor) {
        ajustes.edit().putInt("atraso_cena_ms", valor).apply();
    }
}
