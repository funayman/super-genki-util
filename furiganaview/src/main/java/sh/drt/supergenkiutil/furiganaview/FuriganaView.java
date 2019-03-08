/*
 * Original code credit goes to:
 * FuriganaView widget
 * Copyright (C) 2013 sh0 <sh0@yutani.ee>
 * Licensed under Creative Commons BY-SA 3.0
 *
 * updates made by drt
 */

package sh.drt.supergenkiutil.furiganaview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Typeface;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.View;
import java.util.Vector;

public class FuriganaView extends View {

  // Attributes
  private int baseColor;
  private int highlightColor;
  private int furiganaColor;
  private float baseTextSize;

  // Paints
  private TextPaint furiganaPaint;
  private TextPaint normalTextPaint;
  private TextPaint highlightTextPaint;

  // Sizes
  private float lineSize;
  private float normalHeight;
  private float furiganaHeight;
  private float lineMax;

  // Spans and lines
  private Vector<Span> spanVector = new Vector<>();
  private Vector<LineNormal> lineNormalVector = new Vector<>();
  private Vector<LineFurigana> lineFuriganaVector = new Vector<>();

  // Constructors
  public FuriganaView(Context context, AttributeSet attrs) {
    super(context, attrs);
    init(context, attrs);
  }

  private void init(Context context, AttributeSet attrs) {
    TypedArray ta = context.getTheme()
        .obtainStyledAttributes(attrs, R.styleable.FuriganaView, 0, 0);
    baseColor = ta.getColor(R.styleable.FuriganaView_baseColor,
        getResources().getColor(R.color.defaultBaseColor));
    highlightColor = ta.getColor(R.styleable.FuriganaView_highlightColor,
        getResources().getColor(R.color.defaultHighlightColor));
    furiganaColor = ta.getColor(R.styleable.FuriganaView_furiganaColor,
        getResources().getColor(R.color.defaultFuriganaColor));
    baseTextSize = ta.getDimensionPixelSize(R.styleable.FuriganaView_baseTextSize, 36);

    // main text
    normalTextPaint = new TextPaint();
    normalTextPaint.setColor(baseColor);
    normalTextPaint.setTextSize(baseTextSize);
    normalTextPaint.setTypeface(Typeface.DEFAULT);
    normalTextPaint.setFakeBoldText(true);

    // highlighted text
    highlightTextPaint = new TextPaint();
    highlightTextPaint.setColor(highlightColor);
    highlightTextPaint.setTextSize(baseTextSize);
    highlightTextPaint.setFakeBoldText(true);

    // furigana text
    furiganaPaint = new TextPaint();
    furiganaPaint.setColor(furiganaColor);
    furiganaPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
    furiganaPaint.setTextSize(normalTextPaint.getTextSize() / 2.0f);
  }

  // Getters and Setters for Attributes
  public int getBaseColor() {
    return baseColor;
  }

  public void setBaseColor(int baseColor) {
    this.baseColor = baseColor;
  }

  public int getHighlightColor() {
    return highlightColor;
  }

  public void setHighlightColor(int highlightColor) {
    this.highlightColor = highlightColor;
  }

  public int getFuriganaColor() {
    return furiganaColor;
  }

  public void setFuriganaColor(int furiganaColor) {
    this.furiganaColor = furiganaColor;
  }

  public float getBaseTextSize() {
    return baseTextSize;
  }

  public void setBaseTextSize(float baseTextSize) {
    this.baseTextSize = baseTextSize;
  }

  // Text functions
  private void calculateAndSetText(String text, int startHightlight, int endHightlight) {

    // Linesize
    normalHeight = normalTextPaint.descent() - normalTextPaint.ascent();
    furiganaHeight = furiganaPaint.descent() - furiganaPaint.ascent();
    lineSize = normalHeight + furiganaHeight;

    // Clear spans
    spanVector.clear();

    // Sizes
    lineSize = furiganaPaint.getFontSpacing() + Math
        .max(normalTextPaint.getFontSpacing(), highlightTextPaint.getFontSpacing());

    // Spannify text
    while (text.length() > 0) {
      int idx = text.indexOf('{');
      if (idx >= 0) {
        // Prefix string
        if (idx > 0) {
          // Spans
          spanVector.add(new Span("", text.substring(0, idx), startHightlight, endHightlight));

          // Remove text
          text = text.substring(idx);
          startHightlight -= idx;
          endHightlight -= idx;
        }

        // End bracket
        idx = text.indexOf('}');
        if (idx < 1) {
          // Error
          text = "";
          break;
        } else if (idx == 1) {
          // Empty bracket
          text = text.substring(2);
          continue;
        }

        // Spans
        String[] split = text.substring(1, idx).split(";");
        spanVector.add(new Span(((split.length > 1) ? split[1] : ""), split[0], startHightlight, endHightlight));

        // Remove text
        text = text.substring(idx + 1);
        startHightlight -= split[0].length();
        endHightlight -= split[0].length();

      } else {
        // Single span
        spanVector.add(new Span("", text, startHightlight, endHightlight));
        text = "";
      }
    }

    // Invalidate view
    this.invalidate();
    this.requestLayout();
  }

