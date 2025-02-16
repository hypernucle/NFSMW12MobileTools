package util;

import java.util.Arrays;
import java.util.Optional;

public enum SBinFieldType {
	INT8(0x2), 
	INT32(0x5), 
	FLOAT(0xA), 
	BOOLEAN(0x9), 
	CHDR_ID_REF(0xD), 
	DATA_ID_REF(0xF), 
	DATA_ID_MAP(0x11),
	INT32_0X12(0x12),
	INT32_0X16(0x16);
	
	private int id;

	private SBinFieldType(int id) {
		this.id = id;
	}
	public int getId() {
		return Integer.valueOf(id);
	}
	public static SBinFieldType valueOf(int value) {
		Optional<SBinFieldType> type = 
				Arrays.stream(values()).filter(legNo -> legNo.id == value).findFirst();
		return type.isPresent() ? type.get() : null;
	}
}
