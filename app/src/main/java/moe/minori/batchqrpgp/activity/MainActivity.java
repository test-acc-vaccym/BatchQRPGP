package moe.minori.batchqrpgp.activity;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.Toast;

import moe.minori.batchqrpgp.batchqrpgp.R;

public class MainActivity extends Activity
{

	private static final int CAMERA_PERMISSION = 1;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
	}

	public void onClick(View view)
	{
		if (view.getId() == R.id.startDecodingBtn)
		{
			// Check if camera permission is granted

			if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
			{
				ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION);
			}
			else
			{
				Intent intent = new Intent(this, ScanningActivity.class);
				startActivity(intent);
			}
		}
		else if (view.getId() == R.id.createQRBtn)
		{
			//TODO: Implement
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode,
										   String[] permissions,
										   int[] grantResults)
	{
		switch (requestCode)
		{
			case CAMERA_PERMISSION:
				if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
				{
					Toast.makeText(this, R.string.permission_granted, Toast.LENGTH_SHORT).show();
				}
				else
				{
					Toast.makeText(this, R.string.permission_not_granted, Toast.LENGTH_SHORT).show();
				}
		}
	}
}
