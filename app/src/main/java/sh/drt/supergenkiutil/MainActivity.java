package sh.drt.supergenkiutil;

import android.graphics.Color;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import sh.drt.supergenkiutil.furiganaview.FuriganaView;

public class MainActivity extends AppCompatActivity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    FuriganaView furiganaView = (FuriganaView) findViewById(R.id.furiganaView);
    furiganaView.setBaseTextSize(103);
    furiganaView.setBaseColor(Color.BLACK);
    // furiganaView.setText("{宇宙飛行士;うちゅうひこうし}はロケットで{宇;う}{宙;ちゅう}に{行;い}った");

    String text = "{彼女;かのじょ}は{寒気;さむけ}を{防;ふせ}ぐために{厚;あつ}いコートを{着;き}ていた。";
    furiganaView.setText(text);
  }
}