  private void calculateText(float lineMax) {
    // Clear lines
    lineNormalVector.clear();
    lineFuriganaVector.clear();

    // Sizes
    lineMax = 0.0f;

    // Check if no limits on width
    if (lineMax < 0.0) {

      // Create single normal and furigana line
      LineNormal lineNormal = new LineNormal();
      LineFurigana lineFurigana = new LineFurigana();

      // Loop spans
      for (Span span : spanVector) {
        // Text
        lineNormal.add(span.normal());
        lineFurigana.add(span.furigana(lineMax));

        // Widths update
        for (float width : span.widths()) {
          lineMax += width;
        }
      }

      // Commit both lines
      lineNormalVector.add(lineNormal);
      lineFuriganaVector.add(lineFurigana);

    } else {

      // Lines
      float linex = 0.0f;
      LineNormal lineNormal = new LineNormal();
      LineFurigana lineFurigana = new LineFurigana();

      // Initial span
      int spani = 0;

      Span span = null;
      if (spanVector.size() != 0) {
        span = spanVector.get(spani);
      }

      // Iterate
      while (span != null) {
        // Start offset
        float lines = linex;

        // Calculate possible line size
        Vector<Float> widths = span.widths();
        int i = 0;
        for (i = 0; i < widths.size(); i++) {
          if (linex + widths.get(i) <= lineMax) {
            linex += widths.get(i);
          } else {
            break;
          }
        }

        // Add span to line
        if (i >= 0 && i < widths.size()) {

          // Span does not fit entirely
          if (i > 0) {
            // Split half that fits
            Vector<TextNormal> normalA = new Vector<>();
            Vector<TextNormal> normalB = new Vector<>();
            span.split(i, normalA, normalB);
            lineNormal.add(normalA);
            span = new Span(normalB);
          }

          // Add new line with current spans
          if (lineNormal.size() != 0) {
            // Add
            lineMax = (lineMax > linex ? lineMax : linex);
            lineNormalVector.add(lineNormal);
            lineFuriganaVector.add(lineFurigana);

            // Reset
            lineNormal = new LineNormal();
            lineFurigana = new LineFurigana();
            linex = 0.0f;

            // Next span
            continue;
          }

        } else {

          // Span fits entirely
          lineNormal.add(span.normal());
          lineFurigana.add(span.furigana(lines));

        }

        // Next span
        span = null;
        spani++;
        if (spani < spanVector.size()) {
          span = spanVector.get(spani);
        }
      }

      // Last span
      if (lineNormal.size() != 0) {
        // Add
        lineMax = (lineMax > linex ? lineMax : linex);
        lineNormalVector.add(lineNormal);
        lineFuriganaVector.add(lineFurigana);
      }
    }

    // Calculate furigana
    for (LineFurigana line : lineFuriganaVector) {
      line.calculate();
    }
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    // Modes
    int wmode = MeasureSpec.getMode(widthMeasureSpec);
    int hmode = MeasureSpec.getMode(heightMeasureSpec);

    // Dimensions
    int wold = MeasureSpec.getSize(widthMeasureSpec);
    int hold = MeasureSpec.getSize(heightMeasureSpec);

    // Draw mode
    if (wmode == MeasureSpec.EXACTLY || wmode == MeasureSpec.AT_MOST && wold > 0) {
      // Width limited
      calculateText(wold);
    } else {
      // Width unlimited
      calculateText(-1.0f);
    }

    // New height
    int hnew = (int) Math.round(Math.ceil(lineSize * (float) lineNormalVector.size()));
    int wnew = wold;
    if (wmode != MeasureSpec.EXACTLY && lineNormalVector.size() <= 1) {
      wnew = (int) Math.round(Math.ceil(lineMax));
    }
    if (hmode != MeasureSpec.UNSPECIFIED && hnew > hold) {
      hnew |= MEASURED_STATE_TOO_SMALL;
    }

    // Set result
    setMeasuredDimension(wnew, hnew);

  }

