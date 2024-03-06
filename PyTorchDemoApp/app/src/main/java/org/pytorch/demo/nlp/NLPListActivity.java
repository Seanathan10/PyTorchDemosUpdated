package org.pytorch.demo.nlp;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import org.pytorch.demo.AbstractListActivity;
import org.pytorch.demo.R;

public class NLPListActivity extends AbstractListActivity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      getWindow().setDecorFitsSystemWindows( false );
    }

    findViewById(R.id.nlp_card_lstm_click_area).setOnClickListener(v -> {
      final Intent intent = new Intent(NLPListActivity.this, TextClassificationActivity.class);
      startActivity(intent);
    });
  }

  @Override
  protected int getListContentLayoutRes() {
    return R.layout.nlp_list_content;
  }
}
