package moe.minori.batchqrpgp.container;

import java.io.Serializable;

/**
 * Created by minori on 17. 2. 4.
 */
public interface DataContainer extends Serializable
{
	int getProtocolVersion();

	int getContainerSerial();

	int getTotalPartsNumber();
	int getThisPartNumber();

	boolean isResilientContainer ();
	boolean isRecoveryPart ();

	int getTotalRecoveryPartNumber();
	int getThisRecoveryPartNumber();

	int getContainerDataType ();
	Object getData ();
}
