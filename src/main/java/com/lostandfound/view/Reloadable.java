package com.lostandfound.view;

/** A screen that can re-fetch its data, MainView calls this when its tab is selected. */
public interface Reloadable {
    void reload();
}
