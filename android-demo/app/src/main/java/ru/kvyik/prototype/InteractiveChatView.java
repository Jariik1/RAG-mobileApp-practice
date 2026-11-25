package ru.kvyik.prototype;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.os.SystemClock;
import android.text.Layout;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;

final class InteractiveChatView extends View {
    private static final int ORANGE = Color.rgb(255, 105, 17);
    private static final int ORANGE_DARK = Color.rgb(255, 75, 8);
    private static final int WHITE = Color.rgb(244, 241, 239);
    private static final int MUTED = Color.rgb(156, 150, 146);

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final float density;
    private final float scaledDensity;
    private final Conversation[] conversations;

    private Shader chatBackgroundShader;
    private int selectedChat;
    private String selectedModel = "gemma-3";
    private float drawerProgress;
    private float modelSheetProgress;
    private float settingsSheetProgress;
    private float contentAlpha = 1f;
    private float contentOffset;
    private boolean cacheEnabled = true;
    private boolean sourcesEnabled = true;
    private float cacheToggle = 1f;
    private float sourcesToggle = 1f;
    private float rippleProgress = -1f;
    private float rippleX;
    private float rippleY;

    InteractiveChatView(Context context) {
        super(context);
        density = getResources().getDisplayMetrics().density;
        scaledDensity = getResources().getDisplayMetrics().scaledDensity;
        conversations = new Conversation[]{
                new Conversation(
                        "Регламент и SLA",
                        "Найди регламент по обработке входящих заявок и кратко перескажи раздел про SLA.",
                        "Нашёл 3 документа в базе знаний. Главное про SLA:",
                        "3 документа",
                        new String[]{"Ответ на заявку — до 4 ч", "Решение P1 — до 24 ч", "Эскалация — после 2-го SLA"},
                        new String[]{"regulation_2025.pdf", "sla_matrix.docx"},
                        "А кто отвечает за эскалацию в нерабочее время?",
                        true),
                new Conversation(
                        "Отпуск и HR",
                        "Сколько дней отпуска и за сколько подавать заявление?",
                        "По HR-регламенту:",
                        "HR-регламенту",
                        new String[]{"28 календарных дней в год", "Заявление — за 2 недели"},
                        new String[]{"hr_policy.pdf"},
                        null,
                        false),
                new Conversation(
                        "Настройка VPN",
                        "Как настроить VPN для удалённого доступа?",
                        "Нашёл инструкцию. Кратко:",
                        "инструкцию",
                        new String[]{"Установить GlobalProtect", "Войти по корп. логину", "Выбрать шлюз msk-gw"},
                        new String[]{"vpn_setup_v2.pdf"},
                        null,
                        false)
        };
        setBackgroundColor(Color.rgb(9, 9, 9));
        setClickable(true);
        setContentDescription("Интерактивный экран kvyik: боковое меню, выбор чата, модели и настройки");
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

        paint.setShader(chatBackgroundShader);
        canvas.drawRect(0, headerHeight, width, height - composerHeight, paint);
        paint.setShader(null);

        int layer = canvas.saveLayerAlpha(0, headerHeight, width, height - composerHeight,
                Math.max(0, Math.min(255, Math.round(contentAlpha * 255))));
        canvas.translate(contentOffset, 0);
        drawConversation(canvas, width, height, headerHeight, composerHeight, conversations[selectedChat]);
        canvas.restoreToCount(layer);

        drawComposer(canvas, width, height, composerHeight);
        drawHeader(canvas, width, headerHeight);
        drawRipple(canvas);

        if (drawerProgress > .001f) drawDrawer(canvas, width, height);
        if (modelSheetProgress > .001f) drawModelSheet(canvas, width, height);
        if (settingsSheetProgress > .001f) drawSettingsSheet(canvas, width, height);

        postInvalidateDelayed(280);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() != MotionEvent.ACTION_UP) return true;
        float x = event.getX();
        float y = event.getY();
        float width = getWidth();
        float height = getHeight();

