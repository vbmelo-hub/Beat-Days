package com.example.beatdays.ui.game.sprite;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatImageView;

/** Renderiza a área visível do sprite, preservando os PNGs originais. */
public class SpriteImageView extends AppCompatImageView {
    private final Rect origem = new Rect();
    private final RectF destino = new RectF();
    private final Paint tinta = new Paint();
    private Bitmap bitmap;
    private float ancoraPes = .5f;
    private final RectF destinoRosto = new RectF();

    public SpriteImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        if (getDrawable() instanceof BitmapDrawable) {
            bitmap = ((BitmapDrawable) getDrawable()).getBitmap();
            int esquerda = bitmap.getWidth(), topo = bitmap.getHeight();
            int direita = -1, base = -1;
            int[] linha = new int[bitmap.getWidth()];
            for (int y = 0; y < bitmap.getHeight(); y++) {
                bitmap.getPixels(linha, 0, linha.length, 0, y, linha.length, 1);
                for (int x = 0; x < linha.length; x++) {
                    if ((linha[x] >>> 24) > 16) {
                        esquerda = Math.min(esquerda, x);
                        topo = Math.min(topo, y);
                        direita = Math.max(direita, x);
                        base = Math.max(base, y);
                    }
                }
            }
            if (direita >= esquerda) {
                origem.set(esquerda, topo, direita + 1, base + 1);
            } else {
                origem.set(0, 0, bitmap.getWidth(), bitmap.getHeight());
            }
            long soma = 0, total = 0;
            for (int y = origem.top + origem.height() * 4 / 5; y < origem.bottom; y++) {
                bitmap.getPixels(linha, 0, linha.length, 0, y, linha.length, 1);
                for (int x = origem.left; x < origem.right; x++) {
                    if ((linha[x] >>> 24) > 16) {
                        soma += x;
                        total++;
                    }
                }
            }
            if (total > 0) {
                ancoraPes = ((float) soma / total - origem.left) / origem.width();
            }
        }
        tinta.setFilterBitmap(false);
    }

    private QuadroSprite quadro;
    private Bitmap rosto;

    public void definirQuadro(QuadroSprite q) {
        if (quadro == q) {
            return;
        }
        quadro = q;
        bitmap = q.bitmap;
        origem.set(q.origem);
        ancoraPes = q.ancora;
        setImageDrawable(null);
        invalidate();
    }

    public void definirRosto(Bitmap imagem) {
        if (rosto == imagem) {
            return;
        }
        rosto = imagem;
        invalidate();
    }

    public static Bitmap carregarRosto(Context context, int recurso) {
        try (java.io.InputStream in = context.getResources().openRawResource(recurso)) {
            android.graphics.BitmapRegionDecoder decoder =
                    android.graphics.BitmapRegionDecoder.newInstance(in, false);
            Rect r =
                    new Rect(
                            Math.round(decoder.getWidth() * .420f),
                            Math.round(decoder.getHeight() * .125f),
                            Math.round(decoder.getWidth() * .595f),
                            Math.round(decoder.getHeight() * .270f));
            Bitmap b = decoder.decodeRegion(r, new android.graphics.BitmapFactory.Options());
            decoder.recycle();
            return b;
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public float getProporcaoVisivel() {
        return quadro != null
                ? 1f
                : origem.height() > 0 ? (float) origem.width() / origem.height() : 1f;
    }

    public float getAncoraPes() {
        return ancoraPes;
    }

    /** Ponto do desenho no quadro comum, já transformado para as coordenadas da cena. */
    public void mapearPonto(float x, float y, float[] destino) {
        float lado = Math.min(getWidth(), getHeight());
        destino[0] = (getWidth() - lado) / 2f + x * lado;
        destino[1] = (getHeight() - lado) / 2f + y * lado;
        getMatrix().mapPoints(destino);
        destino[0] += getLeft();
        destino[1] += getTop();
    }

    private final android.graphics.Path contornoRosto = new android.graphics.Path();

    private void desenharRosto(Canvas c, float x, float y, float lado) {
        contornoRosto.rewind();
        float[][] pontos = {
            {.450f, .140f},
            {.552f, .125f},
            {.585f, .178f},
            {.585f, .227f},
            {.553f, .263f},
            {.517f, .268f},
            {.477f, .255f},
            {.445f, .221f},
            {.420f, .184f}
        };
        for (int i = 0; i < pontos.length; i++) {
            float px = x + pontos[i][0] * lado, py = y + pontos[i][1] * lado;
            if (i == 0) {
                contornoRosto.moveTo(px, py);
            } else {
                contornoRosto.lineTo(px, py);
            }
        }
        contornoRosto.close();
        int save = c.save();
        c.clipPath(contornoRosto);
        destinoRosto.set(x + .420f * lado, y + .125f * lado, x + .595f * lado, y + .270f * lado);
        c.drawBitmap(rosto, null, destinoRosto, tinta);
        c.restoreToCount(save);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (bitmap == null) {
            super.onDraw(canvas);
            return;
        }
        if (quadro != null) {
            float lado = Math.min(getWidth(), getHeight());
            float x = (getWidth() - lado) / 2f, y = (getHeight() - lado) / 2f;
            RectF p = quadro.posicao;
            destino.set(
                    x + p.left * lado, y + p.top * lado, x + p.right * lado, y + p.bottom * lado);
            canvas.drawBitmap(bitmap, origem, destino, tinta);
            if (rosto != null) {
                desenharRosto(canvas, x, y, lado);
            }
            return;
        }
        float escala =
                Math.min(
                        (float) getWidth() / origem.width(), (float) getHeight() / origem.height());
        float largura = origem.width() * escala, altura = origem.height() * escala;
        destino.set(
                (getWidth() - largura) / 2f,
                (getHeight() - altura) / 2f,
                (getWidth() + largura) / 2f,
                (getHeight() + altura) / 2f);
        canvas.drawBitmap(bitmap, origem, destino, tinta);
    }
}
