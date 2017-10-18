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

    public String[] getBookTitles() {
        String strBookTitles = "";
        Book[] books = library.getBooks();
        String[] titles = new String[library.getBooks().length];
        for(int i = 0; i < titles.length; i++) {
            titles[i] = books[i].toString();
        }
        return titles;
    }
}
