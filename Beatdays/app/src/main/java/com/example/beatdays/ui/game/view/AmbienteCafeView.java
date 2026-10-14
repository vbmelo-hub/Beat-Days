package com.example.beatdays.ui.game.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import com.example.beatdays.game.RoteiroRecepcao;

/** Vapor decorativo atrás dos personagens, no mesmo espaço do fundo da cafeteria. */
public class AmbienteCafeView extends View {
    private final Paint tinta = new Paint();
    private long tempo;

    public AmbienteCafeView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void atualizar(long tempo) {
        this.tempo = Math.max(0, tempo);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float largura = getWidth(), altura = getHeight();
        if (largura == 0 || altura == 0) {
            return;
        }
        float pixel = Math.max(1, largura / 724f);
        float origemX = largura * .597f, origemY = altura * .368f;
        long ciclo = RoteiroRecepcao.BATIDA_MS * 4;
        tinta.setColor(Color.rgb(255, 241, 216));
        for (int filete = 0; filete < 3; filete++) {
            float p = ((tempo + filete * ciclo / 3) % ciclo) / (float) ciclo;
            tinta.setAlpha(Math.round(64 * (float) Math.sin(p * Math.PI)));
            for (int segmento = 0; segmento < 9; segmento++) {
                float q = segmento / 8f;
                float x =
                        origemX
                                + (filete - 1) * pixel * 3
                                + (float) Math.sin(q * Math.PI * 2 + p * Math.PI) * pixel * 1.4f;
                float y = origemY - altura * (.028f * p + .020f * q);
                x = Math.round(x / pixel) * pixel;
                y = Math.round(y / pixel) * pixel;
                canvas.drawRect(x, y, x + pixel, y + pixel * 1.5f, tinta);
            }
        }
    }
}
