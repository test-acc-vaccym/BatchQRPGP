package moe.minori.batchqrpgp.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.Result;

import org.sufficientlysecure.keychain.intents.OpenKeychainIntents;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import me.dm7.barcodescanner.zxing.ZXingScannerView;
import moe.minori.batchqrpgp.Constants;
import moe.minori.batchqrpgp.batchqrpgp.R;
import moe.minori.batchqrpgp.container.DataContainer;
import moe.minori.pgpclipper.util.PGPBlockDetector;

/**
 * Created by minori on 17. 2. 4.
 */
public class ScanningActivity extends Activity implements ZXingScannerView.ResultHandler
{
	private ZXingScannerView scannerView;

	private StringBuilder dataBuilder = new StringBuilder();
	private ArrayList<DataContainer> dataContainers = new ArrayList<>();

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		scannerView = new ZXingScannerView(this);

		ArrayList<BarcodeFormat> searchFormats = new ArrayList<>();
		searchFormats.add(BarcodeFormat.QR_CODE);

		scannerView.setFormats(searchFormats);

		setContentView(scannerView);
	}

	@Override
	protected void onResume()
	{
		super.onResume();
		scannerView.setResultHandler(this);
		scannerView.startCamera();
	}

	@Override
	protected void onPause()
	{
		super.onPause();
		scannerView.stopCamera();
	}

	@Override
	public void handleResult(Result result)
	{
		if (PGPBlockDetector.isBlockPresent(result.getText()))
		{
			handleStringResult(result.getText());
			return;
		}
		else
		{
			try
			{
				byte[] data = Base64.decode(result.getText(), Base64.DEFAULT);
				handleBase64Result(data);
				return;
			}
			catch (IllegalArgumentException e)
			{
				Toast.makeText(this, R.string.no_data_to_decode, Toast.LENGTH_LONG).show();
				scannerView.resumeCameraPreview(this);
				return;
			}
		}

	}

	/**
	 * DataContainer decoding, supports advance features but is much inefficient in terms of total data length
	 *
	 * @param data
	 */
	public void handleBase64Result(byte[] data)
	{
		DataContainer o;

		try
		{
			ObjectInputStream objectInputStream = new ObjectInputStream(new ByteArrayInputStream(data));

			o = (DataContainer) objectInputStream.readObject();
			objectInputStream.close();
		}
		catch (ClassNotFoundException | IOException e) // ReadObject failed
		{
			Toast.makeText(this, R.string.no_dataContainer_found, Toast.LENGTH_LONG).show();
			scannerView.resumeCameraPreview(this);
			return;
		}

		if (o != null)
		{
			try
			{
				Log.d("ScanningActivity", o.getProtocolVersion() + "");
			}
			catch (Exception e)
			{
				Toast.makeText(this, R.string.malformed_datacontainer, Toast.LENGTH_LONG).show();
				scannerView.resumeCameraPreview(this);
				return;
			}

			// DataContainer is valid, check serial

			if (dataContainers == null)
			{
				// Oops, somehow dataContainers is not initialized.
				Log.w("ScanningActivity", "Something should not happen has happened. Desperately trying to continue...");
				dataContainers = new ArrayList<>();
			}

			if (dataContainers.size() != 0) // Not empty
			{
				// Compare this container's serial to the list's first container
				if (dataContainers.get(0).getContainerSerial() == o.getContainerSerial())
				{
					// Correct, check for duplicates

					for (DataContainer c : dataContainers)
					{
						if (c.getThisPartNumber() == o.getThisPartNumber()) // Duplicate container
						{
							Log.d("ScanningActivity", "Duplicate container scanned, Scanned container serial: " + o.getContainerSerial() + " / Container number: " + o.getThisPartNumber());

							Toast.makeText(this, R.string.already_scanned_datacontainer, Toast.LENGTH_LONG).show();
							scannerView.resumeCameraPreview(this);
							return;
						}
					}
				}
				else
				{
					// Wrong container
					Toast.makeText(this, R.string.wrong_container_scanned, Toast.LENGTH_LONG).show();
					scannerView.resumeCameraPreview(this);
					return;
				}
			}

			// No duplicates, or empty. add to list

			dataContainers.add(o);

			// Check if all parts are here
			// TODO: Implement checking container resiliency and recovering datas

			if (dataContainers.size() == o.getTotalPartsNumber())
			{
				// All containers are here, now decode
				boolean decodeResult = decodeContainers(dataContainers);

				if (decodeResult)
				{
					// Success, close this activity
					finish();
				}
				else
				{
					// Appears data decoding routine failed, let user to try again
					Toast.makeText(this, R.string.decoding_routine_failed, Toast.LENGTH_LONG).show();

					dataContainers = new ArrayList<>();
					scannerView.resumeCameraPreview(this);
					return;
				}
			}

			// Some more parts to scan, notify user
			Toast.makeText(this, o.getTotalPartsNumber() - dataContainers.size() + getString(R.string.xx_to_go), Toast.LENGTH_SHORT).show();
			scannerView.resumeCameraPreview(this);
			return;
		}
		else
		{
			// o is null, which is unlikely to happen if the laws of physics are here. But just to be safe, handle this problem.

			Toast.makeText(this, R.string.object_data_null, Toast.LENGTH_LONG).show();

			scannerView.resumeCameraPreview(this);
			return;
		}
	}

	/**
	 * This is more efficient form of data passing, but does not support advanced functions such as
	 * scan result reordering, QR code data recovery (WIP), total number of QRs left for scan, etc.
	 *
	 * @param data
	 */
	public void handleStringResult(String data)
	{
		// Try to tidy data input

		data = PGPBlockDetector.pgpInputTidy(data);


	}

	/**
	 * Try to decode datacontainers, and do the work
	 *
	 * @param dataContainers
	 * @return true if decoded, false if failed
	 */
	private boolean decodeContainers(ArrayList<DataContainer> dataContainers)
	{
		//TODO: Implement resilient container group decoding

		if (dataContainers == null)
		{
			return false;
		}

		if (dataContainers.size() == 0)
		{
			return false;
		}

		Comparator<DataContainer> dataContainerComparator = new Comparator<DataContainer>()
		{
			@Override
			public int compare(DataContainer t0, DataContainer t1)
			{
				return (t0.getThisPartNumber() < t1.getThisPartNumber()) ? -1 : (t0.getThisPartNumber() > t1.getThisPartNumber()) ? 1 : 0;
			}
		};

		if (dataContainers.size() != 1)
		{
			Collections.sort(dataContainers, dataContainerComparator);
		}

		int datatype = dataContainers.get(0).getContainerDataType();

		if (datatype == Constants.DATATYPE_STRING)
		{
			StringBuilder stringBuilder = new StringBuilder();

			for (DataContainer container : dataContainers)
			{
				stringBuilder.append((String) container.getData());
			}

			//TODO: Implement other pgp applications
			// Data collected, launch OpenKeychain

			Intent intent = new Intent(OpenKeychainIntents.DECRYPT_DATA);
			intent.putExtra(OpenKeychainIntents.DECRYPT_DATA, stringBuilder.toString());
			startActivity(intent);

			return true;
		}
		else if (datatype == Constants.DATATYPE_BYTEARRAY)
		{
			int length = 0;
			int cursor = 0;
			int copiedOffset = 0;

			for (DataContainer container : dataContainers)
			{
				length += ((byte[]) container.getData()).length;
			}

			byte[] datas = new byte[length];

			for (DataContainer container : dataContainers)
			{
				byte[] containerdata = (byte[]) container.getData();

				for (; length == cursor; cursor++)
				{
					datas[cursor] = containerdata[cursor - copiedOffset];
				}

				copiedOffset += containerdata.length;
			}

			Intent intent = new Intent(OpenKeychainIntents.DECRYPT_DATA);
			intent.putExtra(OpenKeychainIntents.DECRYPT_DATA, datas);
			startActivity(intent);

			return true;
		}
		else
		{
			// Exception, not implemented datatype
			Log.e("ScanningActivity", "Decoding not implemented: " + datatype);
			return false;
		}


	}
}
