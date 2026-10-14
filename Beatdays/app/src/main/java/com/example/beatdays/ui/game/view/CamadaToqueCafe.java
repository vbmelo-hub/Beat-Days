package com.example.beatdays.ui.game.view;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

import com.example.beatdays.R;

/** A cena inteira recebe o gesto, inclusive sobre os sinais e textos. O pause mantém seu clique. */
public final class CamadaToqueCafe extends FrameLayout {
    public interface AoTocar {
        boolean executar(View view, MotionEvent event);
    }

    private AoTocar aoTocar;
    private final Rect areaControle = new Rect();
    private boolean gestoNoControle;

    public CamadaToqueCafe(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void definirAoTocar(AoTocar aoTocar) {
        this.aoTocar = aoTocar;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            gestoNoControle = false;
            gestoNoControle =
                    dentroDoControle(R.id.btn_pausar, event)
                            || dentroDoControle(R.id.painel_sincronia, event)
                            || dentroDoControle(R.id.btn_acao_menu, event)
                            || dentroDoControle(R.id.btn_sincronia, event)
                            || dentroDoControle(R.id.menu_scroll_area, event)
                            || dentroDoControle(R.id.menu_marca_scroll, event)
                            || dentroDoControle(R.id.btn_treinar, event)
                            || dentroDoControle(R.id.btn_inicio, event)
                            || dentroDoControle(R.id.btn_sobre, event);
        }
        return !gestoNoControle && aoTocar != null;
    }

    private boolean dentroDoControle(int id, MotionEvent event) {
        View controle = findViewById(id);
        if (controle == null || !controle.isShown()) {
            return false;
        }
        areaControle.set(0, 0, controle.getWidth(), controle.getHeight());
        offsetDescendantRectToMyCoords(controle, areaControle);
        return areaControle.contains((int) event.getX(), (int) event.getY());
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (aoTocar == null) {
            return super.onTouchEvent(event);
        }
        boolean tratado = aoTocar.executar(this, event);
        if (tratado && event.getActionMasked() == MotionEvent.ACTION_UP) {
            performClick();
        }
        return tratado;
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }
}
