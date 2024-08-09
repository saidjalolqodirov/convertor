package uz.convertor.files;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class FilesService {
    @Value("${files_folder}")
    private String filesFolder;

    List<FileDto> fileDtoList = new ArrayList<>();

    public void addFileDto(FileDto fileDto) {
        this.fileDtoList.add(fileDto);
    }

    public List<FileDto> getList(String sessionId) {
        return fileDtoList.stream()
                .filter(fileDto -> Objects.equals(sessionId, fileDto.getSessionId()))
                .sorted((o1, o2) -> o2.getOrder() - o1.getOrder()).collect(Collectors.toList());
    }

    public Integer size() {
        return fileDtoList.size();
    }

    public String getPathForUpload() {
        java.io.File root = new java.io.File(filesFolder);
        if (!root.exists() || !root.isDirectory()) {
            if (root.mkdirs()) {
                System.out.println("file created successfully");
            }
        }
        return filesFolder;
    }

    public ResponseEntity<Resource> getFileAsResourceForDownloading(FileDto file) {
        try {
            Resource fileAsResource = getFileAsResource(file);

            if (fileAsResource == null) return null;

            MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getFile_name() + "\"");


            String extension = FileUtils.getExtensionFromFileName(file.getFile_name());
            switch (extension.toLowerCase()) {
                case "pdf": {
                    headers.add(HttpHeaders.CONTENT_TYPE, "application/pdf");
                    break;
                }
                case "zip": {
                    headers.add(HttpHeaders.CONTENT_TYPE, "application/zip");
                    break;
                }
                case "jpeg": {
                    headers.add(HttpHeaders.CONTENT_TYPE, "image/jpeg");
                    break;
                }
                case "png": {
                    headers.add(HttpHeaders.CONTENT_TYPE, "image/png");
                    break;
                }
                case "doc": {
                    headers.add(HttpHeaders.CONTENT_TYPE, "application/msword");
                    break;
                }
                case "docx": {
                    headers.add(HttpHeaders.CONTENT_TYPE, "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
                    break;
                }
                default: {
                    break;
                }
            }
            return new ResponseEntity<>(fileAsResource, headers, HttpStatus.OK);
        } catch (Exception e) {
            System.out.println("e.getMessage() = " + e.getMessage());
        }
        return null;
    }

    public Resource getFileAsResource(FileDto file) {
        try {
            Path filePath = Paths.get(file.getFile_name());
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() || resource.isReadable()) {
                return resource;
            } else {
                System.out.println("could not read file: " + file.getFile_name());
            }
        } catch (Exception e) {
            System.out.println("e.getMessage() = " + e.getMessage());
        }
        return null;
    }

    public ResponseEntity<?> download(String id) {
        FileDto fileDto = this.fileDtoList.stream().filter(o -> Objects.equals(o.getFile_id(), id)).findFirst().orElse(null);
        if (fileDto == null) return new ResponseEntity<>("", HttpStatus.NOT_FOUND);
        return getFileAsResourceForDownloading(fileDto);
    }

    public void delete(String id) {
        this.fileDtoList.stream().filter(o -> Objects.equals(o.getFile_id(), id)).findFirst().ifPresent(fileDto -> this.fileDtoList.remove(fileDto));
    }

    public void deleteAllFiles() {
        System.out.println("fileDtoList.size() = " + fileDtoList.size());
        for (FileDto fileDto : fileDtoList) {
            deleteFile(fileDto.getFile_name());
        }
        fileDtoList.removeAll(new ArrayList<>(fileDtoList));
    }

    private void deleteFile(String path) {
        File file = new File(path);
        if (file.delete()) {
            System.out.println("Deleted the file: " + file.getName());
        } else {
            System.out.println("Failed to delete the file.");
        }
    }
}
