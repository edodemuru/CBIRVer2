package com.example.rosannacatte.cbirsoftwarever2;

/**
 * Created by rosannacatte on 23/07/17.
 */

public class InputBackgroundTask {
    private String imagePath;
    private Comparatore comparatore;

    public InputBackgroundTask(String imagePath, Comparatore comparatore){
        this.setImagePath(imagePath);
        this.setComparatore(comparatore);

    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public Comparatore getComparatore() {
        return comparatore;
    }

    public void setComparatore(Comparatore comparatore) {
        this.comparatore = comparatore;
    }
}