        if (modelSheetProgress > .5f) {
            float top = height - dp(360);
            if (y < top) {
                animateModelSheet(false);
            } else if (y >= top + dp(88) && y < top + dp(262)) {
                int index = Math.min(2, (int) ((y - top - dp(88)) / dp(58)));
                selectedModel = new String[]{"gemma-3", "qwen2.5", "llama-3.2"}[index];
                performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
                pulse(width / 2, y);
                animateModelSheet(false);
            }
            return true;
        }

        if (settingsSheetProgress > .5f) {
            float top = height - dp(410);
            if (y < top) {
                animateSettingsSheet(false);
            } else if (y >= top + dp(104) && y < top + dp(166)) {
                cacheEnabled = !cacheEnabled;
                animateToggle(true, cacheEnabled);
            } else if (y >= top + dp(178) && y < top + dp(240)) {
                sourcesEnabled = !sourcesEnabled;
                animateToggle(false, sourcesEnabled);
            }
            return true;
        }

        if (drawerProgress > .5f) {
            float drawerWidth = Math.min(width * .86f, dp(340));
            if (x > drawerWidth) {
                animateDrawer(false);
                return true;
            }
            if (y >= dp(140) && y < dp(190)) selectChat(0);
            else if (y >= dp(198) && y < dp(248)) selectChat(1);
            else if (y >= dp(256) && y < dp(306)) selectChat(2);
            else if (y >= dp(365) && y < dp(417)) {
                drawerProgress = 0;
                animateModelSheet(true);
            } else if (y >= dp(430) && y < dp(482)) {
                drawerProgress = 0;
                animateSettingsSheet(true);
            } else if (x > drawerWidth - dp(70) && y < dp(90)) animateDrawer(false);
            return true;
        }

