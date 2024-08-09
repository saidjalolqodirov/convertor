package uz.convertor.convert;

import com.aspose.cad.Image;
import com.aspose.cad.ImageOptionsBase;
import com.aspose.cad.imageoptions.CadRasterizationOptions;
import com.aspose.cad.imageoptions.PngOptions;
import com.aspose.cad.imageoptions.TiffOptions;
import com.aspose.pdf.devices.JpegDevice;
import com.aspose.pdf.devices.PngDevice;
import com.aspose.words.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import uz.convertor.files.FileDto;
import uz.convertor.files.FileType;
import uz.convertor.files.FileUtils;
import uz.convertor.files.FilesService;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@RequiredArgsConstructor
public class ConvertorService {
    private final FilesService filesService;
    private String session = "";

    public ResponseEntity<?> convert(String sessionId, MultipartFile multipartFile, FileType fromType, FileType toType) {
        if (!check(multipartFile, fromType)) {
            return new ResponseEntity<>("File Type error", HttpStatus.BAD_REQUEST);
        }
        this.session = sessionId;

        if (toType == FileType.PDF) {
            return convertToPdf(multipartFile, fromType);
        } else if (toType == FileType.PNG) {
            return convertToPng(multipartFile, fromType);
        } else if (toType == FileType.JPEG || toType == FileType.JPG) {
            return convertToJpeg(multipartFile, fromType);
        } else if (toType == FileType.DOC || toType == FileType.DOCX) {
            return convertToDocOrDocx(multipartFile, fromType, toType);
        } else if (toType == FileType.DWG || toType == FileType.DXF) {
            return convertToDwgOrDxf(multipartFile, fromType, toType);
        }

        return ResponseEntity.ok(new FileDto());
    }

    private ResponseEntity<?> convertToPdf(MultipartFile multipartFile, FileType fromType) {
        switch (fromType) {
            case JPEG:
            case JPG:
            case PNG:
                return ResponseEntity.ok(convertToPdpFromImage(multipartFile, fromType));
            case DOC:
            case DOCX:
                return ResponseEntity.ok(convertToPdfFromDocAndDocx(multipartFile, fromType));
            case DWG:
            case DXF:
                return ResponseEntity.ok(convertToPdfFromDwgAndDxf(multipartFile, fromType));
            default:
                return ResponseEntity.ok(new FileDto());
        }
    }

    private ResponseEntity<?> convertToPng(MultipartFile multipartFile, FileType fromType) {
        switch (fromType) {
            case JPEG:
            case JPG:
                return ResponseEntity.ok(convertToPngFromJpeg(multipartFile, fromType));
            case PDF:
                return ResponseEntity.ok(convertToPngFromPdf(multipartFile, fromType));
            case DWG:
            case DXF:
                return ResponseEntity.ok(convertToPngFromDwgDxf(multipartFile, fromType));
            case DOCX:
            case DOC:
                return ResponseEntity.ok(convertToPngFromDocDocx(multipartFile, fromType));
            default:
                return ResponseEntity.ok(new FileDto());
        }
    }

    private ResponseEntity<?> convertToJpeg(MultipartFile multipartFile, FileType fromType) {
        switch (fromType) {
            case DWG:
            case DXF:
                return ResponseEntity.ok(convertToJpegFromDwgDxf(multipartFile, fromType));
            case DOC:
            case DOCX:
                return ResponseEntity.ok(convertToJpegFromDocDocx(multipartFile, fromType));
            case PDF:
                return ResponseEntity.ok(convertToJpegFromPdf(multipartFile, fromType));
            case PNG:
                return ResponseEntity.ok(convertToJpegFromPng(multipartFile, fromType));
            default:
                return ResponseEntity.ok(new FileDto());
        }
    }

    private ResponseEntity<?> convertToDocOrDocx(MultipartFile multipartFile, FileType fromType, FileType toType) {
        switch (fromType) {
            case PDF:
                return ResponseEntity.ok(convertToDocFromPdf(multipartFile, toType));
            case PNG:
            case JPEG:
                return ResponseEntity.ok(convertToDocFromPngJpeg(multipartFile, fromType, toType));
            case DOC:
            case DOCX:
                return ResponseEntity.ok(convertToDocFromDoc(multipartFile, fromType, toType));
            default:
                return ResponseEntity.ok(new FileDto());
        }
    }