  @Override
  public void onDraw(Canvas canvas) {
        /*
        // Debug background
        Paint paint = new Paint();
        paint.setARGB(0x30, 0, 0, 0xff);
        Rect rect = new Rect();
        canvas.getClipBounds(rect);
        canvas.drawRect(rect, paint);
        */

    // Check
    assert (lineNormalVector.size() == lineFuriganaVector.size());

    // Coordinates
    float y = lineSize;

    // Loop lines
    for (int i = 0; i < lineNormalVector.size(); i++) {
      lineNormalVector.get(i).draw(canvas, y);
      lineFuriganaVector.get(i).draw(canvas, y - normalHeight);
      y += lineSize;
    }

  }

  // Public Functions
  public void setText(String text) {
    this.calculateAndSetText(text, 0, 0);
  }

  public void setText(String text, int startHighlight, int endHighlight) {
    this.calculateAndSetText(text, startHighlight, endHighlight);
  }

  // private classes
  class TextFurigana {

    // Info
    private String mainText;

    // Coordinates
    float offset;
    float width;

    // Constructor
    TextFurigana(String mainText) {
      // Info
      this.mainText = mainText;

      // Coordinates
      width = furiganaPaint.measureText(mainText);
    }

    // Info
    //private String text() { return mainText; }

    // Coordinates
    float getOffset() {
      return offset;
    }

    void setOffset(float value) {
      offset = value;
    }

    float width() {
      return width;
    }

    // Draw
    void draw(Canvas canvas, float x, float y) {
      x -= width / 2.0f;
      if (x < 0) {
        x = 0;
      } else if (x + width > canvas.getWidth()) {
        x = canvas.getWidth() - width;
      }
      canvas.drawText(mainText, 0, mainText.length(), x, y, furiganaPaint);
    }
  }

  class TextNormal {

    // Info
    private String mainText;
    private boolean isMarked;

    // Widths
    private float widthTotal;
    private float[] widthChars;

    // Constructor
    TextNormal(String text, boolean isMarked) {
      // Info
      mainText = text;
      isMarked = isMarked;

      // Character widths
      widthChars = new float[mainText.length()];
      if (isMarked) {
        highlightTextPaint.getTextWidths(mainText, widthChars);
      } else {
        normalTextPaint.getTextWidths(mainText, widthChars);
      }

      // Total width
      widthTotal = 0.0f;
      for (float v : widthChars) {
        widthTotal += v;
      }
    }

    // Info
    int length() {
      return mainText.length();
    }

    // Widths
    float[] getWidthChars() {
      return widthChars;
    }

    // Split
    TextNormal[] split(int offset) {
      return new TextNormal[]{
          new TextNormal(mainText.substring(0, offset), isMarked),
          new TextNormal(mainText.substring(offset), isMarked)
      };
    }

    // Draw
    float draw(Canvas canvas, float x, float y) {
      if (isMarked) {
        canvas.drawText(mainText, 0, mainText.length(), x, y, highlightTextPaint);
      } else {
        canvas.drawText(mainText, 0, mainText.length(), x, y, normalTextPaint);
      }
      return widthTotal;
    }
  }

  class LineFurigana {

    // Text
    private Vector<TextFurigana> mainText = new Vector<>();
    private Vector<Float> offset = new Vector<>();

    // Add
    void add(TextFurigana text) {
      if (text != null) {
        mainText.add(text);
      }
    }

    // Calculate
    void calculate() {
      // Check size
      if (mainText.size() == 0) {
        return;
      }

            /*
            // Debug
            String str = "";
            for (TextFurigana text : mainText)
                str += "'" + text.text() + "' ";
            */

      // r[] - ideal offsets
      float[] r = new float[mainText.size()];
      for (int i = 0; i < mainText.size(); i++) {
        r[i] = mainText.get(i).getOffset();
      }

      // a[] - constraint matrix
      float[][] a = new float[mainText.size() + 1][mainText.size()];
      for (int i = 0; i < a.length; i++) {
        for (int j = 0; j < a[0].length; j++) {
          a[i][j] = 0.0f;
        }
      }
      a[0][0] = 1.0f;
      for (int i = 1; i < a.length - 2; i++) {
        a[i][i - 1] = -1.0f;
        a[i][i] = 1.0f;
      }
      a[a.length - 1][a[0].length - 1] = -1.0f;

      // b[] - constraint vector
      float[] b = new float[mainText.size() + 1];
      b[0] = -r[0] + (0.5f * mainText.get(0).width());
      for (int i = 1; i < b.length - 2; i++) {
        b[i] = (0.5f * (mainText.get(i).width() + mainText.get(i - 1).width())) + (r[i - 1] - r[i]);
      }
      b[b.length - 1] =
          -lineMax + r[r.length - 1] + (0.5f * mainText.get(mainText.size() - 1).width());

      // Calculate constraint optimization
      float[] x = new float[mainText.size()];
      for (int i = 0; i < x.length; i++) {
        x[i] = 0.0f;
      }
      QuadraticOptimizer co = new QuadraticOptimizer(a, b);
      co.calculate(x);
      for (int i = 0; i < x.length; i++) {
        offset.add(x[i] + r[i]);
      }
    }

