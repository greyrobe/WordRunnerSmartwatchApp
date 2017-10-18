package com.apps.greyrobe.wordrunner.books;

import java.io.Serializable;

/**
 * Created by James on 10/5/2017.
 */

public class Book{
    private VolumeInfo volumeInfo;
    private AccessInfo accessInfo;

    public String getTitle() {
        if(volumeInfo != null) {
            return volumeInfo.title;
        }
        return null;
    }

    public String getEpubDownloadLink() {
        if(accessInfo != null && accessInfo.epub != null) {
            return accessInfo.epub.downloadLink;
        }
        return null;
    }

    static class VolumeInfo {
        private String title;
    }

    static class AccessInfo {
        private EPub epub;

        static class EPub {
            private String downloadLink;
        }
    }

    @Override
    public String toString() {
        return getTitle();
    }
}
