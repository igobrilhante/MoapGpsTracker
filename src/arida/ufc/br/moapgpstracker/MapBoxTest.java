package arida.ufc.br.moapgpstracker;

import android.os.Bundle;

import java.io.File;

import org.osmdroid.DefaultResourceProxyImpl;
import org.osmdroid.ResourceProxy;
import org.osmdroid.api.IGeoPoint;
import org.osmdroid.tileprovider.MapTileProviderArray;
import org.osmdroid.tileprovider.modules.IArchiveFile;
import org.osmdroid.tileprovider.modules.MBTilesFileArchive;
import org.osmdroid.tileprovider.modules.MapTileFileArchiveProvider;
import org.osmdroid.tileprovider.modules.MapTileModuleProviderBase;
import org.osmdroid.tileprovider.tilesource.XYTileSource;
import org.osmdroid.tileprovider.util.SimpleRegisterReceiver;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import android.app.Activity;
import android.util.Log;
import android.view.Menu;

public class MapBoxTest extends Activity {
  
		private static final String TAG = MapBoxTest.class.getName();

		// Most of this is useless
		private XYTileSource MBTILESRENDER = new XYTileSource(
				"mbtiles", 
				ResourceProxy.string.offline_mode, 
				0, 10,  // zoom min/max <- should be taken from metadata if available 			 
				256, ".png", "http://a.tiles.mapbox.com/v3/igobrilhante.map-8c50ugow");

		private MapView mOsmv;
		private ResourceProxy mResourceProxy;
		private MapTileProviderArray mProvider;

	    @Override
	    public void onCreate(Bundle savedInstanceState) {
	        super.onCreate(savedInstanceState);
	        
	        mapBeginConfig();
	        loadUI();
	        mapEndConfig();      
	        
	    }
	    
	    private void mapBeginConfig(){
	    	Log.d(TAG,"Initial Config");
	    	mResourceProxy = new DefaultResourceProxyImpl(getApplicationContext());
	    	SimpleRegisterReceiver simpleReceiver = new SimpleRegisterReceiver(this);
	    	    	
	    	File f = new File(Environment.getExternalStorageDirectory(), "mapbox.mbtiles");
	    	
	    	IArchiveFile[] files = { MBTilesFileArchive.getDatabaseFileArchive(f) };    	
	    	MapTileModuleProviderBase moduleProvider = new MapTileFileArchiveProvider(simpleReceiver, MBTILESRENDER, files);
	    	    	    	
			mProvider = new MapTileProviderArray(MBTILESRENDER, null, 
					new MapTileModuleProviderBase[]{ moduleProvider }
			);

			this.mOsmv = new MapView(this, 256, mResourceProxy, mProvider);
			
			Log.d(TAG,"created map view");

	    }
	    
	    private void mapEndConfig(){
	    	Log.d(TAG,"End Config");
	    	
	    	this.mOsmv.setBuiltInZoomControls(true);
			this.mOsmv.setMultiTouchControls(true);

			mOsmv.getController().setZoom(1);
			double lon = 0;
			double lat = 0;

			IGeoPoint point = new GeoPoint(lat, lon); // lat lon and not inverse
			mOsmv.getController().setCenter(point);		
	    }
	    
	    private void loadUI() {
	    	Log.d(TAG,"Load UI");
	    	
	    	this.setContentView(R.layout.main);    	
	    	RelativeLayout rl = (RelativeLayout) findViewById(R.id.maplayout);

			this.mOsmv.setLayoutParams(new LinearLayout.LayoutParams(
			          LinearLayout.LayoutParams.FILL_PARENT,
			          LinearLayout.LayoutParams.FILL_PARENT
			));
			rl.addView(this.mOsmv);
		}

}
