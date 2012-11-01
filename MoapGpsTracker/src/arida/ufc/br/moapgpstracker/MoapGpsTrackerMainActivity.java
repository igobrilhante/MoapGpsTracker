package arida.ufc.br.moapgpstracker;

import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;

public class MoapGpsTrackerMainActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_moap_gps_tracker_main);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_moap_gps_tracker_main, menu);
        return true;
    }
}
