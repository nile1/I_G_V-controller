package com.example.nileshgupta.car;

import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import java.io.IOException;

interface SwipeInterface {

    public void bottom2top(View v) throws IOException;

    public void left2right(View v) throws IOException;

    public void right2left(View v) throws IOException;

    public void top2bottom(View v) throws IOException;



}
public class ActivitySwipeDetector implements View.OnTouchListener {

    String msg;
    private SwipeInterface activity;
    static final int MIN_DISTANCE = 100;
    private float downX, downY, upX, upY;

    public ActivitySwipeDetector(SwipeInterface activity){
        this.activity = activity;
    }

    public void onRightToLeftSwipe(View v) throws IOException {
        System.out.println( "RightToLeftSwipe!");
       activity.right2left(v);
    }

    public void onLeftToRightSwipe(View v) throws IOException {
        System.out.println( "LeftToRightSwipe!");
       activity.left2right(v);
    }

    public void onTopToBottomSwipe(View v) throws IOException {
        System.out.println( "onTopToBottomSwipe!");
        activity.top2bottom(v);
    }

    public void onBottomToTopSwipe(View v) throws IOException {
        System.out.println( "onBottomToTopSwipe!");
        activity.bottom2top(v);
    }


    public boolean onTouch(View v, MotionEvent event) {
        switch(event.getAction()){
            case MotionEvent.ACTION_DOWN: {
                downX = event.getX();
                downY = event.getY();
                return true;
            }
            case MotionEvent.ACTION_UP: {
                upX = event.getX();
                upY = event.getY();

                float deltaX = downX - upX;
                float deltaY = downY - upY;

                // swipe horizontal?
                if(Math.abs(deltaX) > MIN_DISTANCE){
                    // left or right
                    if(deltaX < 0) {
                        try {
                            this.onLeftToRightSwipe(v);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        return true; }
                    if(deltaX > 0) {
                        try {
                            this.onRightToLeftSwipe(v);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        return true; }
                }
                else {
                    System.out.println( "Swipe was only " + Math.abs(deltaX) + " long, need at least " + MIN_DISTANCE);
                }

                // swipe vertical?
                if(Math.abs(deltaY) > MIN_DISTANCE){
                    // top or down
                    if(deltaY < 0) {
                        try {
                            this.onTopToBottomSwipe(v);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        return true; }
                    if(deltaY > 0) {
                        try {
                            this.onBottomToTopSwipe(v);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        return true; }
                }
                else {
                    System.out.println( "Swipe was only " + Math.abs(deltaX) + " long, need at least " + MIN_DISTANCE);
                    v.performClick();
                }
            }
        }
        return false;
    }


}
