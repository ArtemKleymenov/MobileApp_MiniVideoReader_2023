package com.example.minivideoreader;

import android.graphics.Bitmap;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

public class BestFrameSelector {
    public Boolean process(Bitmap bitmap) {
        Mat image = new Mat();
        Utils.bitmapToMat(bitmap, image);

        Mat image_lab = new Mat();
        Imgproc.cvtColor(image, image_lab, Imgproc.COLOR_BGR2Lab);

        Mat shifted = new Mat();
        Imgproc.pyrMeanShiftFiltering(image_lab, shifted, 20, 45);

        Mat shifted_gray = new Mat();
        Imgproc.cvtColor(shifted, shifted_gray, Imgproc.COLOR_BGR2GRAY);

        Mat thresh = new Mat();
        Imgproc.threshold(shifted_gray, thresh, 0, 255, Imgproc.THRESH_BINARY | Imgproc.THRESH_OTSU);

        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(thresh.clone(), contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
        MatOfPoint largest_contour = contours.get(0);
        double maxArea = Imgproc.contourArea(largest_contour);

        for (MatOfPoint contour : contours) {
            double area = Imgproc.contourArea(contour);
            if (area > maxArea) {
                maxArea = area;
                largest_contour = contour;
            }
        }

        Rect boundingRect = Imgproc.boundingRect(largest_contour);
        int x = boundingRect.x;
        int y = boundingRect.y;
        int w = boundingRect.width;
        int h = boundingRect.height;
        int center_x = x + w / 2;
        int center_y = y + h / 2;
        int a = w / 2;
        int b = h / 2;

        Mat canvas = Mat.zeros(image.size(), image.type());
        Imgproc.ellipse(canvas, new Point(center_x, center_y), new Size(a, b), 0, 0, 360, new Scalar(255, 255, 255), -1);
        Core.bitwise_and(image, canvas, image);

        Mat gray_image = new Mat();
        Imgproc.cvtColor(image, gray_image, Imgproc.COLOR_BGR2GRAY);
        int total_pixels = gray_image.rows() * gray_image.cols();
        int black_pixels = Core.countNonZero(gray_image);
        double percentage = (double) black_pixels / total_pixels * 100;
        if (percentage > 60.0) {
            return false;
        }

        Mat blurred = new Mat();
        Imgproc.medianBlur(image, blurred, 15);
        int[] hsv_max_range = new int[126];
        for (int i = 0; i < 126; i++) {
            hsv_max_range[i] = 255 - i;
        }

        for (int v_max : hsv_max_range) {
            Scalar hsv_max = new Scalar(255, 255, v_max);
            Mat hsv = new Mat();
            Imgproc.cvtColor(blurred, hsv, Imgproc.COLOR_BGR2HSV);
            Mat thresh_local = new Mat();
            Core.inRange(hsv, new Scalar(0, 0, 0), hsv_max, thresh_local);

            List<MatOfPoint> contours_local = new ArrayList<>();
            Mat hierarchy_local = new Mat();
            Imgproc.findContours(thresh_local, contours_local, hierarchy_local, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);

            for (MatOfPoint cnt : contours_local) {
                double area = Imgproc.contourArea(cnt);
                if (3479 < area && area < 9000) {
                    RotatedRect rect = Imgproc.minAreaRect(new MatOfPoint2f(cnt.toArray()));
                    Point[] box = new Point[4];
                    rect.points(box);

                    double length = Math.sqrt(Math.pow(box[0].x - box[1].x, 2) + Math.pow(box[0].y - box[1].y, 2));
                    double width = Math.sqrt(Math.pow(box[1].x - box[2].x, 2) + Math.pow(box[1].y - box[2].y, 2));

                    if (Math.abs(width - length) < width / 4) {
                        boolean validBox = true;
                        for (Point point : box) {
                            if (point.x < 0 || point.x >= image.cols() || point.y < 0 || point.y >= image.rows()) {
                                validBox = false;
                                break;
                            }
                        }
                        if (validBox) {
                            boolean containsBlackPixel = false;
                            for (Point point : box) {
                                if (image.get((int) point.y, (int) point.x)[0] == 0) {
                                    containsBlackPixel = true;
                                    break;
                                }
                            }
                            if (containsBlackPixel) {
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }
//
//    public Boolean select(Bitmap bitmap) {
//        Mat image = new Mat();
//        Utils.bitmapToMat(bitmap, image);
//
//        if (image.rows() < 300) {
//            int width = image.rows() * 5;
//            int height = image.rows() * 5;
//            Imgproc.resize(image, image, new Size(width, height));
//        }
//
//        Mat image_lab = new Mat();
//        Imgproc.cvtColor(image, image_lab, Imgproc.COLOR_BGR2Lab);
//
//        Mat shifted = new Mat();
//        Imgproc.pyrMeanShiftFiltering(image_lab, shifted, 20, 45);
//
//        Mat shifted_gray = new Mat();
//        Imgproc.cvtColor(shifted, shifted_gray, Imgproc.COLOR_BGR2GRAY);
//
//        Mat thresh = new Mat();
//        Imgproc.threshold(shifted_gray, thresh, 0, 255, Imgproc.THRESH_BINARY | Imgproc.THRESH_OTSU);
//
//        List<MatOfPoint> contours = new ArrayList<>();
//        Mat hierarchy = new Mat();
//        Imgproc.findContours(thresh.clone(), contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
//        MatOfPoint largest_contour = contours.get(0);
//        for (MatOfPoint contour : contours) {
//            if (Imgproc.contourArea(contour) > Imgproc.contourArea(largest_contour)) {
//                largest_contour = contour;
//            }
//        }
//
//        Rect boundingRect = Imgproc.boundingRect(largest_contour);
//        int x = boundingRect.x;
//        int y = boundingRect.y;
//        int w = boundingRect.width;
//        int h = boundingRect.height;
//        int center_x = x + w / 2;
//        int center_y = y + h / 2;
//        int a = w / 2;
//        int b = h / 2;
//
//        Mat canvas = Mat.zeros(image.size(), image.type());
//        Imgproc.ellipse(canvas, new Point(center_x, center_y), new Size(a, b), 0, 0, 360, new Scalar(255, 255, 255), -1);
//        Core.bitwise_and(image, canvas, image);
//
//        Mat gray_image = new Mat();
//        Imgproc.cvtColor(image, gray_image, Imgproc.COLOR_BGR2GRAY);
//        int total_pixels = gray_image.rows() * gray_image.cols();
//        int black_pixels = Core.countNonZero(gray_image);
//        double percentage = (double) black_pixels / total_pixels * 100;
//        if (percentage > 60) {
//            System.out.println(false);
//        }
//
//        Mat blurred = new Mat();
//        Imgproc.medianBlur(image, blurred, 15);
//        int[] hsv_max_range = new int[126];
//        for (int i = 0; i < 126; i++) {
//            hsv_max_range[i] = 255 - i;
//        }
//
//        for (int v_max : hsv_max_range) {
//            Mat hsv_max = new Mat(new Scalar(255, 255, v_max), CvType.CV_8UC3);
//            Mat hsv = new Mat();
//            Imgproc.cvtColor(blurred, hsv, Imgproc.COLOR_BGR2HSV);
//            Mat mask = new Mat();
//            Core.inRange(hsv, new Scalar(0, 0, 0), hsv_max, mask);
//            List<MatOfPoint> contours = new ArrayList<>();
//            Mat hierarchy = new Mat();
//            Imgproc.findContours(thresh, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);
//            for (MatOfPoint cnt : contours) {
//                double area = Imgproc.contourArea(cnt);
//                if (3479 < area && area < 9000) {
//                    RotatedRect rect = Imgproc.minAreaRect(new MatOfPoint2f(cnt.toArray()));
//                    Point[] boxPoints = new Point[4];
//                    rect.points(boxPoints);
//                    int[][] box = new int[4][2];
//                    for (int i = 0; i < 4; i++) {
//                        box[i][0] = (int) boxPoints[i].x;
//                        box[i][1] = (int) boxPoints[i].y;
//                    }
//                    double length = Math.sqrt(Math.pow(box[0][0] - box[1][0], 2) + Math.pow(box[0][1] - box[1][1], 2));
//                    double width = Math.sqrt(Math.pow(box[1][0] - box[2][0], 2) + Math.pow(box[1][1] - box[2][1], 2));
//                    if (Math.abs(width - length) < width / 4) {
//                        boolean validBox = true;
//                        for (int i = 0; i < 4; i++) {
//                            if (box[i][0] < 0 || box[i][0] >= image.cols() || box[i][1] < 0 || box[i][1] >= image.rows()) {
//                                validBox = false;
//                                break;
//                            }
//                        }
//                        if (validBox) {
//                            boolean containsBlackPixel = false;
//                            for (int i = 0; i < 4; i++) {
//                                if (image.get(box[i][1], box[i][0])[0] == 0) {
//                                    containsBlackPixel = true;
//                                    break;
//                                }
//                            }
//                            if (containsBlackPixel) {
//                                return true;
//                            }
//                        }
//                    }
//                }
//            }
//
//        }
//        return null;
//    }

}
