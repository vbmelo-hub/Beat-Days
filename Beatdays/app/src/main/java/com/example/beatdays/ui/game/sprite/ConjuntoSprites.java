package com.example.beatdays.ui.game.sprite;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Rect;
import android.graphics.RectF;

import com.example.beatdays.R;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/** PNGs separados com referência comum. Recortes medidos sem dividir o desenho em células. */
public final class ConjuntoSprites {
    private final JSONObject metadata;
    private final Resources resources;

    public ConjuntoSprites(Resources resources) {
        this.resources = resources;
        try (InputStream in = resources.openRawResource(R.raw.sprites_geometry)) {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int n;
            while ((n = in.read(buffer)) >= 0) {
                bytes.write(buffer, 0, n);
            }
            metadata = new JSONObject(bytes.toString("UTF-8"));
        } catch (Exception e) {
            throw new IllegalStateException("Metadados dos sprites inválidos", e);
        }
    }

    public QuadroSprite[] carregar(String familia, String[] nomes) {
        try {
            JSONObject grupo = metadata.getJSONObject(familia);
            float ancora = (float) grupo.getDouble("ancora");
            float base = (float) grupo.getDouble("base");
            QuadroSprite[] quadros = new QuadroSprite[nomes.length];
            for (int i = 0; i < nomes.length; i++) {
                String nome = nomes[i];
                JSONObject dados = grupo.getJSONObject("quadros").getJSONObject(nome);
                JSONArray b = dados.getJSONArray("crop");
                int recurso = recurso(nome);
                if (recurso == 0) {
                    throw new IllegalArgumentException("Sprite ausente: " + nome);
                }
                try (InputStream in = resources.openRawResource(recurso)) {
                    BitmapRegionDecoder decoder = BitmapRegionDecoder.newInstance(in, false);
                    Rect crop = new Rect(b.getInt(0), b.getInt(1), b.getInt(2), b.getInt(3));
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inScaled = false;
                    Bitmap bitmap = decoder.decodeRegion(crop, options);
                    decoder.recycle();
                    JSONArray p = dados.getJSONArray("frame");
                    quadros[i] =
                            new QuadroSprite(
                                    bitmap,
                                    new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight()),
                                    new RectF(
                                            (float) p.getDouble(0),
                                            (float) p.getDouble(1),
                                            (float) p.getDouble(2),
                                            (float) p.getDouble(3)),
                                    ancora,
                                    base);
                }
            }
            return quadros;
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao carregar " + familia, e);
        }
    }

    private static final String[] POSES = {
        "idle", "receber", "pegar", "guardar", "erro", "desapontado", "caminhar_a", "caminhar_b"
    };

    public QuadroSprite[] cliente(String nome) {
        String[] ids = new String[8];
        for (int i = 0; i < 8; i++) {
            ids[i] = nome + "_" + POSES[i];
        }
        return carregar(nome, ids);
    }

    /** Recursos das poses disponíveis. */
    private static int recurso(String nome) {
        switch (nome) {
            case "cliente_idle":
                return R.drawable.cliente_idle;
            case "cliente_receber":
                return R.drawable.cliente_receber;
            case "cliente_pegar":
                return R.drawable.cliente_pegar;
            case "cliente_guardar":
                return R.drawable.cliente_guardar;
            case "cliente_erro":
                return R.drawable.cliente_erro;
            case "cliente_desapontado":
                return R.drawable.cliente_desapontado;
            case "cliente_caminhar_a":
                return R.drawable.cliente_caminhar_a;
            case "cliente_caminhar_b":
                return R.drawable.cliente_caminhar_b;
            case "theo_idle":
                return R.drawable.theo_idle;
            case "theo_receber":
                return R.drawable.theo_receber;
            case "theo_pegar":
                return R.drawable.theo_pegar;
            case "theo_guardar":
                return R.drawable.theo_guardar;
            case "theo_erro":
                return R.drawable.theo_erro;
            case "theo_desapontado":
                return R.drawable.theo_desapontado;
            case "theo_caminhar_a":
                return R.drawable.theo_caminhar_a;
            case "theo_caminhar_b":
                return R.drawable.theo_caminhar_b;
            case "lia_idle":
                return R.drawable.lia_idle;
            case "lia_receber":
                return R.drawable.lia_receber;
            case "lia_pegar":
                return R.drawable.lia_pegar;
            case "lia_guardar":
                return R.drawable.lia_guardar;
            case "lia_erro":
                return R.drawable.lia_erro;
            case "lia_desapontado":
                return R.drawable.lia_desapontado;
            case "lia_caminhar_a":
                return R.drawable.lia_caminhar_a;
            case "lia_caminhar_b":
                return R.drawable.lia_caminhar_b;
            case "barista_idle":
                return R.drawable.barista_idle;
            case "barista_preparar":
                return R.drawable.barista_preparar;
            case "barista_lancar":
                return R.drawable.barista_lancar;
            case "barista_despedida":
                return R.drawable.barista_despedida;
            default:
                throw new IllegalArgumentException("Sprite ausente: " + nome);
        }
    }
}