        if (y < dp(78) && x < dp(62)) {
            performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
            animateDrawer(true);
        } else if (y < dp(78) && x >= dp(48) && x < width - dp(85)) {
            animateModelSheet(true);
        } else if (y < dp(78) && x >= width - dp(85)) {
            animateSettingsSheet(true);
        } else if (y > height - dp(72) && (x < dp(62) || x > width - dp(65))) {
            performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
            pulse(x, y);
        }
        return true;
    }

    private void drawConversation(Canvas canvas, float width, float height, float headerHeight,
                                  float composerHeight, Conversation conversation) {
        float y = headerHeight + dp(15);
        y = drawToday(canvas, width, y) + dp(14);

        float contentWidth = width - dp(28);
        float userWidth = Math.min(contentWidth * .72f, dp(300));
        y = drawUserBubble(canvas, width - dp(14) - userWidth, y, userWidth, conversation.query) + dp(14);

        float botWidth = Math.min(contentWidth * .79f, dp(315));
        y = drawBotBubble(canvas, dp(14), y, botWidth, conversation) + dp(14);

        if (conversation.followUp != null) {
            float followWidth = Math.min(contentWidth * .70f, dp(292));
            y = drawUserBubble(canvas, width - dp(14) - followWidth, y, followWidth, conversation.followUp) + dp(14);
        }
        if (conversation.typing) drawTypingBubble(canvas, dp(14), y, Math.min(dp(180), contentWidth * .52f));
    }

    private void drawHeader(Canvas canvas, float width, float height) {
        paint.setColor(Color.rgb(8, 9, 8));
        canvas.drawRect(0, 0, width, height, paint);
        paint.setColor(Color.rgb(34, 32, 30));
        canvas.drawRect(dp(16), height - dp(1), width - dp(16), height, paint);

        paint.setStrokeWidth(dp(2.2f));
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setColor(WHITE);
        for (int i = 0; i < 3; i++) {
            float lineY = dp(31 + i * 7);
            canvas.drawLine(dp(23), lineY, dp(42), lineY, paint);
        }

        drawWordmark(canvas, dp(56), dp(36), sp(17));
        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        textPaint.setTextSize(sp(11));
        textPaint.setColor(ORANGE);
        canvas.drawText(selectedModel + " · online", dp(56), dp(52), textPaint);

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

    private void drawWordmark(Canvas canvas, float x, float y, float size) {
        textPaint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
        textPaint.setTextSize(size);
        textPaint.setColor(WHITE);
        canvas.drawText("kvy", x, y, textPaint);
        x += textPaint.measureText("kvy");
        textPaint.setColor(ORANGE);
        canvas.drawText("[", x, y, textPaint);
        x += textPaint.measureText("[");
        textPaint.setColor(WHITE);
        canvas.drawText("ɪk", x, y, textPaint);
        x += textPaint.measureText("ɪk");
        textPaint.setColor(ORANGE);
        canvas.drawText("]", x, y, textPaint);
    }

    private float drawToday(Canvas canvas, float width, float y) {
        float pillWidth = dp(78);
        float pillHeight = dp(26);
        RectF rect = new RectF((width - pillWidth) / 2, y, (width + pillWidth) / 2, y + pillHeight);
        paint.setColor(Color.rgb(53, 47, 43));
        canvas.drawRoundRect(rect, dp(14), dp(14), paint);
        strokeRoundRect(canvas, rect, dp(14), Color.rgb(85, 75, 68), 1);
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
        paint.setShader(new LinearGradient(x, y, x + width, y + height, ORANGE, ORANGE_DARK, Shader.TileMode.CLAMP));
        canvas.drawRoundRect(rect, dp(17), dp(17), paint);
        paint.setShader(null);
        canvas.save();
        canvas.translate(x + dp(14), y + dp(12));
        layout.draw(canvas);
        canvas.restore();
        return y + height;
    }

    private float drawBotBubble(Canvas canvas, float x, float y, float width, Conversation conversation) {
        StringBuilder raw = new StringBuilder(conversation.answer).append("\n\n");
        for (int i = 0; i < conversation.bullets.length; i++) {
            if (i > 0) raw.append('\n');
            raw.append("•  ").append(conversation.bullets[i]);
        }
        SpannableStringBuilder body = new SpannableStringBuilder(raw.toString());
        style(body, conversation.highlight, ORANGE, true);
        style(body, "•", ORANGE, true);
        body.setSpan(new StyleSpan(Typeface.BOLD), 0, body.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        StaticLayout layout = layout(body, sp(14), Color.rgb(224, 220, 217), (int) (width - dp(28)), 1.18f);
        float chipsHeight = sourcesEnabled ? conversation.sources.length * dp(30) : 0;
        float height = layout.getHeight() + chipsHeight + dp(28);
        RectF rect = new RectF(x, y, x + width, y + height);
        paint.setColor(Color.rgb(28, 26, 25));
        canvas.drawRoundRect(rect, dp(17), dp(17), paint);
        strokeRoundRect(canvas, rect, dp(17), Color.rgb(75, 69, 65), 1);

        canvas.save();
        canvas.translate(x + dp(14), y + dp(13));
        layout.draw(canvas);
        canvas.restore();

        if (sourcesEnabled) {
            float chipY = y + layout.getHeight() + dp(18);
            for (String source : conversation.sources) {
                drawChip(canvas, x + dp(14), chipY, source, width - dp(28));
                chipY += dp(30);
            }
        }
        return y + height;
    }

    private void drawChip(Canvas canvas, float x, float y, String source, float maxWidth) {
        String label = "[ " + source + " ]";
        textPaint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
        textPaint.setTextSize(sp(10));
        float width = Math.min(maxWidth, textPaint.measureText(label) + dp(20));
        RectF rect = new RectF(x, y, x + width, y + dp(24));
        paint.setColor(Color.rgb(50, 31, 20));
        canvas.drawRoundRect(rect, dp(12), dp(12), paint);
        strokeRoundRect(canvas, rect, dp(12), Color.rgb(146, 66, 19), 1);
        textPaint.setColor(Color.rgb(226, 132, 71));
        canvas.drawText(label, x + dp(10), y + dp(16), textPaint);
    }

    private void drawTypingBubble(Canvas canvas, float x, float y, float width) {
        RectF rect = new RectF(x, y, x + width, y + dp(61));
        paint.setColor(Color.rgb(29, 28, 27));
        canvas.drawRoundRect(rect, dp(15), dp(15), paint);
        strokeRoundRect(canvas, rect, dp(15), Color.rgb(57, 55, 53), 1);
        textPaint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
        textPaint.setTextSize(sp(16));
        textPaint.setColor(Color.rgb(138, 134, 131));
        float cursor = x + dp(14);
        canvas.drawText("kvy", cursor, y + dp(24), textPaint);
        cursor += textPaint.measureText("kvy");
        textPaint.setColor(ORANGE);
        canvas.drawText("[", cursor, y + dp(24), textPaint);
        cursor += textPaint.measureText("[") + dp(5);
        int active = (int) ((SystemClock.uptimeMillis() / 280) % 3);
        for (int i = 0; i < 3; i++) {
            paint.setColor(i == active ? ORANGE : Color.rgb(101, 72, 54));
            canvas.drawCircle(cursor + dp(i * 11), y + dp(19), dp(i == active ? 2.7f : 2.1f), paint);
        }
        cursor += dp(35);
        textPaint.setColor(ORANGE);
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
        textPaint.setColor(WHITE);
        canvas.drawText("+", dp(34), centerY + dp(7), textPaint);

        float fieldLeft = dp(60);
        float micX = width - dp(34);
        RectF field = new RectF(fieldLeft, centerY - dp(19), micX - dp(26), centerY + dp(19));
        paint.setColor(Color.rgb(34, 34, 34));
        canvas.drawRoundRect(field, dp(20), dp(20), paint);
        strokeRoundRect(canvas, field, dp(20), Color.rgb(61, 60, 59), 1);
        textPaint.setTextAlign(Paint.Align.LEFT);
        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        textPaint.setTextSize(sp(12));
        textPaint.setColor(Color.rgb(116, 114, 112));
        canvas.drawText("Спросить про документы...", fieldLeft + dp(15), centerY + dp(4), textPaint);

        paint.setColor(ORANGE);
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

    private void drawDrawer(Canvas canvas, float width, float height) {
        paint.setColor(Color.argb(Math.round(155 * drawerProgress), 0, 0, 0));
        canvas.drawRect(0, 0, width, height, paint);
        float drawerWidth = Math.min(width * .86f, dp(340));
        float left = -drawerWidth * (1f - drawerProgress);
        canvas.save();
        canvas.translate(left, 0);
        paint.setColor(Color.rgb(18, 17, 16));
        canvas.drawRect(0, 0, drawerWidth, height, paint);
        paint.setColor(Color.rgb(41, 37, 34));
        canvas.drawRect(drawerWidth - dp(1), 0, drawerWidth, height, paint);

        drawWordmark(canvas, dp(22), dp(46), sp(20));
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        textPaint.setTextSize(sp(28));
        textPaint.setColor(MUTED);
        canvas.drawText("×", drawerWidth - dp(34), dp(46), textPaint);
        textPaint.setTextAlign(Paint.Align.LEFT);

        drawSectionLabel(canvas, "ЧАТЫ", dp(22), dp(116));
        for (int i = 0; i < conversations.length; i++) {
            drawDrawerRow(canvas, conversations[i].title, dp(14), dp(140 + i * 58), drawerWidth - dp(28), i == selectedChat, i + 1);
        }

        drawSectionLabel(canvas, "АССИСТЕНТ", dp(22), dp(342));
        drawDrawerRow(canvas, "Модель · " + selectedModel, dp(14), dp(365), drawerWidth - dp(28), false, 0);
        drawDrawerRow(canvas, "Настройки", dp(14), dp(430), drawerWidth - dp(28), false, -1);

        paint.setColor(Color.rgb(31, 29, 28));
        canvas.drawRoundRect(new RectF(dp(14), height - dp(78), drawerWidth - dp(14), height - dp(18)), dp(18), dp(18), paint);
        paint.setColor(Color.rgb(255, 143, 39));
        canvas.drawCircle(dp(43), height - dp(48), dp(17), paint);
        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        textPaint.setTextSize(sp(10));
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setColor(Color.rgb(35, 16, 5));
        canvas.drawText("SZ", dp(43), height - dp(44), textPaint);
        textPaint.setTextAlign(Paint.Align.LEFT);
        textPaint.setTextSize(sp(13));
        textPaint.setColor(WHITE);
        canvas.drawText("Сотрудник", dp(70), height - dp(51), textPaint);
        textPaint.setTextSize(sp(10));
        textPaint.setColor(MUTED);
        canvas.drawText("Корпоративный доступ", dp(70), height - dp(34), textPaint);
        canvas.restore();
    }

    private void drawDrawerRow(Canvas canvas, String label, float x, float y, float width, boolean selected, int icon) {
        RectF rect = new RectF(x, y, x + width, y + dp(50));
        paint.setColor(selected ? Color.rgb(65, 36, 20) : Color.rgb(27, 25, 24));
        canvas.drawRoundRect(rect, dp(15), dp(15), paint);
        if (selected) strokeRoundRect(canvas, rect, dp(15), Color.rgb(128, 62, 24), 1);
        paint.setColor(selected ? ORANGE : Color.rgb(65, 61, 58));
        canvas.drawCircle(x + dp(25), y + dp(25), dp(12), paint);
        textPaint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
        textPaint.setTextSize(sp(10));
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setColor(selected ? Color.WHITE : Color.rgb(200, 195, 191));
        String marker = icon > 0 ? "0" + icon : icon == 0 ? "AI" : "⚙";
        canvas.drawText(marker, x + dp(25), y + dp(29), textPaint);
        textPaint.setTextAlign(Paint.Align.LEFT);
        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        textPaint.setTextSize(sp(13));
        textPaint.setColor(selected ? Color.WHITE : Color.rgb(215, 211, 208));
        canvas.drawText(label, x + dp(48), y + dp(30), textPaint);
    }

    private void drawSectionLabel(Canvas canvas, String label, float x, float y) {
        textPaint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
        textPaint.setTextSize(sp(9));
        textPaint.setLetterSpacing(.12f);
        textPaint.setColor(Color.rgb(126, 116, 109));
        canvas.drawText(label, x, y, textPaint);
        textPaint.setLetterSpacing(0);
    }

    private void drawModelSheet(Canvas canvas, float width, float height) {
        drawSheetDim(canvas, width, height, modelSheetProgress);
        float sheetHeight = dp(360);
        float top = height - sheetHeight * modelSheetProgress;
        drawSheetSurface(canvas, width, top, height);
        drawSheetHandle(canvas, width, top);
        drawSheetTitle(canvas, "Выбор модели", "Модель применяется к активному чату", top);
        String[] models = {"gemma-3", "qwen2.5", "llama-3.2"};
        String[] captions = {"Быстрая · активна", "Точная · универсальная", "Локальная · компактная"};
        for (int i = 0; i < models.length; i++) {
            float y = top + dp(88 + i * 58);
            boolean selected = models[i].equals(selectedModel);
            RectF row = new RectF(dp(16), y, width - dp(16), y + dp(50));
            paint.setColor(selected ? Color.rgb(63, 35, 20) : Color.rgb(29, 27, 26));
            canvas.drawRoundRect(row, dp(15), dp(15), paint);
            if (selected) strokeRoundRect(canvas, row, dp(15), Color.rgb(137, 64, 20), 1);
            paint.setColor(selected ? ORANGE : Color.rgb(63, 59, 56));
            canvas.drawCircle(dp(41), y + dp(25), dp(11), paint);
            textPaint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
            textPaint.setTextSize(sp(13));
            textPaint.setColor(WHITE);
            canvas.drawText(models[i], dp(62), y + dp(21), textPaint);
            textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
            textPaint.setTextSize(sp(10));
            textPaint.setColor(MUTED);
            canvas.drawText(captions[i], dp(62), y + dp(37), textPaint);
            if (selected) {
                textPaint.setTextAlign(Paint.Align.CENTER);
                textPaint.setTextSize(sp(14));
                textPaint.setColor(ORANGE);
                canvas.drawText("✓", width - dp(38), y + dp(31), textPaint);
                textPaint.setTextAlign(Paint.Align.LEFT);
            }
        }
    }

    private void drawSettingsSheet(Canvas canvas, float width, float height) {
        drawSheetDim(canvas, width, height, settingsSheetProgress);
        float sheetHeight = dp(410);
        float top = height - sheetHeight * settingsSheetProgress;
        drawSheetSurface(canvas, width, top, height);
        drawSheetHandle(canvas, width, top);
        drawSheetTitle(canvas, "Настройки", "Персонализация интерфейса", top);
        drawSettingRow(canvas, width, top + dp(104), "Кэшировать историю", "Быстрый доступ офлайн", cacheToggle);
        drawSettingRow(canvas, width, top + dp(178), "Показывать источники", "Документы под ответами", sourcesToggle);

        RectF about = new RectF(dp(16), top + dp(264), width - dp(16), top + dp(342));
        paint.setColor(Color.rgb(28, 26, 25));
        canvas.drawRoundRect(about, dp(18), dp(18), paint);
        drawWordmark(canvas, dp(34), top + dp(299), sp(15));
        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        textPaint.setTextSize(sp(11));
        textPaint.setColor(MUTED);
        canvas.drawText("Интерфейсный прототип · v1.0", dp(34), top + dp(324), textPaint);
    }

    private void drawSettingRow(Canvas canvas, float width, float y, String title, String caption, float value) {
        RectF row = new RectF(dp(16), y, width - dp(16), y + dp(62));
        paint.setColor(Color.rgb(29, 27, 26));
        canvas.drawRoundRect(row, dp(17), dp(17), paint);
        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        textPaint.setTextSize(sp(13));
        textPaint.setColor(WHITE);
        canvas.drawText(title, dp(34), y + dp(26), textPaint);
        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        textPaint.setTextSize(sp(10));
        textPaint.setColor(MUTED);
        canvas.drawText(caption, dp(34), y + dp(44), textPaint);

        float toggleLeft = width - dp(75);
        float toggleTop = y + dp(16);
        paint.setColor(blend(Color.rgb(69, 65, 62), ORANGE, value));
        canvas.drawRoundRect(new RectF(toggleLeft, toggleTop, toggleLeft + dp(45), toggleTop + dp(28)), dp(14), dp(14), paint);
        paint.setColor(Color.WHITE);
        canvas.drawCircle(toggleLeft + dp(14 + 17 * value), toggleTop + dp(14), dp(10), paint);
    }

    private void drawSheetDim(Canvas canvas, float width, float height, float progress) {
        paint.setColor(Color.argb(Math.round(165 * progress), 0, 0, 0));
        canvas.drawRect(0, 0, width, height, paint);
    }

    private void drawSheetSurface(Canvas canvas, float width, float top, float height) {
        paint.setColor(Color.rgb(18, 17, 16));
        canvas.drawRoundRect(new RectF(0, top, width, height + dp(30)), dp(28), dp(28), paint);
        strokeRoundRect(canvas, new RectF(0, top, width, height + dp(30)), dp(28), Color.rgb(62, 56, 52), 1);
    }

    private void drawSheetHandle(Canvas canvas, float width, float top) {
        paint.setColor(Color.rgb(81, 76, 72));
        canvas.drawRoundRect(new RectF(width / 2 - dp(22), top + dp(10), width / 2 + dp(22), top + dp(14)), dp(2), dp(2), paint);
    }

    private void drawSheetTitle(Canvas canvas, String title, String subtitle, float top) {
        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        textPaint.setTextSize(sp(20));
        textPaint.setColor(WHITE);
        canvas.drawText(title, dp(22), top + dp(48), textPaint);
        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        textPaint.setTextSize(sp(11));
        textPaint.setColor(MUTED);
        canvas.drawText(subtitle, dp(22), top + dp(69), textPaint);
    }

    private void drawRipple(Canvas canvas) {
        if (rippleProgress < 0) return;
        paint.setColor(Color.argb(Math.round(105 * (1f - rippleProgress)), 255, 125, 35));
        canvas.drawCircle(rippleX, rippleY, dp(12 + 34 * rippleProgress), paint);
    }

    private void animateDrawer(boolean open) {
        ValueAnimator animator = ValueAnimator.ofFloat(drawerProgress, open ? 1f : 0f);
        animator.setDuration(open ? 320 : 230);
        animator.setInterpolator(open ? new OvershootInterpolator(.78f) : new DecelerateInterpolator());
        animator.addUpdateListener(value -> {
            drawerProgress = (float) value.getAnimatedValue();
            invalidate();
        });
        animator.start();
    }

    private void animateModelSheet(boolean open) {
        if (open) {
            settingsSheetProgress = 0;
            drawerProgress = 0;
        }
        ValueAnimator animator = ValueAnimator.ofFloat(modelSheetProgress, open ? 1f : 0f);
        animator.setDuration(280);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(value -> {
            modelSheetProgress = (float) value.getAnimatedValue();
            invalidate();
        });
        animator.start();
    }

    private void animateSettingsSheet(boolean open) {
        if (open) {
            modelSheetProgress = 0;
            drawerProgress = 0;
        }
        ValueAnimator animator = ValueAnimator.ofFloat(settingsSheetProgress, open ? 1f : 0f);
        animator.setDuration(280);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(value -> {
            settingsSheetProgress = (float) value.getAnimatedValue();
            invalidate();
        });
        animator.start();
    }

    private void selectChat(int index) {
        animateDrawer(false);
        if (index == selectedChat) return;
        boolean[] switched = {false};
        ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(360);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(value -> {
            float phase = (float) value.getAnimatedValue();
            if (phase < .5f) {
                contentAlpha = 1f - phase * 2f;
                contentOffset = -dp(18) * phase * 2f;
            } else {
                if (!switched[0]) {
                    selectedChat = index;
                    switched[0] = true;
                }
                contentAlpha = (phase - .5f) * 2f;
                contentOffset = dp(18) * (1f - phase) * 2f;
            }
            invalidate();
        });
        animator.start();
    }

    private void animateToggle(boolean cache, boolean enabled) {
        performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
        float start = cache ? cacheToggle : sourcesToggle;
        ValueAnimator animator = ValueAnimator.ofFloat(start, enabled ? 1f : 0f);
        animator.setDuration(220);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(value -> {
            if (cache) cacheToggle = (float) value.getAnimatedValue();
            else sourcesToggle = (float) value.getAnimatedValue();
            invalidate();
        });
        animator.start();
    }

    private void pulse(float x, float y) {
        rippleX = x;
        rippleY = y;
        ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(360);
        animator.addUpdateListener(value -> {
            rippleProgress = (float) value.getAnimatedValue();
            invalidate();
        });
        animator.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                rippleProgress = -1f;
            }
        });
        animator.start();
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

    private void strokeRoundRect(Canvas canvas, RectF rect, float radius, int color, float width) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(width));
        paint.setColor(color);
        canvas.drawRoundRect(rect, radius, radius, paint);
        paint.setStyle(Paint.Style.FILL);
    }

    private int blend(int from, int to, float amount) {
        return Color.rgb(
                Math.round(Color.red(from) + (Color.red(to) - Color.red(from)) * amount),
                Math.round(Color.green(from) + (Color.green(to) - Color.green(from)) * amount),
                Math.round(Color.blue(from) + (Color.blue(to) - Color.blue(from)) * amount));
    }

    private float dp(float value) {
        return value * density;
    }

    private float sp(float value) {
        return value * scaledDensity;
    }

    private static final class Conversation {
        final String title;
        final String query;
        final String answer;
        final String highlight;
        final String[] bullets;
        final String[] sources;
        final String followUp;
        final boolean typing;

        Conversation(String title, String query, String answer, String highlight, String[] bullets,
                     String[] sources, String followUp, boolean typing) {
            this.title = title;
            this.query = query;
            this.answer = answer;
            this.highlight = highlight;
            this.bullets = bullets;
            this.sources = sources;
            this.followUp = followUp;
            this.typing = typing;
        }
    }
}
