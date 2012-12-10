package org.moap.overlays;

import java.util.ArrayList;

import android.graphics.drawable.Drawable;

import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.OverlayItem;

public class CustomOverlay extends ItemizedOverlay<OverlayItem> {

	private ArrayList<OverlayItem> overlay_list;
	
	public CustomOverlay(Drawable defaultMarker) {
		super(defaultMarker);
		this.overlay_list = new ArrayList<OverlayItem>();
		
		// TODO Auto-generated constructor stub
	}
	
	public void addOverlayItem(OverlayItem item){
		
		this.overlay_list.add(item);
		populate();
	}

	@Override
	protected OverlayItem createItem(int i) {
		// TODO Auto-generated method stub
		return this.overlay_list.get(i);
	}
	
	

	@Override
	public int size() {
		// TODO Auto-generated method stub
		return this.overlay_list.size();
	}

}
