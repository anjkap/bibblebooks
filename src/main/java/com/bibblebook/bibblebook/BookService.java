package com.bibblebook.bibblebook;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

import static com.bibblebook.bibblebook.Constant.PHOTO_DIRECTORY;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

@Service
@Slf4j
@Transactional(rollbackOn = Exception.class)
@RequiredArgsConstructor
public class BookService {
    private final BookRepo bookRepo;

    public Page<Book> getAllBooks(int page, int size) {
        return bookRepo.findAll(PageRequest.of(page, size, Sort.by("title")));
    }

    public Book getBook(String id) {
        return bookRepo.findById(id).orElseThrow(()->new RuntimeException("Book not found"));
    }

    public Book createBook(Book book) {
        return bookRepo.save(book);
    }

    public void deleteBook(Book book) {
        // Assignment
    }

    public String uploadPhoto(String id, MultipartFile file) {
        Book book = getBook(id);
        String photoUrl = photoFunction.apply(id, file);
        book.setPhotoUrl(photoUrl);
        bookRepo.save(book);
        return photoUrl;
    }

    private final Function<String, String> fileExtension = filename -> Optional.of(filename).filter(name -> name.contains("."))
            .map(name -> "." + name.substring(filename.lastIndexOf(".")+1)).orElse(".png");

    private final BiFunction<String, MultipartFile, String> photoFunction = (id, image) -> {
    String filename = id + fileExtension.apply(image.getOriginalFilename());
        try {
            Path fileStorageLocation = Paths.get(PHOTO_DIRECTORY).toAbsolutePath().normalize();
            if(!Files.exists(fileStorageLocation)) { Files.createDirectories(fileStorageLocation); }
            Files.copy(image.getInputStream(), fileStorageLocation.resolve(id + fileExtension.apply(image.getOriginalFilename())), REPLACE_EXISTING);
            return ServletUriComponentsBuilder
                    .fromCurrentContextPath()
                    .path("/books/image/"+id+fileExtension.apply(image.getOriginalFilename())).toUriString();
        } catch (Exception exception) {
            throw new RuntimeException("Unable to save image");
        }
    };
}
