package io.adaptant.labs.flutter_windowmanager;

import android.app.Activity;
import android.content.Context;
import android.hardware.display.DisplayManager;
import android.media.MediaRouter;
import android.media.MediaRouter.RouteInfo;
import android.os.Build;
import android.view.WindowManager;

import androidx.annotation.RequiresApi;

import io.flutter.embedding.android.FlutterActivity;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;

/** FlutterWindowManagerPlugin */
public class FlutterWindowManagerPlugin implements MethodCallHandler {
  private final Activity activity;
  private Object mDisplayListener;
  private MyDisplayListener mListener;

  public interface MyDisplayListener {
    public void onDisplayChanged();
  }

  private FlutterWindowManagerPlugin(Registrar registrar) {
    this.activity = registrar.activity();
  }

  /** Plugin registration. */
  public static void registerWith(Registrar registrar) {
    final MethodChannel channel = new MethodChannel(registrar.messenger(), "flutter_windowmanager");
    FlutterWindowManagerPlugin instance = new FlutterWindowManagerPlugin(registrar);
    channel.setMethodCallHandler(instance);
  }

  /**
   * Validate flag specification against WindowManager.LayoutParams and API levels, as per:
   * https://developer.android.com/reference/android/view/WindowManager.LayoutParams
   */
  @SuppressWarnings("deprecation")
  private boolean validLayoutParam(int flag) {
    switch (flag) {
      case WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON:
      case WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM:
      case WindowManager.LayoutParams.FLAG_DIM_BEHIND:
      case WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN:
      case WindowManager.LayoutParams.FLAG_FULLSCREEN:
      case WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED:
      case WindowManager.LayoutParams.FLAG_IGNORE_CHEEK_PRESSES:
      case WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON:
      case WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR:
      case WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN:
      case WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS:
      case WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE:
      case WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE:
      case WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL:
      case WindowManager.LayoutParams.FLAG_SCALED:
      case WindowManager.LayoutParams.FLAG_SECURE:
      case WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER:
      case WindowManager.LayoutParams.FLAG_SPLIT_TOUCH:
      case WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH:
        return true;
      case WindowManager.LayoutParams.FLAG_BLUR_BEHIND:
        return !(Build.VERSION.SDK_INT >= 15);
      case WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD:
        return (Build.VERSION.SDK_INT >= 5 && Build.VERSION.SDK_INT < 26);
      case WindowManager.LayoutParams.FLAG_DITHER:
        return !(Build.VERSION.SDK_INT >= 17);
      case WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS:
        return (Build.VERSION.SDK_INT >= 21);
      case WindowManager.LayoutParams.FLAG_LAYOUT_ATTACHED_IN_DECOR:
        return (Build.VERSION.SDK_INT >= 22);
      case WindowManager.LayoutParams.FLAG_LAYOUT_IN_OVERSCAN:
        return (Build.VERSION.SDK_INT >= 18);
      case WindowManager.LayoutParams.FLAG_LOCAL_FOCUS_MODE:
        return (Build.VERSION.SDK_INT >= 19);
      case WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED:
        return !(Build.VERSION.SDK_INT >= 27);
      case WindowManager.LayoutParams.FLAG_TOUCHABLE_WHEN_WAKING:
        return !(Build.VERSION.SDK_INT >= 20);
      case WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION:
      case WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS:
        return (Build.VERSION.SDK_INT >= 19);
      case WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON:
        return !(Build.VERSION.SDK_INT >= 27);
      default:
        return false;
    }
  }

  private boolean validLayoutParams(Result result, int flags) {
    for (int i = 0; i < Integer.SIZE; i++) {
      int flag = (1 << i);
      if ((flags & flag) == 1) {
        if (!validLayoutParam(flag)) {
          result.error("FlutterWindowManagerPlugin","FlutterWindowManagerPlugin: invalid flag specification: " + Integer.toHexString(flag), null);
          return false;
        }
      }
    }

    return true;
  }

  @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
  @Override
  public void onMethodCall(MethodCall call, Result result) {
    final int flags = call.argument("flags");



    if (activity == null) {
      result.error("FlutterWindowManagerPlugin", "FlutterWindowManagerPlugin: ignored flag state change, current activity is null", null);
    }

    if (!validLayoutParams(result, flags)) {
      return;
    }


    switch (call.method) {
      case "addFlags":
        System.out.println("ENTERED ADD FLAGS");
        activity.getWindow().addFlags(flags);
        result.success(true);
        break;
      case "clearFlags":
        activity.getWindow().clearFlags(flags);
        result.success(true);
        break;
      case "detectDevices":
        System.out.println("ENTERED DETECT DEVICES");
        initDisplayListener();
        DisplayManager displayManager = (DisplayManager) activity.getApplicationContext().getSystemService(Context.DISPLAY_SERVICE);
        System.out.println("DISPLAY PRESENTATION NAMES: " + displayManager.getDisplays(DisplayManager.DISPLAY_CATEGORY_PRESENTATION).toString());
        System.out.println("DISPLAY NAMES: " + displayManager.getDisplays().toString());
        System.out.println("DISPLAY COUNT: " + displayManager.getDisplays().length);
        result.success(displayManager.getDisplays().length);
      default:
        result.notImplemented();
    }
  }

  private void initDisplayListener() {

    System.out.println("INIT DISPLAY LISTENER");


    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1) {
      System.out.println("INIT DISPLAY LISTENER ACCEPTED");
      mDisplayListener = new DisplayManager.DisplayListener() {
        @Override
        public void onDisplayAdded(int arg0) {
          System.out.println("DISPLAY ADDED");
        }

        @Override
        public void onDisplayChanged(int arg0) {
          System.out.println("ON DISPLAY CHANGED");
          if (mListener != null) {
            System.out.println("DISPLAY CHANGED");
            mListener.onDisplayChanged();
          }
        }

        @Override
        public void onDisplayRemoved(int arg0) {
          System.out.println("DISPLAY REMOVED");
        }
      };
    } else {
      System.out.println("INIT DISPLAY LISTENER REFUSED");
    }
  }

}
