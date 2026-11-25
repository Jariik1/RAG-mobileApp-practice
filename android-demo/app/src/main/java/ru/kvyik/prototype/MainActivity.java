package ru.kvyik.prototype;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.InputType;
import android.text.Layout;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.method.PasswordTransformationMethod;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public final class MainActivity extends Activity {
    private static final String DEMO_LOGIN = "admin";
    private static final String DEMO_PASSWORD = "kvyik2026";

    private final int orange = Color.rgb(255, 106, 19);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window window = getWindow();
        window.setStatusBarColor(Color.rgb(8, 8, 8));
        window.setNavigationBarColor(Color.rgb(8, 8, 8));
        showLogin();
    }

    private void showLogin() {
        FrameLayout root = new FrameLayout(this);
        root.setBackground(background(new int[]{Color.rgb(57, 24, 9), Color.rgb(18, 13, 10), Color.rgb(7, 7, 7)}, 0, 0));
        root.addView(new LoginGlowView(this), new FrameLayout.LayoutParams(-1, -1));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setGravity(Gravity.CENTER_HORIZONTAL);
        page.setPadding(dp(24), dp(36), dp(24), dp(36));
        scroll.addView(page, new ScrollView.LayoutParams(-1, -1));

        TextView logo = text("kvy[ɪk]", 28, Color.WHITE, Typeface.BOLD);
        logo.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        SpannableStringBuilder logoText = new SpannableStringBuilder("kvy[ɪk]");
        logoText.setSpan(new ForegroundColorSpan(orange), 3, 4, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        logoText.setSpan(new ForegroundColorSpan(orange), 6, 7, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        logo.setText(logoText);
        page.addView(logo);

        TextView eyebrow = text("ВНУТРИ ЗНАНИЙ", 11, orange, Typeface.BOLD);
        eyebrow.setLetterSpacing(.20f);
        LinearLayout.LayoutParams eyebrowParams = new LinearLayout.LayoutParams(-2, -2);
        eyebrowParams.topMargin = dp(7);
        eyebrowParams.bottomMargin = dp(42);
        page.addView(eyebrow, eyebrowParams);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(24), dp(26), dp(24), dp(24));
        card.setBackground(panel(Color.rgb(20, 18, 17), Color.rgb(66, 57, 51), 28, 1));
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(-1, -2);
        cardParams.width = Math.min(getResources().getDisplayMetrics().widthPixels - dp(48), dp(420));
        page.addView(card, cardParams);

        TextView title = text("Вход в kvyik", 27, Color.WHITE, Typeface.BOLD);
        card.addView(title);

        TextView subtitle = text("Корпоративный AI-ассистент и поиск по базе знаний", 14, Color.rgb(168, 160, 155), Typeface.NORMAL);
        subtitle.setLineSpacing(0, 1.25f);
        LinearLayout.LayoutParams subtitleParams = new LinearLayout.LayoutParams(-1, -2);
        subtitleParams.topMargin = dp(8);
        subtitleParams.bottomMargin = dp(24);
        card.addView(subtitle, subtitleParams);

        EditText login = input("Логин");
        login.setSingleLine(true);
        login.setInputType(InputType.TYPE_CLASS_TEXT);
        login.setImeOptions(EditorInfo.IME_ACTION_NEXT);
        card.addView(login, fieldParams());

        EditText password = input("Пароль");
        password.setSingleLine(true);
        password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        password.setTransformationMethod(PasswordTransformationMethod.getInstance());
        password.setImeOptions(EditorInfo.IME_ACTION_DONE);
        LinearLayout.LayoutParams passwordParams = fieldParams();
        passwordParams.topMargin = dp(12);
        card.addView(password, passwordParams);

        TextView error = text("Неверный логин или пароль", 13, Color.rgb(255, 139, 99), Typeface.BOLD);
        error.setVisibility(View.GONE);
        LinearLayout.LayoutParams errorParams = new LinearLayout.LayoutParams(-1, -2);
        errorParams.topMargin = dp(12);
        card.addView(error, errorParams);

        Button enter = new Button(this);
        enter.setText("ВОЙТИ");
        enter.setTextColor(Color.WHITE);
        enter.setTextSize(14);
        enter.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        enter.setLetterSpacing(.12f);
        enter.setAllCaps(false);
        enter.setStateListAnimator(null);
        enter.setBackground(panel(orange, orange, 18, 0));
        LinearLayout.LayoutParams enterParams = new LinearLayout.LayoutParams(-1, dp(56));
        enterParams.topMargin = dp(20);
        card.addView(enter, enterParams);

        View.OnClickListener submit = view -> {
            boolean valid = DEMO_LOGIN.equals(login.getText().toString().trim())
                    && DEMO_PASSWORD.equals(password.getText().toString());
            if (valid) {
                error.setVisibility(View.GONE);
                InputMethodManager keyboard = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                keyboard.hideSoftInputFromWindow(password.getWindowToken(), 0);
                setContentView(new InteractiveChatView(this));
            } else {
                error.setVisibility(View.VISIBLE);
                password.setText("");
                password.requestFocus();
            }
        };
        enter.setOnClickListener(submit);
        password.setOnEditorActionListener((view, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                enter.performClick();
                return true;
            }
            return false;
        });

        root.addView(scroll, new FrameLayout.LayoutParams(-1, -1));
        setContentView(root);
    }

    private EditText input(String hint) {
        EditText field = new EditText(this);
        field.setHint(hint);
        field.setHintTextColor(Color.rgb(126, 119, 115));
        field.setTextColor(Color.WHITE);
        field.setTextSize(15);
        field.setPadding(dp(17), 0, dp(17), 0);
        field.setBackground(panel(Color.rgb(38, 35, 33), Color.rgb(74, 68, 64), 17, 1));
        return field;
    }

    private LinearLayout.LayoutParams fieldParams() {
        return new LinearLayout.LayoutParams(-1, dp(56));
    }

    private TextView text(String value, float size, int color, int style) {
        TextView text = new TextView(this);
        text.setText(value);
        text.setTextSize(size);
        text.setTextColor(color);
        text.setTypeface(Typeface.DEFAULT, style);
        return text;
    }

    private android.graphics.drawable.GradientDrawable background(int[] colors, int radius, int stroke) {
        android.graphics.drawable.GradientDrawable drawable = new android.graphics.drawable.GradientDrawable(
                android.graphics.drawable.GradientDrawable.Orientation.TL_BR, colors);
        drawable.setCornerRadius(dp(radius));
        if (stroke > 0) drawable.setStroke(dp(stroke), colors[colors.length - 1]);
        return drawable;
    }

    private android.graphics.drawable.GradientDrawable panel(int fill, int strokeColor, int radius, int stroke) {
        android.graphics.drawable.GradientDrawable drawable = new android.graphics.drawable.GradientDrawable();
        drawable.setColor(fill);
        drawable.setCornerRadius(dp(radius));
        if (stroke > 0) drawable.setStroke(dp(stroke), strokeColor);
        return drawable;
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static final class LoginGlowView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private Shader glowShader;

        LoginGlowView(Context context) {
            super(context);
        }

        @Override
        protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
            float radius = Math.max(width, height) * .72f;
            glowShader = new RadialGradient(width * .80f, height * .15f, radius,
                    new int[]{Color.argb(115, 255, 104, 15), Color.argb(20, 255, 80, 5), Color.TRANSPARENT},
                    new float[]{0f, .42f, 1f}, Shader.TileMode.CLAMP);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            paint.setShader(glowShader);
            canvas.drawRect(0, 0, getWidth(), getHeight(), paint);
        }
    }

    private static final class KvyikChatView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final TextPaint textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        private final int orange = Color.rgb(255, 105, 17);
        private final int orangeDark = Color.rgb(255, 75, 8);
        private final int white = Color.rgb(244, 241, 239);
        private final int muted = Color.rgb(156, 150, 146);
        private final float density;
        private final float scaledDensity;
        private Shader chatBackgroundShader;

        KvyikChatView(Context context) {
            super(context);
            density = getResources().getDisplayMetrics().density;
            scaledDensity = getResources().getDisplayMetrics().scaledDensity;
            setBackgroundColor(Color.rgb(9, 9, 9));
            setContentDescription("Экран чата kvyik с примером поиска регламента и источниками");
        }

        @Override
        protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
            float headerHeight = dp(78);
            chatBackgroundShader = new RadialGradient(dp(40), headerHeight + dp(150), dp(300),
                    new int[]{Color.rgb(38, 19, 11), Color.rgb(15, 13, 12), Color.rgb(10, 10, 10)},
                    new float[]{0f, .52f, 1f}, Shader.TileMode.CLAMP);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float width = getWidth();
            float height = getHeight();
            float headerHeight = dp(78);
            float composerHeight = dp(72);

            drawHeader(canvas, width, headerHeight);

            paint.setShader(chatBackgroundShader);
            canvas.drawRect(0, headerHeight, width, height - composerHeight, paint);
            paint.setShader(null);

            float y = headerHeight + dp(15);
            y = drawToday(canvas, width, y) + dp(14);

            float contentWidth = width - dp(28);
            float userWidth = Math.min(contentWidth * .72f, dp(300));
            float userX = width - dp(14) - userWidth;
            y = drawUserBubble(canvas, userX, y, userWidth,
                    "Найди регламент по обработке входящих заявок и кратко перескажи раздел про SLA.") + dp(14);

            float botWidth = Math.min(contentWidth * .79f, dp(315));
            y = drawBotBubble(canvas, dp(14), y, botWidth) + dp(14);

            float followWidth = Math.min(contentWidth * .70f, dp(292));
            float followX = width - dp(14) - followWidth;
            y = drawUserBubble(canvas, followX, y, followWidth,
                    "А кто отвечает за эскалацию в нерабочее время?") + dp(14);

            drawTypingBubble(canvas, dp(14), y, Math.min(dp(180), contentWidth * .52f));
            drawComposer(canvas, width, height, composerHeight);

            postInvalidateDelayed(280);
        }

        private void drawHeader(Canvas canvas, float width, float height) {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.rgb(8, 9, 8));
            canvas.drawRect(0, 0, width, height, paint);
            paint.setColor(Color.rgb(34, 32, 30));
            canvas.drawRect(dp(16), height - dp(1), width - dp(16), height, paint);

            paint.setStrokeWidth(dp(2.2f));
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setColor(white);
            for (int i = 0; i < 3; i++) {
                float lineY = dp(31 + i * 7);
                canvas.drawLine(dp(23), lineY, dp(42), lineY, paint);
            }

            textPaint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
            textPaint.setTextSize(sp(17));
            textPaint.setColor(white);
            float logoX = dp(56);
            float logoY = dp(36);
            canvas.drawText("kvy", logoX, logoY, textPaint);
            float x = logoX + textPaint.measureText("kvy");
            textPaint.setColor(orange);
            canvas.drawText("[", x, logoY, textPaint);
            x += textPaint.measureText("[");
            textPaint.setColor(white);
            canvas.drawText("ɪk", x, logoY, textPaint);
            x += textPaint.measureText("ɪk");
            textPaint.setColor(orange);
            canvas.drawText("]", x, logoY, textPaint);

            textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            textPaint.setTextSize(sp(11));
            textPaint.setColor(orange);
            canvas.drawText("gemma-3 · online", logoX, dp(52), textPaint);

            float avatarX = width - dp(39);
            float avatarY = dp(38);
            paint.setColor(Color.rgb(255, 143, 39));
            canvas.drawCircle(avatarX, avatarY, dp(18), paint);
            textPaint.setTextSize(sp(11));
            textPaint.setColor(Color.rgb(33, 15, 4));
            textPaint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("SZ", avatarX, avatarY + dp(4), textPaint);
            textPaint.setTextAlign(Paint.Align.LEFT);
        }

        private float drawToday(Canvas canvas, float width, float y) {
            float pillWidth = dp(78);
            float pillHeight = dp(26);
            RectF rect = new RectF((width - pillWidth) / 2, y, (width + pillWidth) / 2, y + pillHeight);
            paint.setColor(Color.rgb(53, 47, 43));
            canvas.drawRoundRect(rect, dp(14), dp(14), paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(1));
            paint.setColor(Color.rgb(85, 75, 68));
            canvas.drawRoundRect(rect, dp(14), dp(14), paint);
            paint.setStyle(Paint.Style.FILL);
            textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            textPaint.setTextSize(sp(11));
            textPaint.setColor(Color.rgb(174, 167, 161));
            textPaint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("Сегодня", width / 2, y + dp(17), textPaint);
            textPaint.setTextAlign(Paint.Align.LEFT);
            return y + pillHeight;
        }

        private float drawUserBubble(Canvas canvas, float x, float y, float width, String value) {
            SpannableStringBuilder text = new SpannableStringBuilder(value);
            text.setSpan(new StyleSpan(Typeface.BOLD), 0, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            StaticLayout layout = layout(text, sp(15), Color.WHITE, (int) (width - dp(28)), 1.12f);
            float height = layout.getHeight() + dp(24);
            RectF rect = new RectF(x, y, x + width, y + height);
            paint.setShader(new LinearGradient(x, y, x + width, y + height, orange, orangeDark, Shader.TileMode.CLAMP));
            canvas.drawRoundRect(rect, dp(17), dp(17), paint);
            paint.setShader(null);
            canvas.save();
            canvas.translate(x + dp(14), y + dp(12));
            layout.draw(canvas);
            canvas.restore();
            return y + height;
        }

        private float drawBotBubble(Canvas canvas, float x, float y, float width) {
            SpannableStringBuilder body = new SpannableStringBuilder(
                    "Нашёл 3 документа в базе знаний. Главное про SLA:\n\n" +
                    "•  Ответ на заявку — до 4 ч\n" +
                    "•  Решение P1 — до 24 ч\n" +
                    "•  Эскалация — после 2-го SLA");
            style(body, "3 документа", orange, true);
            style(body, "•", orange, true);
            body.setSpan(new StyleSpan(Typeface.BOLD), 0, body.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            StaticLayout layout = layout(body, sp(14), Color.rgb(224, 220, 217), (int) (width - dp(28)), 1.18f);
            float chipHeight = dp(30);
            float height = layout.getHeight() + chipHeight * 2 + dp(32);
            RectF rect = new RectF(x, y, x + width, y + height);
            paint.setColor(Color.rgb(28, 26, 25));
            canvas.drawRoundRect(rect, dp(17), dp(17), paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(1));
            paint.setColor(Color.rgb(75, 69, 65));
            canvas.drawRoundRect(rect, dp(17), dp(17), paint);
            paint.setStyle(Paint.Style.FILL);

            canvas.save();
            canvas.translate(x + dp(14), y + dp(13));
            layout.draw(canvas);
            canvas.restore();

            float chipY = y + layout.getHeight() + dp(18);
            drawChip(canvas, x + dp(14), chipY, "[ regulation_2025.pdf ]", dp(180));
            drawChip(canvas, x + dp(14), chipY + chipHeight, "[ sla_matrix.docx ]", dp(158));
            return y + height;
        }

        private void drawChip(Canvas canvas, float x, float y, String label, float width) {
            RectF rect = new RectF(x, y, x + width, y + dp(24));
            paint.setColor(Color.rgb(50, 31, 20));
            canvas.drawRoundRect(rect, dp(12), dp(12), paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(1));
            paint.setColor(Color.rgb(146, 66, 19));
            canvas.drawRoundRect(rect, dp(12), dp(12), paint);
            paint.setStyle(Paint.Style.FILL);
            textPaint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
            textPaint.setTextSize(sp(10));
            textPaint.setColor(Color.rgb(226, 132, 71));
            canvas.drawText(label, x + dp(10), y + dp(16), textPaint);
        }

        private void drawTypingBubble(Canvas canvas, float x, float y, float width) {
            RectF rect = new RectF(x, y, x + width, y + dp(61));
            paint.setColor(Color.rgb(29, 28, 27));
            canvas.drawRoundRect(rect, dp(15), dp(15), paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(1));
            paint.setColor(Color.rgb(57, 55, 53));
            canvas.drawRoundRect(rect, dp(15), dp(15), paint);
            paint.setStyle(Paint.Style.FILL);

            textPaint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
            textPaint.setTextSize(sp(16));
            textPaint.setColor(Color.rgb(138, 134, 131));
            float cursor = x + dp(14);
            canvas.drawText("kvy", cursor, y + dp(24), textPaint);
            cursor += textPaint.measureText("kvy");
            textPaint.setColor(orange);
            canvas.drawText("[", cursor, y + dp(24), textPaint);
            cursor += textPaint.measureText("[") + dp(5);
            int active = (int) ((SystemClock.uptimeMillis() / 280) % 3);
            for (int i = 0; i < 3; i++) {
                paint.setColor(i == active ? orange : Color.rgb(101, 72, 54));
                canvas.drawCircle(cursor + dp(i * 11), y + dp(19), dp(i == active ? 2.7f : 2.1f), paint);
            }
            cursor += dp(35);
            textPaint.setColor(orange);
            canvas.drawText("]", cursor, y + dp(24), textPaint);
            textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            textPaint.setTextSize(sp(10));
            textPaint.setColor(Color.rgb(101, 98, 95));
            canvas.drawText("ищу в базе знаний...", x + dp(14), y + dp(45), textPaint);
        }

        private void drawComposer(Canvas canvas, float width, float height, float composerHeight) {
            float top = height - composerHeight;
            paint.setColor(Color.rgb(9, 10, 10));
            canvas.drawRect(0, top, width, height, paint);
            paint.setColor(Color.rgb(63, 61, 59));
            canvas.drawRect(dp(14), top, width - dp(14), top + dp(2), paint);

            float centerY = top + dp(39);
            paint.setColor(Color.rgb(40, 40, 40));
            canvas.drawCircle(dp(34), centerY, dp(19), paint);
            textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
            textPaint.setTextSize(sp(22));
            textPaint.setTextAlign(Paint.Align.CENTER);
            textPaint.setColor(white);
            canvas.drawText("+", dp(34), centerY + dp(7), textPaint);

            float fieldLeft = dp(60);
            float micX = width - dp(34);
            RectF field = new RectF(fieldLeft, centerY - dp(19), micX - dp(26), centerY + dp(19));
            paint.setColor(Color.rgb(34, 34, 34));
            canvas.drawRoundRect(field, dp(20), dp(20), paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(1));
            paint.setColor(Color.rgb(61, 60, 59));
            canvas.drawRoundRect(field, dp(20), dp(20), paint);
            paint.setStyle(Paint.Style.FILL);
            textPaint.setTextAlign(Paint.Align.LEFT);
            textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            textPaint.setTextSize(sp(12));
            textPaint.setColor(Color.rgb(116, 114, 112));
            canvas.drawText("Спросить про документы...", fieldLeft + dp(15), centerY + dp(4), textPaint);

            paint.setColor(orange);
            canvas.drawCircle(micX, centerY, dp(19), paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(1.8f));
            paint.setColor(Color.WHITE);
            RectF capsule = new RectF(micX - dp(4), centerY - dp(8), micX + dp(4), centerY + dp(5));
            canvas.drawRoundRect(capsule, dp(5), dp(5), paint);
            canvas.drawArc(new RectF(micX - dp(8), centerY - dp(3), micX + dp(8), centerY + dp(10)), 0, 180, false, paint);
            canvas.drawLine(micX, centerY + dp(10), micX, centerY + dp(14), paint);
            paint.setStyle(Paint.Style.FILL);
        }

        private StaticLayout layout(CharSequence text, float size, int color, int width, float spacing) {
            textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
            textPaint.setTextSize(size);
            textPaint.setColor(color);
            textPaint.setTextAlign(Paint.Align.LEFT);
            return StaticLayout.Builder.obtain(text, 0, text.length(), textPaint, width)
                    .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                    .setLineSpacing(0, spacing)
                    .setIncludePad(false)
                    .build();
        }

        private void style(SpannableStringBuilder text, String needle, int color, boolean all) {
            int start = text.toString().indexOf(needle);
            while (start >= 0) {
                int end = start + needle.length();
                text.setSpan(new ForegroundColorSpan(color), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                if (!all) return;
                start = text.toString().indexOf(needle, end);
            }
        }

        private float dp(float value) {
            return value * density;
        }

        private float sp(float value) {
            return value * scaledDensity;
        }
    }
}
