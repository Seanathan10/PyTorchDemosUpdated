package org.pytorch.demo;

import android.os.Build;
import android.os.Bundle;
import android.view.ViewStub;

import androidx.annotation.LayoutRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

public abstract class AbstractListActivity extends AppCompatActivity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
      WindowCompat.setDecorFitsSystemWindows( getWindow(), false );
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      getWindow().setDecorFitsSystemWindows( false );
    }

    setContentView(R.layout.activity_list_stub);
    findViewById(R.id.list_back).setOnClickListener(v -> finish());
    final ViewStub listContentStub = findViewById(R.id.list_content_stub);
    listContentStub.setLayoutResource(getListContentLayoutRes());
    listContentStub.inflate();
  }

  protected abstract @LayoutRes
  int getListContentLayoutRes();
}
