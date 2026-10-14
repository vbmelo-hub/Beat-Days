package com.example.beatdays.ui.game.sprite;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.RectF;

/** Um recorte real inserido no quadro de referência: o recorte não controla o tamanho do corpo. */
public final class QuadroSprite {
    final Bitmap bitmap;
    final Rect origem;
    final RectF posicao;
    final float ancora, base;

    QuadroSprite(Bitmap bitmap, Rect origem, RectF posicao, float ancora, float base) {
        this.bitmap = bitmap;
        this.origem = origem;
        this.posicao = posicao;
        this.ancora = ancora;
        this.base = base;
    }

    public int getMemoriaBytes() {
        return bitmap.getAllocationByteCount();
    }
}