    private ResponseEntity<?> convertToDwgOrDxf(MultipartFile multipartFile, FileType fromType, FileType toType) {
        if (fromType == FileType.DXF && toType == FileType.DWG) {
            return ResponseEntity.ok(convertToDwgFromDxf(multipartFile));
        } else if (fromType == FileType.DWG && toType == FileType.DXF) {
            return ResponseEntity.ok(convertToDxfFromDwg(multipartFile));
        }
        return ResponseEntity.ok(new FileDto());
    }

    private boolean check(MultipartFile multipartFile, FileType fromType) {
        String extension = FileUtils.getExtensionFromFileName(multipartFile.getOriginalFilename());
        return extension.toUpperCase().equals(fromType.name());
    }


    //   ====================================   convert to PDF  =================================
    private FileDto convertToPdfFromDwgAndDxf(MultipartFile multipartFile, FileType fromType) {
        try {
            Image image = Image.load(multipartFile.getInputStream());

            CadRasterizationOptions rasterizationOptions = new CadRasterizationOptions();
            rasterizationOptions.setPageWidth(1600);
            rasterizationOptions.setPageHeight(1600);

            TiffOptions options = new TiffOptions();

            options.setVectorRasterizationOptions(rasterizationOptions);

            FileDto fileDto = uploadFile(".pdf", fromType, multipartFile.getOriginalFilename());
            image.save(fileDto.getFile_name(), options);
            image.close();

            filesService.addFileDto(fileDto);
            return fileDto;
        } catch (Exception e) {
            System.out.println("e.getMessage() = " + e.getMessage());
        }
        return null;
    }

    private FileDto convertToPdfFromDocAndDocx(MultipartFile multipartFile, FileType fromType) {
        try {
            Document doc = new Document(multipartFile.getInputStream());

            FileDto fileDto = uploadFile(".pdf", fromType, multipartFile.getOriginalFilename());
            doc.save(fileDto.getFile_name());
            filesService.addFileDto(fileDto);
            return fileDto;
        } catch (Exception e) {
            System.out.println("e.getMessage() = " + e.getMessage());
        }
        return null;
    }

    private FileDto convertToPdpFromImage(MultipartFile multipartFile, FileType fromType) {
        try {
            Document doc = new Document();
            DocumentBuilder builder = new DocumentBuilder(doc);

            builder.insertImage(multipartFile.getInputStream());

            FileDto fileDto = uploadFile(".pdf", fromType, multipartFile.getOriginalFilename());

            doc.save(fileDto.getFile_name());
            filesService.addFileDto(fileDto);
            return fileDto;
        } catch (Exception e) {
            System.out.println("e.getMessage() = " + e.getMessage());
        }
        return null;
    }


    //   ====================================   convert to PNG  =================================
    private FileDto convertToPngFromJpeg(MultipartFile multipartFile, FileType fromType) {
        try {
            Document doc = new Document();
            DocumentBuilder builder = new DocumentBuilder(doc);
            Shape shape = builder.insertImage(multipartFile.getInputStream());

            FileDto fileDto = uploadFile(".png", fromType, multipartFile.getOriginalFilename());
            shape.getImageData().save(fileDto.getFile_name());

            doc.save(fileDto.getFile_name());
            filesService.addFileDto(fileDto);
            return fileDto;
        } catch (Exception e) {
            System.out.println("e.getMessage() = " + e.getMessage());
        }
        return null;
    }

    private FileDto convertToPngFromDwgDxf(MultipartFile multipartFile, FileType fromType) {
        try {
            Image image = Image.load(multipartFile.getInputStream());
            CadRasterizationOptions rasterizationOptions = new CadRasterizationOptions();
            rasterizationOptions.setPageWidth(1200);
            rasterizationOptions.setPageHeight(1200);
            rasterizationOptions.setEmbedBackground(true);
            ImageOptionsBase options = new PngOptions();
            options.setVectorRasterizationOptions(rasterizationOptions);

            FileDto fileDto = uploadFile(".png", fromType, multipartFile.getOriginalFilename());
            image.save(fileDto.getFile_name(), options);
            image.close();
            filesService.addFileDto(fileDto);
            return fileDto;
        } catch (Exception e) {
            System.out.println("e.getMessage() = " + e.getMessage());
        }
        return null;
    }

