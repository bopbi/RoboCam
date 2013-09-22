package com.kulinr.android.robocam;

import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.widget.ImageView;

public class PhotoActivity extends FragmentActivity {
	private Uri uri;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		uri = getIntent().getData();

		setContentView(R.layout.activity_photo);

		ImageView photoView = (ImageView) findViewById(R.id.photo);
		photoView.setImageURI(uri);
	}
}
