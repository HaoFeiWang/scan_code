package com.whf.scancode;

public class ScanParams {

    private Integer width;
    private Integer height;

    public Integer getWidth() {
        return width;
    }

    public void setWidth(Integer width) {
        this.width = width;
    }

    public Integer getHeight() {
        return height;
    }

    public void setHeight(Integer height) {
        this.height = height;
    }

    @Override
    public String toString() {
        return "ScanParams{" +
                "width=" + width +
                ", height=" + height +
                '}';
    }
}