    private FileDto convertToPngFromPdf(MultipartFile multipartFile, FileType fromType) {
        try {
            com.aspose.pdf.Document document = new com.aspose.pdf.Document(multipartFile.getInputStream());
            PngDevice renderer = new PngDevice();
            if (document.getPages().size() == 1) {
                FileDto fileDto = uploadFile(".png", fromType, multipartFile.getOriginalFilename());
                renderer.process(document.getPages().get_Item(1), fileDto.getFile_name());
                filesService.addFileDto(fileDto);
                return fileDto;
            } else {
                FileDto fileDto = uploadFile(".zip", fromType, multipartFile.getOriginalFilename());
                FileOutputStream fos = new FileOutputStream(fileDto.getFile_name());
                ZipOutputStream zos = new ZipOutputStream(fos);
                for (int i = 1; i <= document.getPages().size(); i++) {
                    String filePath = filesService.getPathForUpload() + "/" + UUID.randomUUID() + ".png";
                    renderer.process(document.getPages().get_Item(i), filePath);
                    addToZipFile(filePath, zos);
                    new File(filePath).delete();
                }
                zos.close();
                fos.close();
                filesService.addFileDto(fileDto);
                return fileDto;
            }

        } catch (Exception e) {
            System.out.println("e.getMessage() = " + e.getMessage());
        }
        return null;
    }

    private FileDto convertToPngFromDocDocx(MultipartFile multipartFile, FileType fromType) {
        try {
            Document doc = new Document(multipartFile.getInputStream());

            if (doc.getPageCount() == 1) {
                FileDto fileDto = uploadFile(".png", fromType, multipartFile.getOriginalFilename());
                for (int page = 0; page < doc.getPageCount(); page++) {
                    Document extractedPage = doc.extractPages(page, 1);
                    extractedPage.save(String.format(fileDto.getFile_name(), page + 1));
                }
                filesService.addFileDto(fileDto);
                return fileDto;
            }

            ImageSaveOptions options = new ImageSaveOptions(SaveFormat.PNG);
            FileDto fileDto = uploadFile(".zip", fromType, multipartFile.getOriginalFilename());

            FileOutputStream fos = new FileOutputStream(fileDto.getFile_name());
            ZipOutputStream zos = new ZipOutputStream(fos);

            for (int pageNumber = 0; pageNumber < doc.getPageCount(); pageNumber++) {
                options.setPageSet(new PageSet(pageNumber)); // Adjust page number (1-based index)
                String filePath = filesService.getPathForUpload() + "/" + UUID.randomUUID() + ".png";
                doc.save(filePath, options);
                addToZipFile(filePath, zos);
                new File(filePath).delete(); // Delete temporary PNG files after adding to ZIP
            }

            zos.close();
            fos.close();
            filesService.addFileDto(fileDto);
            return fileDto;
        } catch (Exception e) {
            System.out.println("e.getMessage() = " + e.getMessage());
        }
        return null;
    }

    //   ====================================   convert to JPEG  =================================
    private FileDto convertToJpegFromDwgDxf(MultipartFile multipartFile, FileType fromType) {
        try {
            Image image = Image.load(multipartFile.getInputStream());
            CadRasterizationOptions rasterizationOptions = new CadRasterizationOptions();
            rasterizationOptions.setPageWidth(1600);
            rasterizationOptions.setPageHeight(1600);

            TiffOptions options = new TiffOptions();
            options.setVectorRasterizationOptions(rasterizationOptions);

            FileDto fileDto = uploadFile(".jpeg", fromType, multipartFile.getOriginalFilename());
            image.save(fileDto.getFile_name(), options);
            image.close();
            filesService.addFileDto(fileDto);
            return fileDto;
        } catch (Exception e) {
            System.out.println("e.getMessage() = " + e.getMessage());
        }
        return null;
    }

