package uz.convertor.files;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Objects;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FileDto {
    private String file_id;
    private FileType from_type;
    private String original_file_name;
    private String file_name;
    private Integer order = 1;
    private String remoteAddress;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FileDto fileDto = (FileDto) o;
        return Objects.equals(file_id, fileDto.file_id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(file_id);
    }
}
