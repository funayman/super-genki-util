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
  private float lineSize = 0.0f;
  private float normalHeight = 0.0f;
  private float furiganaHeight = 0.0f;
  private float lineMax = 0.0f;

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
    this.normalTextPaint.setColor(baseColor);
  }

  public int getHighlightColor() {
    return highlightColor;
  }

  public void setHighlightColor(int highlightColor) {
    this.highlightColor = highlightColor;
    this.highlightTextPaint.setColor(highlightColor);
  }

  public int getFuriganaColor() {
    return furiganaColor;
  }

  public void setFuriganaColor(int furiganaColor) {
    this.furiganaColor = furiganaColor;
    this.furiganaPaint.setColor(furiganaColor);
  }

  public float getBaseTextSize() {
    return baseTextSize;
  }

  public void setBaseTextSize(float baseTextSize) {
    this.baseTextSize = baseTextSize;
    this.normalTextPaint.setTextSize(baseTextSize);
    this.highlightTextPaint.setTextSize(baseTextSize);
    this.furiganaPaint.setTextSize(baseTextSize / 2.0f);
  }

  // Text functions
  private void setText(String text, int startHighlight, int endHighlight, boolean isInternal) {

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
          spanVector.add(new Span("", text.substring(0, idx), startHighlight, endHighlight));

          // Remove text
          text = text.substring(idx);
          startHighlight -= idx;
          endHighlight -= idx;
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
        spanVector.add(
            new Span(((split.length > 1) ? split[1] : ""), split[0], startHighlight, endHighlight));

        // Remove text
        text = text.substring(idx + 1);
        startHighlight -= split[0].length();
        endHighlight -= split[0].length();

      } else {
        // Single span
        spanVector.add(new Span("", text, startHighlight, endHighlight));
        text = "";
      }
    }

    // Invalidate view
    this.invalidate();
    this.requestLayout();
  }

  private void calculateText(float lineMaxValue) {
    // Clear lines
    lineNormalVector.clear();
    lineFuriganaVector.clear();

    // Sizes
    this.lineMax = 0.0f;

    // Check if no limits on width
    if (lineMaxValue < 0.0) {

      // Create single normal and furigana line
      LineNormal lineNormal = new LineNormal();
      LineFurigana lineFurigana = new LineFurigana();

      // Loop spans
      for (Span span : spanVector) {
        // Text
        lineNormal.add(span.normal());
        lineFurigana.add(span.furigana(this.lineMax));

        // Widths update
        for (float width : span.widths()) {
          this.lineMax += width;
        }
      }

      // Commit both lines
      lineNormalVector.add(lineNormal);
      lineFuriganaVector.add(lineFurigana);

    } else {

      // Lines
      float lineX = 0.0f;
      LineNormal lineNormal = new LineNormal();
      LineFurigana lineFurigana = new LineFurigana();

      // Initial span
      int spanI = 0;

      Span span = null;
      if (spanVector.size() != 0) {
        span = spanVector.get(spanI);
      }

      // Iterate
      while (span != null) {
        // Start offset
        float lineS = lineX;

        // Calculate possible line size
        Vector<Float> widths = span.widths();
        int i = 0;
        for (i = 0; i < widths.size(); i++) {
          if (lineX + widths.get(i) <= lineMaxValue) {
            lineX += widths.get(i);
          } else {
            break;
          }
        }

        // Add span to line
        if (i >= 0 && i < widths.size()) {

          // Span does not fit entirely
          if (i > 0) {
            // Split half that fits
            Vector<TextNormal> textNormalVectorOne = new Vector<>();
            Vector<TextNormal> textNormalVectorTwo = new Vector<>();
            span.split(i, textNormalVectorOne, textNormalVectorTwo);
            lineNormal.add(textNormalVectorOne);
            span = new Span(textNormalVectorTwo);
          }

          // Add new line with current spans
          if (lineNormal.size() != 0) {
            // Add
            this.lineMax = (this.lineMax > lineX ? this.lineMax : lineX);
            lineNormalVector.add(lineNormal);
            lineFuriganaVector.add(lineFurigana);

            // Reset
            lineNormal = new LineNormal();
            lineFurigana = new LineFurigana();
            lineX = 0.0f;

            // Next span
            continue;
          }

        } else {

          // Span fits entirely
          lineNormal.add(span.normal());
          lineFurigana.add(span.furigana(lineS));

        }

        // Next span
        span = null;
        spanI++;
        if (spanI < spanVector.size()) {
          span = spanVector.get(spanI);
        }
      }

      // Last span
      if (lineNormal.size() != 0) {
        // Add
        this.lineMax = (this.lineMax > lineX ? this.lineMax : lineX);
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
      wnew = (int) Math.round(Math.ceil(this.lineMax));
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

  // public functions
  public void setText(String text) {
    this.setText(text, 0, 0);
  }

  public void setText(String text, int startHighlight, int endHighlight) {
    this.setText(text, startHighlight, endHighlight, Boolean.FALSE);
  }

  // private classes
  class TextFurigana {

    // Info
    private String text;

    // Coordinates
    float offset;
    float width;

    // Constructor
    TextFurigana(String text) {
      // Info
      this.text = text;

      // Coordinates
      this.width = furiganaPaint.measureText(text);
    }

    // Info
    //private String text() { return this.text; }

    // Coordinates
    float getOffset() {
      return this.offset;
    }

    void setOffset(float value) {
      this.offset = value;
    }

    float width() {
      return this.width;
    }

    // Draw
    void draw(Canvas canvas, float x, float y) {
      x -= this.width / 2.0f;
      if (x < 0) {
        x = 0;
      } else if (x + this.width > canvas.getWidth()) {
        x = canvas.getWidth() - this.width;
      }
      canvas.drawText(this.text, 0, this.text.length(), x, y, furiganaPaint);
    }
  }

  class TextNormal {

    // Info
    private String textData;
    private boolean isMarked;

    // Widths
    private float widthTotal;
    private float[] charsWidth;

    // Constructor
    TextNormal(String text, boolean isMarked) {
      // Info
      textData = text;
      this.isMarked = isMarked;

      // Character widths
      charsWidth = new float[textData.length()];
      if (isMarked) {
        highlightTextPaint.getTextWidths(textData, charsWidth);
      } else {
        normalTextPaint.getTextWidths(textData, charsWidth);
      }

      // Total width
      widthTotal = 0.0f;
      for (float v : charsWidth) {
        widthTotal += v;
      }
    }

    // Info
    int length() {
      return textData.length();
    }

    // Widths
    float[] getCharsWidth() {
      return charsWidth;
    }

    // Split
    TextNormal[] split(int offset) {
      return new TextNormal[]{
          new TextNormal(textData.substring(0, offset), isMarked),
          new TextNormal(textData.substring(offset), isMarked)
      };
    }

    // Draw
    float draw(Canvas canvas, float x, float y) {
      if (isMarked) {
        canvas.drawText(textData, 0, textData.length(), x, y, highlightTextPaint);
      } else {
        canvas.drawText(textData, 0, textData.length(), x, y, normalTextPaint);
      }
      return widthTotal;
    }
  }

  class LineFurigana {

    // Text
    private Vector<TextFurigana> textFuriganaVector = new Vector<>();
    private Vector<Float> floatVectorOffset = new Vector<>();

    // Add
    void add(TextFurigana text) {
      if (text != null) {
        this.textFuriganaVector.add(text);
      }
    }

    // Calculate
    void calculate() {
      // Check size
      if (this.textFuriganaVector.size() == 0) {
        return;
      }

            /*
            // Debug
            String str = "";
            for (TextFurigana text : this.textFuriganaVector)
                str += "'" + text.text() + "' ";
            */

      // r[] - ideal offsets
      float[] r = new float[this.textFuriganaVector.size()];
      for (int i = 0; i < this.textFuriganaVector.size(); i++) {
        r[i] = this.textFuriganaVector.get(i).getOffset();
      }

      // a[] - constraint matrix
      float[][] a = new float[this.textFuriganaVector.size() + 1][this.textFuriganaVector.size()];
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
      float[] b = new float[this.textFuriganaVector.size() + 1];
      b[0] = -r[0] + (0.5f * this.textFuriganaVector.get(0).width());
      for (int i = 1; i < b.length - 2; i++) {
        b[i] = (0.5f * (this.textFuriganaVector.get(i).width() + this.textFuriganaVector.get(i - 1)
            .width())) + (r[i - 1] - r[i]);
      }
      b[b.length - 1] =
          -FuriganaView.this.lineMax + r[r.length - 1] + (0.5f * this.textFuriganaVector
              .get(this.textFuriganaVector.size() - 1).width());

      // Calculate constraint optimization
      float[] x = new float[this.textFuriganaVector.size()];
      for (int i = 0; i < x.length; i++) {
        x[i] = 0.0f;
      }
      QuadraticOptimizer co = new QuadraticOptimizer(a, b);
      co.calculate(x);
      for (int i = 0; i < x.length; i++) {
        this.floatVectorOffset.add(x[i] + r[i]);
      }
    }

    // Draw
    void draw(Canvas canvas, float y) {
      y -= furiganaPaint.descent();
      if (this.floatVectorOffset.size() == this.textFuriganaVector.size()) {
        // Render with fixed offsets
        for (int i = 0; i < this.floatVectorOffset.size(); i++) {
          this.textFuriganaVector.get(i).draw(canvas, this.floatVectorOffset.get(i), y);
        }
      } else {
        // Render with original offsets
        for (TextFurigana text : this.textFuriganaVector) {
          text.draw(canvas, text.getOffset(), y);
        }
      }
    }
  }

  class LineNormal {

    // Text
    private Vector<TextNormal> textNormalVector = new Vector<>();

    // Elements
    int size() {
      return this.textNormalVector.size();
    }

    void add(Vector<TextNormal> text) {
      this.textNormalVector.addAll(text);
    }

    // Draw
    void draw(Canvas canvas, float y) {
      y -= normalTextPaint.descent();
      float x = 0.0f;
      for (TextNormal text : this.textNormalVector) {
        x += text.draw(canvas, x, y);
      }
    }
  }

  class Span {

    // Text
    private TextFurigana textFurigana = null;
    private Vector<TextNormal> textNormalVector = new Vector<>();

    // Widths
    private Vector<Float> floatVectorWidthChars = new Vector<>();
    private float widthTotal = 0.0f;

    // Constructors
    Span(String furiganaTextData, String kanjiTextData, int startHighlight, int endHighlight) {
      // Furigana text
      if (furiganaTextData.length() > 0) {
        textFurigana = new TextFurigana(furiganaTextData);
      }

      // Normal text
      if (startHighlight < kanjiTextData.length() && endHighlight > 0
          && startHighlight < endHighlight) {

        // Fix marked bounds
        startHighlight = Math.max(0, startHighlight);
        endHighlight = Math.min(kanjiTextData.length(), endHighlight);

        // Prefix
        if (startHighlight > 0) {
          textNormalVector.add(new TextNormal(kanjiTextData.substring(0, startHighlight), false));
        }

        // Marked
        if (endHighlight > startHighlight) {
          textNormalVector
              .add(new TextNormal(kanjiTextData.substring(startHighlight, endHighlight), true));
        }

        // Postfix
        if (endHighlight < kanjiTextData.length()) {
          textNormalVector.add(new TextNormal(kanjiTextData.substring(endHighlight), false));
        }

      } else {

        // Non marked
        textNormalVector.add(new TextNormal(kanjiTextData, false));

      }

      // Widths
      calculateWidths();
    }

    Span(Vector<TextNormal> normal) {
      // Only normal text
      textNormalVector = normal;

      // Widths
      calculateWidths();
    }

    // Text
    TextFurigana furigana(float x) {
      if (textFurigana == null) {
        return null;
      }
      textFurigana.setOffset(x + (widthTotal / 2.0f));
      return textFurigana;
    }

    Vector<TextNormal> normal() {
      return textNormalVector;
    }

    // Widths
    Vector<Float> widths() {
      return floatVectorWidthChars;
    }

    private void calculateWidths() {
      // Chars
      if (textFurigana == null) {
        for (TextNormal normal : textNormalVector) {
          for (float v : normal.getCharsWidth()) {
            floatVectorWidthChars.add(v);
          }
        }
      } else {
        float sum = 0.0f;
        for (TextNormal normal : textNormalVector) {
          for (float v : normal.getCharsWidth()) {
            sum += v;
          }
        }
        floatVectorWidthChars.add(sum);
      }

      // Total
      widthTotal = 0.0f;
      for (float v : floatVectorWidthChars) {
        widthTotal += v;
      }
    }

    // Split
    void split(int offset, Vector<TextNormal> textNormalVectorOne,
        Vector<TextNormal> textNormalVectorTwo) {
      // Check if no furigana
      assert (textFurigana == null);

      // Split normal list
      for (TextNormal cur : textNormalVector) {
        if (offset <= 0) {
          textNormalVectorTwo.add(cur);
        } else if (offset >= cur.length()) {
          textNormalVectorOne.add(cur);
        } else {
          TextNormal[] split = cur.split(offset);
          textNormalVectorOne.add(split[0]);
          textNormalVectorTwo.add(split[1]);
        }
        offset -= cur.length();
      }
    }
  }
}

