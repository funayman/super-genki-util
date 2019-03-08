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
  private float m_linesize = 0.0f;
  private float m_height_n = 0.0f;
  private float m_height_f = 0.0f;
  private float m_linemax = 0.0f;

  // Spans and lines
  private Vector<Span> m_span = new Vector<>();
  private Vector<LineNormal> m_line_n = new Vector<>();
  private Vector<LineFurigana> m_line_f = new Vector<>();

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
  private void text_set(String text, int mark_s, int mark_e) {

    // Linesize
    m_height_n = normalTextPaint.descent() - normalTextPaint.ascent();
    m_height_f = furiganaPaint.descent() - furiganaPaint.ascent();
    m_linesize = m_height_n + m_height_f;

    // Clear spans
    m_span.clear();

    // Sizes
    m_linesize = furiganaPaint.getFontSpacing() + Math
        .max(normalTextPaint.getFontSpacing(), highlightTextPaint.getFontSpacing());

    // Spannify text
    while (text.length() > 0) {
      int idx = text.indexOf('{');
      if (idx >= 0) {
        // Prefix string
        if (idx > 0) {
          // Spans
          m_span.add(new Span("", text.substring(0, idx), mark_s, mark_e));

          // Remove text
          text = text.substring(idx);
          mark_s -= idx;
          mark_e -= idx;
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
        m_span.add(new Span(((split.length > 1) ? split[1] : ""), split[0], mark_s, mark_e));

        // Remove text
        text = text.substring(idx + 1);
        mark_s -= split[0].length();
        mark_e -= split[0].length();

      } else {
        // Single span
        m_span.add(new Span("", text, mark_s, mark_e));
        text = "";
      }
    }

    // Invalidate view
    this.invalidate();
    this.requestLayout();
  }

  private void text_calculate(float line_max) {
    // Clear lines
    m_line_n.clear();
    m_line_f.clear();

    // Sizes
    m_linemax = 0.0f;

    // Check if no limits on width
    if (line_max < 0.0) {

      // Create single normal and furigana line
      LineNormal line_n = new LineNormal();
      LineFurigana line_f = new LineFurigana();

      // Loop spans
      for (Span span : m_span) {
        // Text
        line_n.add(span.normal());
        line_f.add(span.furigana(m_linemax));

        // Widths update
        for (float width : span.widths()) {
          m_linemax += width;
        }
      }

      // Commit both lines
      m_line_n.add(line_n);
      m_line_f.add(line_f);

    } else {

      // Lines
      float line_x = 0.0f;
      LineNormal line_n = new LineNormal();
      LineFurigana line_f = new LineFurigana();

      // Initial span
      int span_i = 0;

      Span span = null;
      if (m_span.size() != 0) {
        span = m_span.get(span_i);
      }

      // Iterate
      while (span != null) {
        // Start offset
        float line_s = line_x;

        // Calculate possible line size
        Vector<Float> widths = span.widths();
        int i = 0;
        for (i = 0; i < widths.size(); i++) {
          if (line_x + widths.get(i) <= line_max) {
            line_x += widths.get(i);
          } else {
            break;
          }
        }

        // Add span to line
        if (i >= 0 && i < widths.size()) {

          // Span does not fit entirely
          if (i > 0) {
            // Split half that fits
            Vector<TextNormal> normal_a = new Vector<>();
            Vector<TextNormal> normal_b = new Vector<>();
            span.split(i, normal_a, normal_b);
            line_n.add(normal_a);
            span = new Span(normal_b);
          }

          // Add new line with current spans
          if (line_n.size() != 0) {
            // Add
            m_linemax = (m_linemax > line_x ? m_linemax : line_x);
            m_line_n.add(line_n);
            m_line_f.add(line_f);

            // Reset
            line_n = new LineNormal();
            line_f = new LineFurigana();
            line_x = 0.0f;

            // Next span
            continue;
          }

        } else {

          // Span fits entirely
          line_n.add(span.normal());
          line_f.add(span.furigana(line_s));

        }

        // Next span
        span = null;
        span_i++;
        if (span_i < m_span.size()) {
          span = m_span.get(span_i);
        }
      }

      // Last span
      if (line_n.size() != 0) {
        // Add
        m_linemax = (m_linemax > line_x ? m_linemax : line_x);
        m_line_n.add(line_n);
        m_line_f.add(line_f);
      }
    }

    // Calculate furigana
    for (LineFurigana line : m_line_f) {
      line.calculate();
    }
  }

  @Override
  protected void onMeasure(int width_ms, int height_ms) {
    // Modes
    int wmode = MeasureSpec.getMode(width_ms);
    int hmode = MeasureSpec.getMode(height_ms);

    // Dimensions
    int wold = MeasureSpec.getSize(width_ms);
    int hold = MeasureSpec.getSize(height_ms);

    // Draw mode
    if (wmode == MeasureSpec.EXACTLY || wmode == MeasureSpec.AT_MOST && wold > 0) {
      // Width limited
      text_calculate(wold);
    } else {
      // Width unlimited
      text_calculate(-1.0f);
    }

    // New height
    int hnew = (int) Math.round(Math.ceil(m_linesize * (float) m_line_n.size()));
    int wnew = wold;
    if (wmode != MeasureSpec.EXACTLY && m_line_n.size() <= 1) {
      wnew = (int) Math.round(Math.ceil(m_linemax));
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
    assert (m_line_n.size() == m_line_f.size());

    // Coordinates
    float y = m_linesize;

    // Loop lines
    for (int i = 0; i < m_line_n.size(); i++) {
      m_line_n.get(i).draw(canvas, y);
      m_line_f.get(i).draw(canvas, y - m_height_n);
      y += m_linesize;
    }

  }

  // Public Functions
  public void setText(String text) {
    this.text_set(text, 0, 0);
  }

  public void setText(String text, int startHighlight, int endHighlight) {
    this.text_set(text, startHighlight, endHighlight);
  }

  // private classes
  class TextFurigana {

    // Info
    private String m_text;

    // Coordinates
    float m_offset;
    float m_width;

    // Constructor
    TextFurigana(String text) {
      // Info
      m_text = text;

      // Coordinates
      m_width = furiganaPaint.measureText(m_text);
    }

    // Info
    //private String text() { return m_text; }

    // Coordinates
    float offset_get() {
      return m_offset;
    }

    void offset_set(float value) {
      m_offset = value;
    }

    float width() {
      return m_width;
    }

    // Draw
    void draw(Canvas canvas, float x, float y) {
      x -= m_width / 2.0f;
      if (x < 0) {
        x = 0;
      } else if (x + m_width > canvas.getWidth()) {
        x = canvas.getWidth() - m_width;
      }
      canvas.drawText(m_text, 0, m_text.length(), x, y, furiganaPaint);
    }
  }

  class TextNormal {

    // Info
    private String m_text;
    private boolean m_is_marked;

    // Widths
    private float m_width_total;
    private float[] m_width_chars;

    // Constructor
    TextNormal(String text, boolean is_marked) {
      // Info
      m_text = text;
      m_is_marked = is_marked;

      // Character widths
      m_width_chars = new float[m_text.length()];
      if (m_is_marked) {
        highlightTextPaint.getTextWidths(m_text, m_width_chars);
      } else {
        normalTextPaint.getTextWidths(m_text, m_width_chars);
      }

      // Total width
      m_width_total = 0.0f;
      for (float v : m_width_chars) {
        m_width_total += v;
      }
    }

    // Info
    int length() {
      return m_text.length();
    }

    // Widths
    float[] width_chars() {
      return m_width_chars;
    }

    // Split
    TextNormal[] split(int offset) {
      return new TextNormal[]{
          new TextNormal(m_text.substring(0, offset), m_is_marked),
          new TextNormal(m_text.substring(offset), m_is_marked)
      };
    }

    // Draw
    float draw(Canvas canvas, float x, float y) {
      if (m_is_marked) {
        canvas.drawText(m_text, 0, m_text.length(), x, y, highlightTextPaint);
      } else {
        canvas.drawText(m_text, 0, m_text.length(), x, y, normalTextPaint);
      }
      return m_width_total;
    }
  }

  class LineFurigana {

    // Text
    private Vector<TextFurigana> m_text = new Vector<>();
    private Vector<Float> m_offset = new Vector<>();

    // Add
    void add(TextFurigana text) {
      if (text != null) {
        m_text.add(text);
      }
    }

    // Calculate
    void calculate() {
      // Check size
      if (m_text.size() == 0) {
        return;
      }

            /*
            // Debug
            String str = "";
            for (TextFurigana text : m_text)
                str += "'" + text.text() + "' ";
            */

      // r[] - ideal offsets
      float[] r = new float[m_text.size()];
      for (int i = 0; i < m_text.size(); i++) {
        r[i] = m_text.get(i).offset_get();
      }

      // a[] - constraint matrix
      float[][] a = new float[m_text.size() + 1][m_text.size()];
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
      float[] b = new float[m_text.size() + 1];
      b[0] = -r[0] + (0.5f * m_text.get(0).width());
      for (int i = 1; i < b.length - 2; i++) {
        b[i] = (0.5f * (m_text.get(i).width() + m_text.get(i - 1).width())) + (r[i - 1] - r[i]);
      }
      b[b.length - 1] =
          -m_linemax + r[r.length - 1] + (0.5f * m_text.get(m_text.size() - 1).width());

      // Calculate constraint optimization
      float[] x = new float[m_text.size()];
      for (int i = 0; i < x.length; i++) {
        x[i] = 0.0f;
      }
      QuadraticOptimizer co = new QuadraticOptimizer(a, b);
      co.calculate(x);
      for (int i = 0; i < x.length; i++) {
        m_offset.add(x[i] + r[i]);
      }
    }

    // Draw
    void draw(Canvas canvas, float y) {
      y -= furiganaPaint.descent();
      if (m_offset.size() == m_text.size()) {
        // Render with fixed offsets
        for (int i = 0; i < m_offset.size(); i++) {
          m_text.get(i).draw(canvas, m_offset.get(i), y);
        }
      } else {
        // Render with original offsets
        for (TextFurigana text : m_text) {
          text.draw(canvas, text.offset_get(), y);
        }
      }
    }
  }

  class LineNormal {

    // Text
    private Vector<TextNormal> m_text = new Vector<>();

    // Elements
    int size() {
      return m_text.size();
    }

    void add(Vector<TextNormal> text) {
      m_text.addAll(text);
    }

    // Draw
    void draw(Canvas canvas, float y) {
      y -= normalTextPaint.descent();
      float x = 0.0f;
      for (TextNormal text : m_text) {
        x += text.draw(canvas, x, y);
      }
    }
  }

  class Span {

    // Text
    private TextFurigana m_furigana = null;
    private Vector<TextNormal> m_normal = new Vector<>();

    // Widths
    private Vector<Float> m_width_chars = new Vector<>();
    private float m_width_total = 0.0f;

    // Constructors
    Span(String text_f, String text_k, int mark_s, int mark_e) {
      // Furigana text
      if (text_f.length() > 0) {
        m_furigana = new TextFurigana(text_f);
      }

      // Normal text
      if (mark_s < text_k.length() && mark_e > 0 && mark_s < mark_e) {

        // Fix marked bounds
        mark_s = Math.max(0, mark_s);
        mark_e = Math.min(text_k.length(), mark_e);

        // Prefix
        if (mark_s > 0) {
          m_normal.add(new TextNormal(text_k.substring(0, mark_s), false));
        }

        // Marked
        if (mark_e > mark_s) {
          m_normal.add(new TextNormal(text_k.substring(mark_s, mark_e), true));
        }

        // Postfix
        if (mark_e < text_k.length()) {
          m_normal.add(new TextNormal(text_k.substring(mark_e), false));
        }

      } else {

        // Non marked
        m_normal.add(new TextNormal(text_k, false));

      }

      // Widths
      widths_calculate();
    }

    Span(Vector<TextNormal> normal) {
      // Only normal text
      m_normal = normal;

      // Widths
      widths_calculate();
    }

    // Text
    TextFurigana furigana(float x) {
      if (m_furigana == null) {
        return null;
      }
      m_furigana.offset_set(x + (m_width_total / 2.0f));
      return m_furigana;
    }

    Vector<TextNormal> normal() {
      return m_normal;
    }

    // Widths
    Vector<Float> widths() {
      return m_width_chars;
    }

    private void widths_calculate() {
      // Chars
      if (m_furigana == null) {
        for (TextNormal normal : m_normal) {
          for (float v : normal.width_chars()) {
            m_width_chars.add(v);
          }
        }
      } else {
        float sum = 0.0f;
        for (TextNormal normal : m_normal) {
          for (float v : normal.width_chars()) {
            sum += v;
          }
        }
        m_width_chars.add(sum);
      }

      // Total
      m_width_total = 0.0f;
      for (float v : m_width_chars) {
        m_width_total += v;
      }
    }

    // Split
    void split(int offset, Vector<TextNormal> normal_a, Vector<TextNormal> normal_b) {
      // Check if no furigana
      assert (m_furigana == null);

      // Split normal list
      for (TextNormal cur : m_normal) {
        if (offset <= 0) {
          normal_b.add(cur);
        } else if (offset >= cur.length()) {
          normal_a.add(cur);
        } else {
          TextNormal[] split = cur.split(offset);
          normal_a.add(split[0]);
          normal_b.add(split[1]);
        }
        offset -= cur.length();
      }
    }
  }
}

