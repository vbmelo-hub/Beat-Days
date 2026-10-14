package com.example.beatdays.ui.about;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.beatdays.R;
import com.example.beatdays.ui.common.TelaImersiva;

public final class SobreActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        setContentView(R.layout.activity_sobre);
        findViewById(R.id.btn_voltar_sobre).setOnClickListener(v -> finish());
        TelaImersiva.configurar(this, findViewById(R.id.tela_creditos));
    }

    @Override
    public void onWindowFocusChanged(boolean foco) {
        super.onWindowFocusChanged(foco);
        if (foco) {
            TelaImersiva.ocultarBarras(this);
        }
    }
}
