package com.kulinr.android.robocam;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Matrix;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.kulinr.android.robocam.CameraListener;
import com.kulinr.android.robocam.CameraSurface;

public class CameraActivity extends FragmentActivity implements CameraListener, OnClickListener {

	public static final String TAG = "RoboCam/CameraActivity";

	private static final int PICTURE_QUALITY = 90;
	
	private ImageButton imageButton;
	private CameraSurface cameraSurface;
	
	@Override
	protected void onCreate(Bundle arg0) {
		super.onCreate(arg0);
		cameraSurface = new CameraSurface(this,
				getWindowManager().getDefaultDisplay().getRotation(), this);
		setContentView(R.layout.activity_camera);
		LinearLayout cameraLinearLayout = (LinearLayout) findViewById(R.id.camera_frame);
		cameraLinearLayout.addView(cameraSurface);
		imageButton = (ImageButton) findViewById(R.id.button_take_preview);
		imageButton.setOnClickListener(this);
	}
  
  @Override 
  protected void onResume() {
      super.onResume();
      imageButton.setEnabled(true);
  }

	@Override
	public void onCameraError() {
		Toast.makeText(this, "Camera Error", Toast.LENGTH_SHORT).show();
	}

	@Override
	public void onPictureTaken(Bitmap bitmap) {
		File mediaStorageDir = new File(
				Environment
						.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
				getString(R.string.app_name));

		if (!mediaStorageDir.exists()) {
			if (!mediaStorageDir.mkdirs()) {
				showSavingPictureErrorToast();
				return;
			}
		}

		String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss")
				.format(new Date());
		String path = mediaStorageDir.getPath() + File.separator + "KULINR_"
				+ timeStamp + ".jpg";
		File mediaFile = new File(path);

		int w = bitmap.getWidth();
		int h = bitmap.getHeight();

		// Setting pre rotate
		Matrix mtx = new Matrix();
		mtx.preRotate(90);

		// Rotating Bitmap & convert to ARGB_8888, required by tess
		bitmap = Bitmap.createBitmap(bitmap, 0, 0, w, h, mtx, false);
		bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);

		try {
			FileOutputStream stream = new FileOutputStream(mediaFile);

			bitmap.compress(CompressFormat.JPEG, PICTURE_QUALITY, stream);
		} catch (IOException exception) {
			showSavingPictureErrorToast();

			Log.w(TAG, "IOException during saving bitmap", exception);
			return;
		}

		MediaScannerConnection.scanFile(this,
				new String[] { mediaFile.toString() },
				new String[] { "image/jpeg" }, null);

		Intent intent = new Intent(this, PhotoActivity.class);
		intent.setData(Uri.fromFile(mediaFile));
		startActivity(intent);
	}
	
	private void showSavingPictureErrorToast() {
		Toast.makeText(this, "Save Error", Toast.LENGTH_SHORT).show();
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.button_take_preview:
			imageButton.setEnabled(false);
			cameraSurface.takePicture();
			break;

		default:
			break;
		}
	}
}
