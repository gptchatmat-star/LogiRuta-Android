package com.sbs.scannerpdf;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.GradientDrawable;
import android.graphics.pdf.PdfDocument;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.InputType;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int REQUEST_CAPTURE = 21;
    private static final int REQUEST_IMAGES = 22;
    private static final int MAX_PAGES = 12;
    private final List<Bitmap> pages = new ArrayList<>();
    private ImageView preview;
    private TextView summary;
    private TextView helper;
    private int selected = 0;
    private Uri captureUri;
    private Uri exportedPdf;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Color.rgb(246, 248, 252));
        LinearLayout root = new LinearLayout(this);
        root.setPadding(dp(18), dp(24), dp(18), dp(25));
        root.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(root);
        setContentView(scroll);

        TextView title = new TextView(this);
        title.setText("SBS ESCANER PDF");
        title.setTextSize(23);
        title.setTextColor(Color.rgb(20, 45, 73));
        title.setTypeface(null, 1);
        root.addView(title);

        TextView desc = new TextView(this);
        desc.setText("Captura, prepara y guarda tus documentos en PDF");
        desc.setTextSize(14);
        desc.setTextColor(Color.DKGRAY);
        desc.setPadding(0, dp(5), 0, dp(14));
        root.addView(desc);

        summary = new TextView(this);
        summary.setTextSize(15);
        summary.setTextColor(Color.rgb(28, 58, 88));
        root.addView(summary);

        preview = new ImageView(this);
        preview.setScaleType(ImageView.ScaleType.FIT_CENTER);
        preview.setBackgroundColor(Color.WHITE);
        LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(325));
        imageParams.setMargins(0, dp(10), 0, dp(8));
        root.addView(preview, imageParams);

        helper = new TextView(this);
        helper.setTextSize(13);
        helper.setTextColor(Color.DKGRAY);
        helper.setPadding(0, 0, 0, dp(10));
        root.addView(helper);

        addRow(root, new String[]{"Tomar foto", "Galeria"},
            new Runnable[]{this::capturePhoto, this::chooseImages}, true);
        addRow(root, new String[]{"Anterior", "Siguiente"},
            new Runnable[]{() -> move(-1), () -> move(1)}, false);
        addRow(root, new String[]{"Girar 90°", "Recortar", "Mejorar"},
            new Runnable[]{this::rotate, this::crop, this::improve}, false);
        addRow(root, new String[]{"Eliminar", "Crear PDF"},
            new Runnable[]{this::deletePage, this::exportPdf}, true);
        addRow(root, new String[]{"Abrir PDF", "Compartir PDF"},
            new Runnable[]{this::openPdf, this::sharePdf}, false);

        TextView info = new TextView(this);
        info.setText("Piloto v0.1: recorte manual por margenes; las fotos se procesan en el dispositivo. Hasta 12 paginas.");
        info.setTextSize(12);
        info.setTextColor(Color.GRAY);
        info.setPadding(0, dp(12), 0, 0);
        root.addView(info);
        updatePreview();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void addRow(LinearLayout root, String[] titles, Runnable[] actions, boolean mainRow) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rowParams.setMargins(0, dp(4), 0, dp(4));
        root.addView(row, rowParams);
        for (int i = 0; i < titles.length; i++) {
            Button button = new Button(this);
            button.setAllCaps(false);
            button.setText(titles[i]);
            button.setTextSize(titles.length == 3 ? 12 : 14);
            button.setTextColor(Color.WHITE);
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(mainRow ? Color.rgb(23, 111, 178) : Color.rgb(54, 77, 106));
            bg.setCornerRadius(dp(12));
            button.setBackground(bg);
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, dp(49), 1f);
            p.setMargins(dp(3), 0, dp(3), 0);
            row.addView(button, p);
            final Runnable action = actions[i];
            button.setOnClickListener(v -> action.run());
        }
    }

    private boolean hasPage() {
        if (pages.isEmpty()) {
            toast("Primero toma o importa una foto");
            return false;
        }
        return true;
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void updatePreview() {
        if (pages.isEmpty()) {
            preview.setImageBitmap(null);
            preview.setImageResource(android.R.drawable.ic_menu_gallery);
            summary.setText("Sin paginas todavia");
            helper.setText("Usa la camara o selecciona imagenes.");
        } else {
            selected = Math.max(0, Math.min(selected, pages.size() - 1));
            preview.setImageBitmap(pages.get(selected));
            summary.setText("Pagina " + (selected + 1) + " de " + pages.size());
            helper.setText("Puedes girar, recortar y mejorar la imagen antes de crear el PDF.");
        }
    }

    private void capturePhoto() {
        if (pages.size() >= MAX_PAGES) {
            toast("Limite de 12 paginas para esta prueba");
            return;
        }
        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, "SBSScan_" + System.currentTimeMillis() + ".jpg");
        values.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
        values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/SBSScannerPDF");
        captureUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        if (captureUri == null) {
            toast("No se pudo preparar la camara");
            return;
        }
        Intent camera = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        camera.putExtra(MediaStore.EXTRA_OUTPUT, captureUri);
        camera.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            startActivityForResult(camera, REQUEST_CAPTURE);
        } catch (ActivityNotFoundException ex) {
            getContentResolver().delete(captureUri, null, null);
            captureUri = null;
            toast("No hay una aplicacion de camara disponible");
        }
    }

    private void chooseImages() {
        if (pages.size() >= MAX_PAGES) {
            toast("Limite de 12 paginas para esta prueba");
            return;
        }
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(intent, REQUEST_IMAGES);
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CAPTURE) {
            Uri resultUri = captureUri;
            captureUri = null;
            if (resultCode == RESULT_OK && resultUri != null) {
                appendImage(resultUri);
            } else if (resultUri != null) {
                try { getContentResolver().delete(resultUri, null, null); } catch (Exception ignored) {}
            }
        }
        if (requestCode == REQUEST_IMAGES && resultCode == RESULT_OK && data != null) {
            if (data.getClipData() != null) {
                for (int i = 0; i < data.getClipData().getItemCount() && pages.size() < MAX_PAGES; i++) {
                    appendImage(data.getClipData().getItemAt(i).getUri());
                }
            } else if (data.getData() != null) {
                appendImage(data.getData());
            }
        }
    }

    private void appendImage(Uri uri) {
        if (pages.size() >= MAX_PAGES) return;
        try {
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inJustDecodeBounds = true;
            try (InputStream in = getContentResolver().openInputStream(uri)) {
                BitmapFactory.decodeStream(in, null, opts);
            }
            int sample = 1;
            while (opts.outWidth / sample > 1900 || opts.outHeight / sample > 1900) sample *= 2;
            opts.inJustDecodeBounds = false;
            opts.inSampleSize = sample;
            opts.inPreferredConfig = Bitmap.Config.RGB_565;
            Bitmap bmp;
            try (InputStream in = getContentResolver().openInputStream(uri)) {
                bmp = BitmapFactory.decodeStream(in, null, opts);
            }
            if (bmp == null) throw new Exception("Imagen no compatible");

            int orientation = ExifInterface.ORIENTATION_NORMAL;
            try (InputStream in = getContentResolver().openInputStream(uri)) {
                if (in != null) {
                    ExifInterface exif = new ExifInterface(in);
                    orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
                }
            } catch (Exception ignored) {}
            int angle = orientation == ExifInterface.ORIENTATION_ROTATE_90 ? 90
                : orientation == ExifInterface.ORIENTATION_ROTATE_180 ? 180
                : orientation == ExifInterface.ORIENTATION_ROTATE_270 ? 270 : 0;
            if (angle != 0) {
                Matrix matrix = new Matrix();
                matrix.postRotate(angle);
                Bitmap rotated = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), matrix, true);
                if (rotated != bmp) bmp.recycle();
                bmp = rotated;
            }
            pages.add(bmp);
            selected = pages.size() - 1;
            exportedPdf = null;
            updatePreview();
        } catch (Exception e) {
            toast("No se pudo leer la imagen: " + e.getMessage());
        }
    }

    private void move(int delta) {
        if (!hasPage()) return;
        selected = (selected + delta + pages.size()) % pages.size();
        updatePreview();
    }

    private void replaceCurrent(Bitmap newer) {
        Bitmap old = pages.set(selected, newer);
        if (old != newer) old.recycle();
        exportedPdf = null;
        updatePreview();
    }

    private void rotate() {
        if (!hasPage()) return;
        try {
            Bitmap original = pages.get(selected);
            Matrix m = new Matrix();
            m.postRotate(90);
            replaceCurrent(Bitmap.createBitmap(original, 0, 0, original.getWidth(), original.getHeight(), m, true));
        } catch (Exception ex) {
            toast("No fue posible girar la pagina");
        }
    }

    private int percentage(EditText edit) {
        try {
            int value = Integer.parseInt(edit.getText().toString().trim());
            return Math.max(0, Math.min(40, value));
        } catch (Exception ignored) {
            return 0;
        }
    }

    private void crop() {
        if (!hasPage()) return;
        LinearLayout layout = new LinearLayout(this);
        layout.setPadding(dp(20), dp(6), dp(20), dp(6));
        layout.setOrientation(LinearLayout.VERTICAL);
        TextView note = new TextView(this);
        note.setText("Indica el porcentaje de margen que deseas quitar (0 a 40 por lado).");
        layout.addView(note);
        String[] names = {"Izquierda (%)", "Arriba (%)", "Derecha (%)", "Abajo (%)"};
        EditText[] inputs = new EditText[4];
        for (int i = 0; i < names.length; i++) {
            EditText input = new EditText(this);
            input.setSingleLine(true);
            input.setHint(names[i]);
            input.setInputType(InputType.TYPE_CLASS_NUMBER);
            input.setText("5");
            layout.addView(input);
            inputs[i] = input;
        }
        new AlertDialog.Builder(this).setTitle("Recortar bordes")
            .setView(layout)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Recortar", (dialog, which) -> {
                try {
                    Bitmap b = pages.get(selected);
                    int left = b.getWidth() * percentage(inputs[0]) / 100;
                    int top = b.getHeight() * percentage(inputs[1]) / 100;
                    int right = b.getWidth() * percentage(inputs[2]) / 100;
                    int bottom = b.getHeight() * percentage(inputs[3]) / 100;
                    int w = b.getWidth() - left - right;
                    int h = b.getHeight() - top - bottom;
                    if (w < 100 || h < 100) {
                        toast("Los margenes son demasiado grandes");
                        return;
                    }
                    replaceCurrent(Bitmap.createBitmap(b, left, top, w, h));
                } catch (Exception e) {
                    toast("No fue posible recortar esta pagina");
                }
            }).show();
    }

    private void improve() {
        if (!hasPage()) return;
        try {
            Bitmap b = pages.get(selected);
            Bitmap output = Bitmap.createBitmap(b.getWidth(), b.getHeight(), Bitmap.Config.RGB_565);
            Canvas canvas = new Canvas(output);
            Paint p = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
            ColorMatrix cm = new ColorMatrix(new float[]{
                1.2f, 0, 0, 0, -16,
                0, 1.2f, 0, 0, -16,
                0, 0, 1.2f, 0, -16,
                0, 0, 0, 1, 0
            });
            p.setColorFilter(new ColorMatrixColorFilter(cm));
            canvas.drawBitmap(b, 0, 0, p);
            replaceCurrent(output);
            toast("Contraste mejorado");
        } catch (Exception e) {
            toast("No fue posible mejorar la pagina");
        }
    }

    private void deletePage() {
        if (!hasPage()) return;
        new AlertDialog.Builder(this).setTitle("Eliminar pagina")
            .setMessage("¿Quieres eliminar esta pagina del documento?")
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Eliminar", (dialog, which) -> {
                Bitmap old = pages.remove(selected);
                old.recycle();
                selected = Math.max(0, selected - 1);
                exportedPdf = null;
                updatePreview();
            }).show();
    }

    private void exportPdf() {
        if (!hasPage()) return;
        PdfDocument document = new PdfDocument();
        Uri uri = null;
        try {
            Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
            for (int i = 0; i < pages.size(); i++) {
                Bitmap img = pages.get(i);
                PdfDocument.PageInfo info = new PdfDocument.PageInfo.Builder(595, 842, i + 1).create();
                PdfDocument.Page page = document.startPage(info);
                Canvas canvas = page.getCanvas();
                canvas.drawColor(Color.WHITE);
                float maxW = 545f;
                float maxH = 792f;
                float scale = Math.min(maxW / img.getWidth(), maxH / img.getHeight());
                float width = img.getWidth() * scale;
                float height = img.getHeight() * scale;
                canvas.drawBitmap(img, null,
                    new RectF((595f - width) / 2f, (842f - height) / 2f,
                              (595f + width) / 2f, (842f + height) / 2f), paint);
                document.finishPage(page);
            }
            String stamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
            ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, "SBS_Scan_" + stamp + ".pdf");
            values.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/SBSScannerPDF");
            values.put(MediaStore.MediaColumns.IS_PENDING, 1);
            uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            if (uri == null) throw new Exception("No se pudo crear el archivo de salida");
            try (OutputStream out = getContentResolver().openOutputStream(uri)) {
                if (out == null) throw new Exception("No se pudo abrir el archivo de salida");
                document.writeTo(out);
            }
            ContentValues ready = new ContentValues();
            ready.put(MediaStore.MediaColumns.IS_PENDING, 0);
            getContentResolver().update(uri, ready, null, null);
            exportedPdf = uri;
            toast("PDF guardado en Descargas/SBSScannerPDF");
        } catch (Exception ex) {
            if (uri != null) {
                try { getContentResolver().delete(uri, null, null); } catch (Exception ignored) {}
            }
            toast("Error al crear el PDF: " + ex.getMessage());
        } finally {
            document.close();
        }
    }

    private void openPdf() {
        if (exportedPdf == null) {
            toast("Primero genera un PDF");
            return;
        }
        try {
            Intent view = new Intent(Intent.ACTION_VIEW);
            view.setDataAndType(exportedPdf, "application/pdf");
            view.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(view, "Abrir PDF"));
        } catch (ActivityNotFoundException ex) {
            toast("No hay una aplicacion para abrir PDF");
        }
    }

    private void sharePdf() {
        if (exportedPdf == null) {
            toast("Primero genera un PDF");
            return;
        }
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("application/pdf");
        share.putExtra(Intent.EXTRA_STREAM, exportedPdf);
        share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(share, "Compartir documento PDF"));
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        for (Bitmap bitmap : pages) {
            if (!bitmap.isRecycled()) bitmap.recycle();
        }
        pages.clear();
    }
}
