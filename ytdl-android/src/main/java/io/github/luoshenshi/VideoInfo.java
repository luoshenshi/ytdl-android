package io.github.luoshenshi;

import org.json.JSONObject;

import java.util.List;

public class VideoInfo {
    private final String title;
    private final String author;
    private final String description;
    private final String views;
    private final String likes;
    private final String videoUrl;
    private final List<JSONObject> formats;
    private final List<Thumbnail> thumbnails;
    private final String iframeUrl;
    private final String ownerProfileUrl;
    private final String channelId;
    private final boolean isFamilySafe;
    private final List<String> availableCountries;
    private final String category;
    private final String uploadDate;
    private final boolean isShortsEligible;
    private final JSONObject authorInfo;

    public VideoInfo(String title, String author, String description, String views, String likes, String videoUrl, List<JSONObject> formats, List<Thumbnail> thumbnails, String iframeUrl, String ownerProfileUrl, String channelId, boolean isFamilySafe, List<String> availableCountries, String category, String uploadDate, boolean isShortsEligible, JSONObject authorInfo) {
        this.title = title;
        this.author = author;
        this.description = description;
        this.views = views;
        this.likes = likes;
        this.videoUrl = videoUrl;
        this.formats = formats;
        this.thumbnails = thumbnails;
        this.iframeUrl = iframeUrl;
        this.ownerProfileUrl = ownerProfileUrl;
        this.channelId = channelId;
        this.isFamilySafe = isFamilySafe;
        this.availableCountries = availableCountries;
        this.category = category;
        this.uploadDate = uploadDate;
        this.isShortsEligible = isShortsEligible;
        this.authorInfo = authorInfo;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public String getDescription() {
        return description;
    }

    public String getViews() {
        return views;
    }

    public String getLikes() {
        return likes;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public List<JSONObject> getFormats() {
        return formats;
    }

    public List<Thumbnail> getThumbnails() {
        return thumbnails;
    }

    public String getIframeUrl() {
        return iframeUrl;
    }

    public String getOwnerProfileUrl() {
        return ownerProfileUrl;
    }

    public String getChannelId() {
        return channelId;
    }

    public boolean isFamilySafe() {
        return isFamilySafe;
    }

    public List<String> getAvailableCountries() {
        return availableCountries;
    }

    public String getCategory() {
        return category;
    }

    public String getUploadDate() {
        return uploadDate;
    }

    public boolean isShortsEligible() {
        return isShortsEligible;
    }

    public JSONObject getAuthorInfo() {
        return authorInfo;
    }

    public static class Thumbnail {
        private final String url;
        private final int width;
        private final int height;

        public Thumbnail(String url, int width, int height) {
            this.url = url;
            this.width = width;
            this.height = height;
        }

        public String getUrl() {
            return url;
        }

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }

        @Override
        public String toString() {
            return "{" +
                    "url='" + url + '\'' +
                    ", width=" + width +
                    ", height=" + height +
                    '}';
        }
    }

    public String string() {
        return "{" +
                "title='" + title + '\'' +
                ", author='" + author + '\'' +
                ", description='" + description + '\'' +
                ", views='" + views + '\'' +
                ", likes='" + likes + '\'' +
                ", videoUrl='" + videoUrl + '\'' +
                ", formats=" + formats +
                ", thumbnails=" + thumbnails +
                ", iframeUrl='" + iframeUrl + '\'' +
                ", ownerProfileUrl='" + ownerProfileUrl + '\'' +
                ", channelId='" + channelId + '\'' +
                ", isFamilySafe=" + isFamilySafe +
                ", availableCountries=" + availableCountries +
                ", category='" + category + '\'' +
                ", uploadDate='" + uploadDate + '\'' +
                ", isShortsEligible=" + isShortsEligible +
                ", authorInfo=" + authorInfo +
                '}';
    }
}
