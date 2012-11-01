package arida.ufc.br.moapgpstracker;

import com.mendhak.gpslogger.R;

import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;

public class MoapGpsTrackerMainActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.optionsmenu, menu);
        return true;
    }
}
