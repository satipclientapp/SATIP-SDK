package satipsdk.ses.com.satipsdk;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;

public class StartActivity extends AppCompatActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                StartActivity.this.startActivity(new Intent(StartActivity.this, ServerActivity.class));
                StartActivity.this.finish();
                StartActivity.this.overridePendingTransition(0,0);
            }
        }, 1000);
    }
}
