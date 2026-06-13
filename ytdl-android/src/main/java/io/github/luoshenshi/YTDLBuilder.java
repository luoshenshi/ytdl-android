package io.github.luoshenshi;

import okhttp3.OkHttpClient;

/**
 * A builder for {@link YTDL} instances.
 */
public class YTDLBuilder {
    private OkHttpClient httpClient;

    YTDLBuilder() {}

    /**
     * Sets the custom {@link OkHttpClient} to be used for requests.
     * Useful for configuring proxies, timeouts, etc.
     *
     * @param httpClient the custom OkHttpClient
     * @return this builder
     */
    public YTDLBuilder httpClient(OkHttpClient httpClient) {
        this.httpClient = httpClient;
        return this;
    }

    /**
     * Builds and returns a new {@link YTDL} instance.
     *
     * @return a new YTDL instance
     */
    public YTDL build() {
        if (httpClient == null) {
            okhttp3.Dispatcher dispatcher = new okhttp3.Dispatcher(new java.util.concurrent.ThreadPoolExecutor(
                    0, Integer.MAX_VALUE, 60, java.util.concurrent.TimeUnit.SECONDS,
                    new java.util.concurrent.SynchronousQueue<>(), runnable -> {
                Thread thread = new Thread(runnable, "YTDL OkHttp Dispatcher");
                thread.setDaemon(true);
                return thread;
            }));

            httpClient = new okhttp3.OkHttpClient.Builder()
                    .dispatcher(dispatcher)
                    .build();
        }
        return new YTDL(httpClient);
    }
}
