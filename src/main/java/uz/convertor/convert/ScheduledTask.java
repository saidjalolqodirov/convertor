package uz.convertor.convert;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import uz.convertor.files.FilesService;

@Component
public class ScheduledTask {
    private final FilesService filesService;

    public ScheduledTask(FilesService filesService) {
        this.filesService = filesService;
    }

    @Scheduled(fixedRate = 60 * 60 * 1000)
    public void alertMessageWithInvoicesMoreThan3() {
        filesService.deleteAllFiles();
    }
}
