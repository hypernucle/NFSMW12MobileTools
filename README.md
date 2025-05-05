# NFSMW12MobileTools
Tool to "unpack" (*decrypt*) and "repack" (*encrypt*) the so-called SBin files of .sb & .sba formats, used in mobile release of Need for Speed Most Wanted (2012) title by EA FireMonkeys.
Any compatibility with other games is not guaranteed.

Input file (*.sb or .sba for textures*) support depends on the internal data type. In some cases, the .json output will contain plain HEX parts.

Minimal Java required version is 1.8.0_111.

### How to Use
Very basic usage examples on Windows:

`java -jar NFSMW12MobileTools.jar unpack career.prefabs.sb` - unpack the selected file into readable .json format.

`java -jar NFSMW12MobileTools.jar repack nfsmw_android.sb.json` - repack your .json back into binary .sb (or .sba for textures) file.

### 3rd party Tools & libs
- etc1tool from Android SDK, required to unpack .sba images with ETC1 compression, placed in /tools folder. Copy can be taken from any "Releases" upload.
- DDSUtils: [https://github.com/Dahie/DDS-Utils](https://github.com/Dahie/DDS-Utils)
- GSon: [https://github.com/google/gson](https://github.com/google/gson)
