package ma.wanam.youtubeadaway;

import android.os.AsyncTask;
import android.os.Handler;
import android.text.TextUtils;

import java.util.regex.Pattern;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import ma.wanam.youtubeadaway.utils.Class3C;
import ma.wanam.youtubeadaway.utils.Constants;

public class BFAsync extends AsyncTask<XC_LoadPackage.LoadPackageParam, Void, Boolean> {
    private boolean DEBUG = BuildConfig.DEBUG;
    private volatile Method emptyComponentMethod = null;
    private volatile Method fingerprintMethod = null;
    private volatile Optional<Field> pathBuilderField = Optional.empty();
    private volatile Optional<Field> emptyComponentField = Optional.empty();
    private XC_MethodHook.Unhook unhookFilterMethod;
    private volatile boolean isAtTopOfView = true;
    private Handler handler;

    public Handler getHandler() {
        if (handler == null) {
            handler = new Handler();
        }
        return handler;
    }

    private static final String filterAds = new StringBuilder().append(".*(").append(String.join("|", new String[]{
            "ads_video_with_context",
            "banner_text_icon",
            "square_image_layout",
            "watch_metadata_app_promo",
            "video_display_full_layout",
            "browsy_bar",
            "compact_movie",
            "horizontal_movie_shelf",
            "movie_and_show_upsell_card",
            "compact_tvfilm_item",
            "video_display_full_buttoned_layout",
            "full_width_square_image_layout",
            "_ad_with",
            "landscape_image_wide_button_layout",
            "carousel_ad",
            "paid_content_overlay"
    })).append(").*").toString();

    private static final String filterIgnore = new StringBuilder().append(".*(").append(String.join("|", new String[]{
            "home_video_with_context",
            "related_video_with_context",
            "comment_thread",
            "comment\\.",
            "download_",
            "library_recent_shelf",
            "playlist_add_to_option_wrapper"
    })).append(").*").toString();

    private static final Pattern filterAdsPattern = Pattern.compile(filterAds);
    private static final Pattern filterIgnorePattern = Pattern.compile(filterIgnore);

    @Override
    protected Boolean doInBackground(XC_LoadPackage.LoadPackageParam... params) {
        ClassLoader cl = params[0].classLoader;

        if (params[0].packageName.equals(Constants.GOOGLE_YOUTUBE_PACKAGE) && Xposed.prefs.getBoolean("hide_ad_cards", false)) {
            XC_MethodHook onBackPressedHook = new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (isAtTopOfView) {
                        XposedHelpers.callMethod(param.thisObject, "finish");
                    } else {
                        getHandler().removeCallbacksAndMessages(null);
                        getHandler().postDelayed(() -> isAtTopOfView = true, 1000);
                    }
                }
            };

            boolean hooked = false;
            Class<?> watchWhileActivityClass = XposedHelpers.findClassIfExists("com.google.android.apps.youtube.app.watchwhile.WatchWhileActivity", cl);
            if (watchWhileActivityClass != null) {
                try {
                    XposedHelpers.findAndHookMethod(watchWhileActivityClass, "onBackPressed", onBackPressedHook);
                    hooked = true;
                } catch (Throwable e) {
                    XposedBridge.log("YouTube AdAway: WatchWhileActivity found but onBackPressed hook failed: " + e.getMessage());
                }
            } else {
                XposedBridge.log("YouTube AdAway: WatchWhileActivity class not found.");
            }

            if (!hooked) {
                XposedBridge.log("YouTube AdAway: Trying MainActivity fallback...");
                try {
                    XposedHelpers.findAndHookMethod("com.google.android.apps.youtube.app.watchwhile.MainActivity", cl, "onBackPressed", onBackPressedHook);
                } catch (Throwable e2) {
                    XposedBridge.log("YouTube AdAway: Failed to hook MainActivity.onBackPressed");
                    XposedBridge.log(e2);
                }
            }