    private FileDto convertToJpegFromDocDocx(MultipartFile multipartFile, FileType fromType) {
        try {
            Document doc = new Document(multipartFile.getInputStream());

            if (doc.getPageCount() == 1) {
                FileDto fileDto = uploadFile(".jpeg", fromType, multipartFile.getOriginalFilename());
                for (int page = 0; page < doc.getPageCount(); page++) {
                    Document extractedPage = doc.extractPages(page, 1);
                    extractedPage.save(String.format(fileDto.getFile_name(), page + 1));
                }
                filesService.addFileDto(fileDto);
                return fileDto;
            }

            FileDto fileDto = uploadFile(".zip", fromType, multipartFile.getOriginalFilename());

            FileOutputStream fos = new FileOutputStream(fileDto.getFile_name());
            ZipOutputStream zos = new ZipOutputStream(fos);

            for (int page = 0; page < doc.getPageCount(); page++) {
                Document extractedPage = doc.extractPages(page, 1);
                String filePath = filesService.getPathForUpload() + "/" + UUID.randomUUID() + ".jpeg";
                extractedPage.save(String.format(filePath, page + 1));
                addToZipFile(filePath, zos);
                new File(filePath).delete();
            }
            zos.close();
            fos.close();
            filesService.addFileDto(fileDto);
            return fileDto;
        } catch (Exception e) {
            System.out.println("e.getMessage() = " + e.getMessage());
        }
        return null;
    }

    private FileDto convertToJpegFromPdf(MultipartFile multipartFile, FileType fromType) {
        try {
            com.aspose.pdf.Document document = new com.aspose.pdf.Document(multipartFile.getInputStream());
            JpegDevice renderer = new JpegDevice();
            if (document.getPages().size() == 1) {
                FileDto fileDto = uploadFile(".jpeg", fromType, multipartFile.getOriginalFilename());
                renderer.process(document.getPages().get_Item(1), fileDto.getFile_name());
                filesService.addFileDto(fileDto);
                return fileDto;
            } else {
                FileDto fileDto = uploadFile(".zip", fromType, multipartFile.getOriginalFilename());

                FileOutputStream fos = new FileOutputStream(fileDto.getFile_name());
                ZipOutputStream zos = new ZipOutputStream(fos);
                for (int i = 1; i <= document.getPages().size(); i++) {
                    String filePath = filesService.getPathForUpload() + "/" + UUID.randomUUID() + ".jpeg";
                    renderer.process(document.getPages().get_Item(i), filePath);
                    addToZipFile(filePath, zos);
                    new File(filePath).delete();
                }
                zos.close();
                fos.close();
                filesService.addFileDto(fileDto);
                return fileDto;
            }
        } catch (Exception e) {
            System.out.println("e.getMessage() = " + e.getMessage());
        }
        return null;
    }

    private FileDto convertToJpegFromPng(MultipartFile multipartFile, FileType fromType) {
        try {
            Document doc = new Document();
            DocumentBuilder builder = new DocumentBuilder(doc);
            Shape shape = builder.insertImage(multipartFile.getInputStream());
            FileDto fileDto = uploadFile(".jpeg", fromType, multipartFile.getOriginalFilename());
            shape.getImageData().save(fileDto.getFile_name());
            filesService.addFileDto(fileDto);
            return fileDto;
        } catch (Exception e) {
            System.out.println("e.getMessage() = " + e.getMessage());
        }
        return null;
    }


    //   ============================== convert to DOC AND DOCX  =================================

