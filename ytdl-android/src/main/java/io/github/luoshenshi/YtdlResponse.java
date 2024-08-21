package io.github.luoshenshi;

public interface YtdlResponse<T> {
    void onResponse(T videoInfo);

    void onFailure(Exception e);
}
