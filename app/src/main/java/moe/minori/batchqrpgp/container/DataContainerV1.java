package moe.minori.batchqrpgp.container;

import moe.minori.batchqrpgp.Constants;

/**
 * Created by minori on 17. 2. 4.
 */
public class DataContainerV1 implements DataContainer
{
	final private int PROTOCOL_VERSION = 1;

	private int totalPartsNumber;
	private int thisPartNumber;

	private int containerSerial;

	private int actualDataType;
	Object actualData;

	public DataContainerV1 (int containerSerial, int totalPartsNumber, int thisPartNumber, Object data)
	{
		this.totalPartsNumber = totalPartsNumber;
		this.thisPartNumber = thisPartNumber;
		this.containerSerial = containerSerial;

		actualData = data;

		if ( data instanceof byte[] )
		{
			actualDataType = Constants.DATATYPE_BYTEARRAY;
		}
		else if ( data instanceof String )
		{
			actualDataType = Constants.DATATYPE_STRING;
		}
		else
		{
			// Warning, generic datatype decoding is not implemented (cannot be implemented)
			actualData = Constants.DATATYPE_GENERIC;
		}
	}

	@Override
	public int getProtocolVersion()
	{
		return PROTOCOL_VERSION;
	}

	@Override
	public int getContainerSerial()
	{
		return containerSerial;
	}

	@Override
	public int getTotalPartsNumber()
	{
		return totalPartsNumber;
	}

	@Override
	public int getThisPartNumber()
	{
		return thisPartNumber;
	}

	@Override
	public boolean isResilientContainer()
	{
		return false;
	}

	@Override
	public boolean isRecoveryPart()
	{
		return false;
	}

	@Override
	public int getTotalRecoveryPartNumber()
	{
		return 0;
	}

	@Override
	public int getThisRecoveryPartNumber()
	{
		return -1;
	}

	@Override
	public int getContainerDataType()
	{
		return actualDataType;
	}

	@Override
	public Object getData()
	{
		return actualData;
	}
}
