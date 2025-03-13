package util;

public enum SBinTextureFormat {
	// Supported:
	RGB, RGBA,
	// Unpackable Only:
	ETC_RGB,
	// Not Supported:
	PVRTC_2BPP_RGB,
    PVRTC_2BPP_RGBA,
    PVRTC_4BPP_RGBA,
    PVRTC_4BPP_RGB,
    DXT1,
    DXT3,
    DXT5,
    ATC_RGB,
    ATC_RGBA_Explicit,
    ATC_RGBA_Interpolated
}
