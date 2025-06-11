package util;

import java.util.Arrays;
import java.util.Optional;

public enum M3GObjectType {
	HEADER_ELEMENT(0x0),
	APPEARANCE(0x3),
	COMPOSITING_MODE(0x6),
	POLYGON_MODE(0x8),
	GROUP(0x9),
	IMAGE2D(0xA),
	MESH_CONFIG(0xE),
	TEXTURE_REF(0x11),
	VERTEX_ARRAY(0x14),
	VERTEX_BUFFER(0x15),
	SUB_MESH(0x64),
	INDEX_BUFFER(0x65);
	
	private int id;

	private M3GObjectType(int id) {
		this.id = id;
	}
	public int getId() {
		return Integer.valueOf(id);
	}
	public static M3GObjectType valueOf(int value) {
		Optional<M3GObjectType> type = 
				Arrays.stream(values()).filter(legNo -> legNo.id == value).findFirst();
		return type.isPresent() ? type.get() : null;
	}
}
