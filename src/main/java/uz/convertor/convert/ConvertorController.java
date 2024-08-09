package uz.convertor.convert;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import uz.convertor.files.FileDto;
import uz.convertor.files.FileType;
import uz.convertor.files.FilesService;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/convertor")
public class ConvertorController {
    private final ConvertorService convertorService;
    private final FilesService filesService;

    public ConvertorController(ConvertorService convertorService, FilesService filesService) {
        this.convertorService = convertorService;
        this.filesService = filesService;
    }

    @PostMapping("/upload")
    public ResponseEntity<?> upload(@RequestParam(name = "file") MultipartFile multipartFile,
                                    @RequestParam FileType fromType,
                                    @RequestParam FileType toType,
                                    HttpServletRequest request,
                                    HttpServletResponse response) {

        String cookie;
        if (request.getCookies() == null) {
            cookie = setCookie(request, response);
        } else {
            cookie = getSessionId(request);
        }
        return convertorService.convert(cookie, multipartFile, fromType, toType);
    }

    @GetMapping("/download/{id}")
    public ResponseEntity<?> download(@PathVariable String id) {
        return filesService.download(id);
    }

    @GetMapping("/type_list")
    public FileType[] typeList() {
        return FileType.values();
    }

    @GetMapping("/convert_files_list")
    public List<FileDto> convertFilesList(HttpServletRequest request) {
        return filesService.getList(request.getCookies() == null ? "" : getSessionId(request));
    }

    @GetMapping("/access_type_list_by_type")
    public FileType[] typeList(@RequestParam FileType fileType) {
        switch (fileType) {
            case DWG: {
                return new FileType[]{FileType.DXF, FileType.PDF, FileType.PNG, FileType.JPEG, FileType.JPG};
            }
            case DXF: {
                return new FileType[]{FileType.DWG, FileType.PDF, FileType.PNG, FileType.JPEG, FileType.JPG};
            }
            case PDF: {
                return new FileType[]{FileType.PNG, FileType.JPEG, FileType.JPG, FileType.DOC, FileType.DOCX};
            }
            case PNG: {
                return new FileType[]{FileType.PDF, FileType.JPEG, FileType.JPG, FileType.DOC, FileType.DOCX};
            }
            case JPEG:
            case JPG: {
                return new FileType[]{FileType.PDF, FileType.PNG, FileType.DOC, FileType.DOCX};
            }
            case DOC: {
                return new FileType[]{FileType.PDF, FileType.PNG, FileType.JPEG, FileType.JPG, FileType.DOCX};
            }
            case DOCX: {
                return new FileType[]{FileType.PDF, FileType.PNG, FileType.JPEG, FileType.JPG, FileType.DOC};
            }
            default: {
                return new FileType[]{};
            }
        }
    }

    @DeleteMapping("/delete/{id}")
    public void delete(@PathVariable String id) {
        filesService.delete(id);
    }

    public String setCookie(HttpServletRequest request, HttpServletResponse response) {
        if (request.getCookies() == null || request.getCookies().length == 0 || getSessionId(request).isEmpty()) {
            String cookieValue = UUID.randomUUID().toString();
            Cookie cookie = new Cookie("sessionId", cookieValue);
            cookie.setPath("/");
            response.addCookie(cookie);
            return cookieValue;
        }
        return null;
    }

    private String getSessionId(HttpServletRequest request) {
        for (Cookie cookie : request.getCookies()) {
            if (cookie.getName().equals("sessionId")) {
                return cookie.getValue();
            }
        }
        return "";
    }
}
