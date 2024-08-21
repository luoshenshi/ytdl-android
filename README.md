# ytdl-android

A simple Android library for fetching YouTube video information.

## Features

- **Easy to Use:** Fetch video information with a single line of code.
- **Efficient Caching:** Reduces redundant network requests.
- **Powered by OkHttp:** For fast and reliable HTTP requests.

## Installation

Include Ytdl-Android in your project using Maven or Gradle.

### Maven

```xml

<dependency>
    <groupId>io.github.luoshenshi</groupId>
    <artifactId>ytdl-android</artifactId>
    <version>1.0</version>
</dependency>
```

### Gradle (Kotlin)

```groovy
implementation("io.github.luoshenshi:ytdl-android:1.0")
```

## Usage

```java
YTDL.getInfoAsync("eYfjh0U6uQU",new YtdlResponse<>() {
    @Override
    public void onResponse (VideoInfo videoInfo){
        Log.d(TAG, videoInfo.getFormats());
    }

    @Override
    public void onFailure (Exception e){
        throw new IllegalArgumentException(e.getLocalizedMessage());
    }
});
```

## API Documentation

Ytdl-Android provides an intuitive API to interact with YouTube data. Here are some of the key
classes and methods:

- **`YTDL`**
    - `getInfoAsync(String videoId, YtdlResponse<VideoInfo> response)`: Retrieves video information.

- **`VideoInfo`**
    - `getTitle()`: Returns the title of the video.
    - `getAuthorInfo()`: Returns the author's information.
    - `getLikes()`: Returns the number of likes.
    - `getViews()`: Returns the number of views.

## Contributing

Contributions are welcome! Please feel free to submit a pull request or open an issue.