            XC_MethodHook recyclerViewHook = new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    getHandler().removeCallbacksAndMessages(null);
                    isAtTopOfView = false;
                    getHandler().postDelayed(
                            () -> isAtTopOfView = !(boolean) XposedHelpers.callMethod(param.thisObject, "canScrollVertically", -1)
                            , 1000);
                }
            };

            try {
                XposedHelpers.findAndHookMethod("android.support.v7.widget.RecyclerView", cl, "stopNestedScroll", recyclerViewHook);
            } catch (Throwable e) {
                // Ignore, might be using AndroidX
            }

            try {
                XposedHelpers.findAndHookMethod("androidx.recyclerview.widget.RecyclerView", cl, "stopNestedScroll", recyclerViewHook);
            } catch (Throwable e) {
                // Ignore
            }
        }

        return bruteForceAds(cl);
    }

    private boolean bruteForceAds(ClassLoader cl) {
        Instant start = Instant.now();
        boolean foundBGClass = !Xposed.prefs.getBoolean("enable_bg_playback", true),
                foundInVideoAds = !Xposed.prefs.getBoolean("hide_invideo_ads", true),
                foundCardAds = !Xposed.prefs.getBoolean("hide_ad_cards", false),
                skip;

        Class3C heapPermutation = new Class3C();
        StringBuilder sb = new StringBuilder();
        while (heapPermutation.hasNext()) {
            String clsName = heapPermutation.next();
            skip = false;

            if (foundInVideoAds && foundCardAds && foundBGClass) return true;

            String nameWithA = null;
            if (!skip && !foundInVideoAds) {
                sb.setLength(0);
                nameWithA = sb.append('a').append(clsName).toString();
                foundInVideoAds = findAndHookInvideoAds(nameWithA, cl);
                if (foundInVideoAds) {
                    skip = true;
                    XposedBridge.log("In-Video ads hooks applied in " + Duration.between(start, Instant.now()).getSeconds() + " seconds!");
                }
            }

            if (!skip && !foundBGClass) {
                if (nameWithA == null) {
                    sb.setLength(0);
                    nameWithA = sb.append('a').append(clsName).toString();
                }
                foundBGClass = findAndHookVideoBGP4C(nameWithA, cl);
                if (foundBGClass) {
                    skip = true;
                    XposedBridge.log("Video BG playback hooks applied in " + Duration.between(start, Instant.now()).getSeconds() + " seconds!");
                }
            }

            if (!skip && !foundCardAds) {
                foundCardAds = findAdCardsMethods(clsName, cl);
                if (foundCardAds) {
                    hookAdCardsMethods(fingerprintMethod, emptyComponentMethod);
                    XposedBridge.log("Ad cards hooks applied in " + Duration.between(start, Instant.now()).getSeconds() + " seconds!");
                }
            }
        }
        return false;
    }

    private boolean checkFieldSignature(Class<?> clazz, int maxFields, List<Class<?>> types, int expectedCount) {
        Field[] fields = clazz.getDeclaredFields();
        if (maxFields > 0 && fields.length > maxFields) return false;
        int count = 0;
        for (Field field : fields) {
            if (types.contains(field.getType())) {
                count++;
            }
        }
        return count == expectedCount;
    }

    private boolean checkMethodParams(Method method, Class<?> returnType, Class<?>... paramTypes) {
        if (returnType != null && !method.getReturnType().equals(returnType)) return false;
        if (paramTypes.length != method.getParameterTypes().length) return false;
        for (int i = 0; i < paramTypes.length; i++) {
            if (paramTypes[i] != null && !paramTypes[i].equals(method.getParameterTypes()[i])) return false;
        }
        return true;
    }

    private boolean findAndHookInvideoAds(String clsName, ClassLoader cl) {
        Class<?> aClass;
        Method[] methods;

        try {
            aClass = XposedHelpers.findClass(clsName, cl);
            methods = aClass.getDeclaredMethods();
        } catch (Throwable e1) {
            return false;
        }

        try {
            if (checkFieldSignature(aClass, 10, Arrays.asList(Executor.class, LinkedBlockingQueue.class, Runnable.class), 3)) {
                Method fMethod = null;
                for (Method method : methods) {
                    if (checkMethodParams(method, void.class, boolean.class) && Modifier.isFinal(method.getModifiers())) {
                        fMethod = method;
                        break;
                    }
                }

                if (fMethod != null) {
                    XposedBridge.hookMethod(fMethod, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            param.args[0] = false;
                        }
                    });
                    XposedBridge.log("Found ad class: " + aClass.getName() + "." + fMethod.getName());
                    return true;
                }
            }

        } catch (Throwable e) {
            XposedBridge.log("YouTube AdAway: Failed to hook in-video ads class: " + aClass.getName());
            XposedBridge.log(e);
        }
        return false;
    }

    private boolean findAndHookVideoBGP4C(String clsName, ClassLoader cl) {
        Class<?> aClass;
        Method[] methods;

        try {
            aClass = XposedHelpers.findClass(clsName, cl);
            methods = aClass.getDeclaredMethods();
        } catch (Throwable e1) {
            return false;
        }

        try {
            List<Method> fMethods = new ArrayList<>();
            for (Method method : methods) {
                if (checkMethodParams(method, boolean.class, (Class<?>) null)
                        && method.getName().length() == 1
                        && method.getParameterTypes()[0].getName().length() == 4
                        && method.getName().equals(method.getName().toLowerCase())
                        && Modifier.isStatic(method.getModifiers())
                        && Modifier.isPublic(method.getModifiers())) {
                    fMethods.add(method);
                }
            }

            if (fMethods.size() > 5) {
                XposedBridge.hookMethod(fMethods.get(0), XC_MethodReplacement.returnConstant(true));
                XposedBridge.log("Found bg class: " + aClass.getName() + "." + fMethods.get(0).getName());
                return true;
            }

        } catch (Throwable e) {
            XposedBridge.log("YouTube AdAway: Failed to hook video bg playback class: " + aClass.getName());
            XposedBridge.log(e);
        }
        return false;
    }

    private boolean findAdCardsMethods(String clsName, ClassLoader cl) {
        Class<?> aClass;
        Method[] methods;

        try {
            aClass = XposedHelpers.findClass(clsName, cl);
            methods = aClass.getDeclaredMethods();
        } catch (Throwable e1) {
            return false;
        }

        try {
            if (fingerprintMethod == null) {
                List<Method> fMethods = new ArrayList<>();
                for (Method method : methods) {
                    if (checkMethodParams(method, null, null, null, null, null, null, int.class, boolean.class)
                            && method.getParameterTypes()[0].getName().length() == 3
                            && method.getName().equals(method.getName().toLowerCase())
                            && Modifier.isFinal(method.getModifiers())
                            && Modifier.isPublic(method.getModifiers())) {
                        fMethods.add(method);
                    }
                }

                if (fMethods.size() == 1) {
                    fingerprintMethod = fMethods.size() == 1 ? fMethods.get(0) : null;
                    XposedBridge.log("Found ad cards class: " + aClass.getName() + "." + fMethods.get(0).getName());
                    return fingerprintMethod != null && emptyComponentMethod != null;
                }
            }
        } catch (Throwable e) {
            XposedBridge.log("YouTube AdAway: Failed to hook ad cards class: " + aClass.getName());
            XposedBridge.log(e);
        }

        try {
            if (emptyComponentMethod == null) {
                List<Method> fMethods = new ArrayList<>();
                for (Method method : methods) {
                    if (Modifier.isPublic(method.getModifiers())
                            && Modifier.isStatic(method.getModifiers())
                            && method.getParameterTypes().length == 1) {
                        fMethods.add(method);
                    }
                }

                List<Method> fMethods2 = new ArrayList<>();
                for (Method method : methods) {
                    if (Modifier.isProtected(method.getModifiers())
                            && Modifier.isFinal(method.getModifiers())
                            && method.getParameterTypes().length == 1) {
                        fMethods2.add(method);
                    }
                }

                if (fMethods.size() == 1 && fMethods2.size() == 1 && methods.length == 2) {
                    emptyComponentMethod = fMethods.get(0);
                    XposedBridge.log("Found emptyComponent class: " + aClass.getName() + "." + fMethods.get(0).getName());
                    return fingerprintMethod != null && emptyComponentMethod != null;
                }
            }
        } catch (Throwable e) {
            XposedBridge.log("YouTube AdAway: Failed to hook EmptyElement class: " + aClass.getName());
            XposedBridge.log(e);
        }
        return false;
    }

    private Optional<Field> findFieldInHierarchy(Class<?> clazz, Class<?> type) {
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                if (field.getType().equals(type)) {
                    field.setAccessible(true);
                    return Optional.of(field);
                }
            }
            current = current.getSuperclass();
        }
        return Optional.empty();
    }

    private void hookAdCardsMethods(Method fingerprintMethod, final Method emptyComponentMethod) {

        try {
            unhookFilterMethod = XposedBridge.hookMethod(fingerprintMethod, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
                    if (!pathBuilderField.isPresent()) {
                        pathBuilderField = findFieldInHierarchy(param.args[1].getClass(), StringBuilder.class);
                    }

                    if (pathBuilderField.isPresent()) {
                        // Get template path
                        String pathBuilder = pathBuilderField.get().get(param.args[1]).toString();
                        if (!TextUtils.isEmpty(pathBuilder) && !filterIgnorePattern.matcher(pathBuilder).matches() && filterAdsPattern.matcher(pathBuilder).matches()) {
                            // Create emptyComponent from current componentContext
                            Object x = emptyComponentMethod.invoke(null, param.args[0]);
                            // Get created emptyComponent
                            if (!emptyComponentField.isPresent()) {
                                emptyComponentField = Optional.of(XposedHelpers.findField(x.getClass(), "a"));
                            }
                            Object y = emptyComponentField.get().get(x);
                            param.setResult(y);
                        }
                    } else {
                        XposedBridge.log("Unable to find template's pathBuilder");
                        unhookFilterMethod.unhook();
                    }
                }
            });
        } catch (Throwable e) {
            XposedBridge.log("YouTube AdAway: Error hooking AdCards!");
            XposedBridge.log(e);
        }
    }

    @Override
    protected void onPostExecute(Boolean found) {
        if (!found) {
            XposedBridge.log("YouTube AdAway: brute force failed!");
        }
    }

}
