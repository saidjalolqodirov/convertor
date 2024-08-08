package uz.convertor.files;


public class FileUtils {
    public static String getExtensionFromFileName(String fileName) {
        String extension = fileName;
        int i = extension.lastIndexOf('.');
        if (i >= 0) {
            extension = extension.substring(i + 1);
        } else {
            extension = "";
        }
        return extension;
    }
}
