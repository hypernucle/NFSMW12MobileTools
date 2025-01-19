import java.io.IOException;

public class main {

	private static String about = "NFS Most Wanted (2012, mobile) modding tools by Hypercycle, v0.2";
	
	public static void main(String[] args) throws IOException {
		System.out.println(about);
		SBin sbinTools = new SBin();
		switch(args[0]) {
		case "unpack":
			sbinTools.unpackSBin(args[1], args[2]);
			break;
		case "repack":
			sbinTools.repackSBin(args[1]);
			break;
		case "hash":
			sbinTools.getFNVHash(args[1], args[2]);
			break;
		default: break;
		}
	}
	
}