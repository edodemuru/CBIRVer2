package com.example.rosannacatte.cbirsoftwarever2;

import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;

/**
 * Created by rosannacatte on 08/07/17.
 */

public class ImmagineOrb {
    private MatOfKeyPoint keypoints;
    private Mat descriptors;
    private String path;

    public ImmagineOrb(MatOfKeyPoint keypoints, Mat descriptors,String path){
        this.setKeypoints(keypoints);
        this.setDescriptors(descriptors);
        this.setPath(path);

    }

    public MatOfKeyPoint getKeypoints() {
        return keypoints;
    }

    public void setKeypoints(MatOfKeyPoint keypoints) {
        this.keypoints = keypoints;
    }

    public Mat getDescriptors() {
        return descriptors;
    }

    public void setDescriptors(Mat descriptors) {
        this.descriptors = descriptors;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
