package com.kulinr.android.robocam;

import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Size;
import android.os.Build;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
 * Represents the camera's surface and all the initialization involved with it.
 * 
 * This file was adapted from Mixare <http://www.mixare.org/>
 * 
 * @author Daniele Gobbetti <info@mixare.org>
 * @author Justin Wetherell <phishman3579@gmail.com>
 */
public class CameraSurface extends SurfaceView implements
		SurfaceHolder.Callback, Camera.PictureCallback {

	private static final double ASPECT_RATIO = 3.0 / 4.0;
	private static final int PICTURE_SIZE_MAX_WIDTH = 1280;
	private static final int PREVIEW_SIZE_MAX_WIDTH = 640;

	private static SurfaceHolder holder = null;
	private static Camera camera = null;
	private int cameraId;
	private int rotation;
	private CameraListener listener;

	public CameraSurface(Context context, int rotation, CameraListener listener) {
		super(context);
		this.rotation = rotation;
		this.listener = listener;
		try {
			holder = getHolder();
			holder.addCallback(this);
			holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int width = MeasureSpec.getSize(widthMeasureSpec);
		int height = MeasureSpec.getSize(heightMeasureSpec);

		if (width > height * ASPECT_RATIO) {
			width = (int) (height * ASPECT_RATIO + .5);
		} else {
			height = (int) (width / ASPECT_RATIO + .5);
		}

		setMeasuredDimension(width, height);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		try {
			if (camera != null) {
				try {
					camera.stopPreview();
				} catch (Exception ex) {
					ex.printStackTrace();
				}
				try {
					camera.release();
				} catch (Exception ex) {
					ex.printStackTrace();
				}
				camera = null;
			}

			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
				Camera.CameraInfo info = new Camera.CameraInfo();

				for (int i = 0; i < Camera.getNumberOfCameras(); i++) {
					Camera.getCameraInfo(i, info);

					if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
						cameraId = i;
						camera = Camera.open(i);
					}
				}
			} else {
				camera = Camera.open();
			}

			camera.setPreviewDisplay(holder);
		} catch (Exception ex) {
			try {
				if (camera != null) {
					try {
						camera.stopPreview();
					} catch (Exception ex1) {
						ex.printStackTrace();
					}
					try {
						camera.release();
					} catch (Exception ex2) {
						ex.printStackTrace();
					}
					camera = null;
				}
			} catch (Exception ex3) {
				ex.printStackTrace();
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		try {
			if (camera != null) {
				try {
					camera.stopPreview();
				} catch (Exception ex) {
					ex.printStackTrace();
				}
				try {
					camera.release();
				} catch (Exception ex) {
					ex.printStackTrace();
				}
				camera = null;
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
		try {
			Camera.Parameters parameters = camera.getParameters();
			try {
				List<Camera.Size> supportedSizes = null;
				// On older devices (<1.6) the following will fail
				// the camera will work nevertheless
				supportedSizes = CameraCompatibility
						.getSupportedPreviewSizes(parameters);

				
			} catch (Exception ex) {
				parameters.setPreviewSize(480, 320);
			}

			Size bestPreviewSize = determineBestPreviewSize(parameters);
	        Size bestPictureSize = determineBestPictureSize(parameters);

	        parameters.setPreviewSize(bestPreviewSize.width, bestPreviewSize.height);
	        parameters.setPictureSize(bestPictureSize.width, bestPictureSize.height);

			camera.setParameters(parameters);
			determineDisplayOrientation();
			camera.startPreview();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	/**
	 * Determine the current display orientation and rotate the camera preview
	 * accordingly.
	 */
	private void determineDisplayOrientation() {
		CameraInfo cameraInfo = new CameraInfo();
		Camera.getCameraInfo(cameraId, cameraInfo);

		int degrees = 0;

		switch (rotation) {
		case Surface.ROTATION_0:
			degrees = 0;
			break;

		case Surface.ROTATION_90:
			degrees = 90;
			break;

		case Surface.ROTATION_180:
			degrees = 180;
			break;

		case Surface.ROTATION_270:
			degrees = 270;
			break;
		}

		int displayOrientation;

		if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
			displayOrientation = (cameraInfo.orientation + degrees) % 360;
			displayOrientation = (360 - displayOrientation) % 360;
		} else {
			displayOrientation = (cameraInfo.orientation - degrees + 360) % 360;
		}

		camera.setDisplayOrientation(displayOrientation);
	}

	private Size determineBestPreviewSize(Camera.Parameters parameters) {
		List<Size> sizes = parameters.getSupportedPreviewSizes();

		return determineBestSize(sizes, PREVIEW_SIZE_MAX_WIDTH);
	}

	private Size determineBestPictureSize(Camera.Parameters parameters) {
		List<Size> sizes = parameters.getSupportedPictureSizes();

		return determineBestSize(sizes, PICTURE_SIZE_MAX_WIDTH);
	}

	protected Size determineBestSize(List<Size> sizes, int widthThreshold) {
		Size bestSize = null;

		for (Size currentSize : sizes) {
			boolean isDesiredRatio = (currentSize.width / 4) == (currentSize.height / 3);
			boolean isBetterSize = (bestSize == null || currentSize.width > bestSize.width);
			boolean isInBounds = currentSize.width <= PICTURE_SIZE_MAX_WIDTH;

			if (isDesiredRatio && isInBounds && isBetterSize) {
				bestSize = currentSize;
			}
		}

		if (bestSize == null) {

			return sizes.get(0);
		}

		return bestSize;
	}

    /**
     * Take a picture and notify the listener once the picture is taken.
     */
    public void takePicture() {
        camera.takePicture(null, null, this);
    }
    
	@Override
	public void onPictureTaken(byte[] data, Camera camera) {
		BitmapFactory.Options options = new BitmapFactory.Options();

        Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length, options);

        listener.onPictureTaken(bitmap);
	}
}
