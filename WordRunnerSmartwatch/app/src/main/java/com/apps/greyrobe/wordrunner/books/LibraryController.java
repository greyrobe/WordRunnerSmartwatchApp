package com.apps.greyrobe.wordrunner.books;

import java.util.List;

/**
 * Created by James on 10/14/2017.
 */

public class LibraryController {
    Library library;

    public LibraryController(Library library) {
        this.library = library;
    }

    public String getBookTitles() {
        String strBookTitles = "";
        Book[] books = library.getBooks();
        for(Book book : books) {
            strBookTitles += book.getTitle();
            strBookTitles += ": " + book.getEpubDownloadLink();
            strBookTitles += "\n";
        }
        return strBookTitles;
    }
}
