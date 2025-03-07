package util;

import java.util.Arrays;
import java.util.Optional;

// Order according by libapp.so
public enum SBinFieldType {
	UNK_0X0(0x0),
	INT8(0x1), 
	U_INT8(0x2), 
	INT16(0x3), 
	U_INT16(0x4), 
	INT32(0x5), 
	U_INT32(0x6), 
	INT64(0x7), 
	U_INT64(0x8), 
	BOOLEAN(0x9), 
	FLOAT(0xA),
	DOUBLE(0xB),
	CHAR(0xC),
	CHDR_ID_REF(0xD), // String
	POD(0xE), // ???
	DATA_ID_REF(0xF), // Reference
	SUB_STRUCT(0x10), // InlineStruct
	DATA_ID_MAP(0x11), // Array
	ENUM_ID_INT32(0x12),
	BIT_FIELD(0x13), // ???
	CHDR_SYMBOL_ID_REF(0x14), // Symbol
	BULK_OFFSET_ID(0x16);
	
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
