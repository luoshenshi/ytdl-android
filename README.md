# Ytdl-android

A powerful and efficient Java library for fetching YouTube video metadata, deciphering signatures, and extracting all available stream formats by simulating multiple YouTube clients.

## Features

- **Advanced Extraction:** Spoofs various clients (iOS, Android, VR, TV, Web) to bypass format restrictions and retrieve maximum available streams.
- **Native Async Support:** Fully utilizes Java's `CompletableFuture` for non-blocking, modern asynchronous operations.
- **Java Records:** Clean, immutable data models for easy parsing and memory efficiency.
- **JSON Export:** Easily convert extracted video formats directly into native `org.json.JSONObject` or string representations.
- **Signature Deciphering:** Automatically resolves and deciphers YouTube's dynamic signature ciphers.
- **Powered by OkHttp:** Fast, reliable, and thread-safe HTTP requests.

## Installation

Include Ytdl-android in your project using Maven or Gradle.

### Maven

```xml
<dependency>
    <groupId>io.github.luoshenshi</groupId>
    <artifactId>ytdl-android</artifactId>
    <version>1.2</version>
</dependency>
```

### Gradle

```groovy
implementation "io.github.luoshenshi:ytdl-android:1.2"
```

## Usage

### Initialization

```java
import io.github.luoshenshi.YTDL;

YTDL ytdl = YTDL.builder().build();
```

### Async Example

```java
ytdl.getVideoInfo("Egx0AXIIChA").thenAccept(info -> {
    System.out.println("Title: " + info.title());
    System.out.println("Author: " + info.author().name());
    System.out.println("Views: " + info.views());
}).exceptionally(e -> {
    e.printStackTrace();
    return null;
});
```

### Sync Example

```java
VideoInfo info = ytdl.getVideoInfo("VZGt8DFyX6A").join();
System.out.println(info.title());
```

### JSON Export

```java
JSONArray arr = new JSONArray();

ytdl.getVideoInfo("kJQP7kiw5Fk").thenAccept(info -> {
    for (VideoFormat f : info.formats()) {
        arr.put(f.toJson());
    }

    System.out.println(arr.toString(2));
});
```

### Cleanup

```java
ytdl.close();
```

## License

MIT License

## Contributing

Contributions are welcome! Please feel free to submit a pull request or open an issue.