    // Draw
    void draw(Canvas canvas, float y) {
      y -= furiganaPaint.descent();
      if (offset.size() == mainText.size()) {
        // Render with fixed offsets
        for (int i = 0; i < offset.size(); i++) {
          mainText.get(i).draw(canvas, offset.get(i), y);
        }
      } else {
        // Render with original offsets
        for (TextFurigana text : mainText) {
          text.draw(canvas, text.getOffset(), y);
        }
      }
    }
  }

  class LineNormal {

    // Text
    private Vector<TextNormal> mainText = new Vector<>();

    // Elements
    int size() {
      return mainText.size();
    }

    void add(Vector<TextNormal> text) {
      mainText.addAll(text);
    }

    // Draw
    void draw(Canvas canvas, float y) {
      y -= normalTextPaint.descent();
      float x = 0.0f;
      for (TextNormal text : mainText) {
        x += text.draw(canvas, x, y);
      }
    }
  }

  class Span {

    // Text
    private TextFurigana furigana = null;
    private Vector<TextNormal> normal = new Vector<>();

    // Widths
    private Vector<Float> widthChars = new Vector<>();
    private float widthTotal = 0.0f;

    // Constructors
    Span(String furiganaText, String kanjiText, int startHightlight, int endHightlight) {
      // Furigana text
      if (furiganaText.length() > 0) {
        furigana = new TextFurigana(furiganaText);
      }

      // Normal text
      if (startHightlight < kanjiText.length() && endHightlight > 0 && startHightlight < endHightlight) {

        // Fix marked bounds
        startHightlight = Math.max(0, startHightlight);
        endHightlight = Math.min(kanjiText.length(), endHightlight);

        // Prefix
        if (startHightlight > 0) {
          normal.add(new TextNormal(kanjiText.substring(0, startHightlight), false));
        }

        // Marked
        if (endHightlight > startHightlight) {
          normal.add(new TextNormal(kanjiText.substring(startHightlight, endHightlight), true));
        }

        // Postfix
        if (endHightlight < kanjiText.length()) {
          normal.add(new TextNormal(kanjiText.substring(endHightlight), false));
        }

      } else {

        // Non marked
        normal.add(new TextNormal(kanjiText, false));

      }

      // Widths
      calculateWidths();
    }

    Span(Vector<TextNormal> normal) {
      // Only normal text
      normal = normal;

      // Widths
      calculateWidths();
    }

    // Text
    TextFurigana furigana(float x) {
      if (furigana == null) {
        return null;
      }
      furigana.setOffset(x + (widthTotal / 2.0f));
      return furigana;
    }

    Vector<TextNormal> normal() {
      return normal;
    }

    // Widths
    Vector<Float> widths() {
      return widthChars;
    }

    private void calculateWidths() {
      // Chars
      if (furigana == null) {
        for (TextNormal normal : normal) {
          for (float v : normal.getWidthChars()) {
            widthChars.add(v);
          }
        }
      } else {
        float sum = 0.0f;
        for (TextNormal normal : normal) {
          for (float v : normal.getWidthChars()) {
            sum += v;
          }
        }
        widthChars.add(sum);
      }

      // Total
      widthTotal = 0.0f;
      for (float v : widthChars) {
        widthTotal += v;
      }
    }

    // Split
    void split(int offset, Vector<TextNormal> normalA, Vector<TextNormal> normalB) {
      // Check if no furigana
      assert (furigana == null);

      // Split normal list
      for (TextNormal cur : normal) {
        if (offset <= 0) {
          normalB.add(cur);
        } else if (offset >= cur.length()) {
          normalA.add(cur);
        } else {
          TextNormal[] split = cur.split(offset);
          normalA.add(split[0]);
          normalB.add(split[1]);
        }
        offset -= cur.length();
      }
    }
  }
}