    private FileDto convertToDocFromPdf(MultipartFile multipartFile, FileType toType) {
        try {
            com.aspose.pdf.Document pdfDocument = new com.aspose.pdf.Document(multipartFile.getInputStream());
            String extension = toType.equals(FileType.DOC) ? ".doc" : ".docx";
            FileDto fileDto = uploadFile(extension, FileType.PDF, multipartFile.getOriginalFilename());
            String filePath = fileDto.getFile_name();
            System.out.println("Generated File Path: " + filePath);
            com.aspose.pdf.SaveFormat saveFormat = toType.equals(FileType.DOC) ? com.aspose.pdf.SaveFormat.Doc : com.aspose.pdf.SaveFormat.DocX;
            pdfDocument.save(filePath, saveFormat);
            filesService.addFileDto(fileDto);
            return fileDto;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private FileDto convertToDocFromPngJpeg(MultipartFile multipartFile, FileType fromType, FileType toType) {
        try {
            Document doc = new Document();
            DocumentBuilder builder = new DocumentBuilder(doc);
            builder.insertImage(multipartFile.getInputStream());
            FileDto fileDto = uploadFile(toType.equals(FileType.DOC) ? ".doc" : ".docx", fromType, multipartFile.getOriginalFilename());
            doc.save(fileDto.getFile_name());
            filesService.addFileDto(fileDto);
            return fileDto;
        } catch (Exception e) {
            System.out.println("e.getMessage() = " + e.getMessage());
        }
        return null;
    }

    private FileDto convertToDocFromDoc(MultipartFile multipartFile, FileType fromType, FileType toType) {
        try {
            Document doc = new Document(multipartFile.getInputStream());

            FileDto fileDto = uploadFile(toType.equals(FileType.DOC) ? ".doc" : ".docx", fromType, multipartFile.getOriginalFilename());
            doc.save(fileDto.getFile_name());
            filesService.addFileDto(fileDto);
            return fileDto;
        } catch (Exception e) {
            System.out.println("e.getMessage() = " + e.getMessage());
        }
        return null;
    }


    //   ============================== convert to DWG AND DXF  =================================

    private FileDto convertToDxfFromDwg(MultipartFile multipartFile) {
        try {
            Image image = Image.load(multipartFile.getInputStream());
            CadRasterizationOptions rasterizationOptions = new CadRasterizationOptions();
            rasterizationOptions.setPageWidth(1600);
            rasterizationOptions.setPageHeight(1600);
            TiffOptions options = new TiffOptions();
            options.setVectorRasterizationOptions(rasterizationOptions);

            FileDto fileDto = uploadFile(".dxf", FileType.DWG, multipartFile.getOriginalFilename());
            image.save(fileDto.getFile_name(), options);
            filesService.addFileDto(fileDto);
            return fileDto;
        } catch (Exception e) {
            System.out.println("e.getMessage() = " + e.getMessage());
        }
        return null;
    }

    private FileDto convertToDwgFromDxf(MultipartFile multipartFile) {
        try {
            Image image = Image.load(multipartFile.getInputStream());

            CadRasterizationOptions rasterizationOptions = new CadRasterizationOptions();
            rasterizationOptions.setPageWidth(1600);
            rasterizationOptions.setPageHeight(1600);
            TiffOptions options = new TiffOptions();
            options.setVectorRasterizationOptions(rasterizationOptions);

            FileDto fileDto = uploadFile(".dwg", FileType.DXF, multipartFile.getOriginalFilename());
            image.save(fileDto.getFile_name(), options);
            filesService.addFileDto(fileDto);
            return fileDto;
        } catch (Exception e) {
            System.out.println("e.getMessage() = " + e.getMessage());
        }
        return null;
    }

    //   ============================== end convert task =========================================

    public static void addToZipFile(String fileName, ZipOutputStream zos) {
        try {
            File file = new File(fileName);
            FileInputStream fis = new FileInputStream(file);
            ZipEntry zipEntry = new ZipEntry(file.getName());
            zos.putNextEntry(zipEntry);

            byte[] bytes = new byte[1024];
            int length;
            while ((length = fis.read(bytes)) >= 0) {
                zos.write(bytes, 0, length);
            }
            fis.close();
        } catch (Exception e) {
            System.out.println("e.getMessage() = " + e.getMessage());
        }
    }

    private FileDto uploadFile(String extension, FileType fromType, String originalFilename) {
        String id = UUID.randomUUID().toString();
        String filePath = filesService.getPathForUpload() + "/" + id + extension;
        return new FileDto(id, fromType, originalFilename, filePath, filesService.size() + 1, this.session);
    }
}
