import android.os.SystemClock;
import android.view.InputDevice;
import android.view.InputEvent;
import android.view.KeyEvent;
import android.view.MotionEvent;

import java.io.BufferedReader;
import java.io.FileReader;
import java.lang.reflect.Method;

/**
 * Touch injector v2, run through app_process as the shell user (sendevent is blocked by SELinux).
 * Argument 0 is the script, commands separated by semicolons (or an at sign plus a file path to read the script
 * from a file on the phone).
 * Pointer ids are a digit suffix on the command letter (no suffix = pointer 0):
 *   D[id] x y              pointer down (the first one starts the gesture, further ones are secondary pointers)
 *   U[id]                  pointer up
 *   M[id] x y ms [mode]    move one pointer to (x,y) over ms, events every STEP ms
 *   MM ms mode id:x,y ...  move several pointers at once (pinch, two finger scroll)
 *   W ms                   wait without events (a held finger sends none)
 *   C                      cancel the whole gesture
 *   K code                 key press (4 = BACK, 3 = HOME)
 *   P name                 print a timestamp marker (uptime ms)
 *   STEP ms                event interval (default 4 ms = 250 Hz)
 * modes: lin (constant speed, default), ease (smoothstep), out (cubic ease out), in (cubic ease in), out2 (quadratic ease out)
 */
public class Inject {
    static final int MAXP = 6;
    static int stepMs = 4;
    static Object manager;
    static Method inject;
    static int injectArgs;
    static int deviceId;
    static long downTime;
    static final boolean[] act = new boolean[MAXP];
    static final float[] px = new float[MAXP];
    static final float[] py = new float[MAXP];

    public static void main(String[] args) throws Exception {
        setup();
        String script = args[0];
        if (script.startsWith("@")) {
            StringBuilder sb = new StringBuilder();
            try (BufferedReader r = new BufferedReader(new FileReader(script.substring(1)))) {
                String l;
                while ((l = r.readLine()) != null) sb.append(l).append(';');
            }
            script = sb.toString();
        }
        System.out.println("START " + SystemClock.uptimeMillis());
        for (String raw : script.split(";")) {
            String cmd = raw.trim();
            if (cmd.isEmpty() || cmd.startsWith("#")) continue;
            String[] p = cmd.split("\\s+");
            String c = p[0];
            if (c.equals("MM")) {
                multiMove(p);
                continue;
            }
            if (c.equals("STEP")) {
                stepMs = Integer.parseInt(p[1]);
                continue;
            }
            char letter = c.charAt(0);
            int id = c.length() > 1 ? Integer.parseInt(c.substring(1)) : 0;
            switch (letter) {
                case 'D': {
                    px[id] = Float.parseFloat(p[1]);
                    py[id] = Float.parseFloat(p[2]);
                    boolean first = count() == 0;
                    act[id] = true;
                    if (first) {
                        downTime = SystemClock.uptimeMillis();
                        send(MotionEvent.ACTION_DOWN, id);
                    } else {
                        send(MotionEvent.ACTION_POINTER_DOWN, id);
                    }
                    break;
                }
                case 'U': {
                    send(count() == 1 ? MotionEvent.ACTION_UP : MotionEvent.ACTION_POINTER_UP, id);
                    act[id] = false;
                    break;
                }
                case 'M': {
                    String mode = p.length > 4 ? p[4] : "lin";
                    float[] tx = new float[MAXP], ty = new float[MAXP];
                    boolean[] mv = new boolean[MAXP];
                    tx[id] = Float.parseFloat(p[1]);
                    ty[id] = Float.parseFloat(p[2]);
                    mv[id] = true;
                    move(tx, ty, mv, Long.parseLong(p[3]), mode);
                    break;
                }
                case 'W':
                    Thread.sleep(Long.parseLong(p[1]));
                    break;
                case 'C':
                    send(MotionEvent.ACTION_CANCEL, 0);
                    java.util.Arrays.fill(act, false);
                    break;
                case 'K':
                    key(Integer.parseInt(p[1]));
                    break;
                case 'P':
                    System.out.println("MARK " + p[1] + " " + SystemClock.uptimeMillis());
                    break;
                default:
                    System.out.println("unknown command " + cmd);
            }
        }
    }

    static int count() {
        int n = 0;
        for (boolean b : act) if (b) n++;
        return n;
    }

    static void multiMove(String[] p) throws Exception {
        long ms = Long.parseLong(p[1]);
        String mode = p[2];
        float[] tx = new float[MAXP], ty = new float[MAXP];
        boolean[] mv = new boolean[MAXP];
        for (int i = 3; i < p.length; i++) {
            String[] a = p[i].split("[:,]");
            int id = Integer.parseInt(a[0]);
            tx[id] = Float.parseFloat(a[1]);
            ty[id] = Float.parseFloat(a[2]);
            mv[id] = true;
        }
        move(tx, ty, mv, ms, mode);
    }

    static float profile(float a, String mode) {
        switch (mode) {
            case "ease": return a * a * (3f - 2f * a);
            case "out": return 1f - (1f - a) * (1f - a) * (1f - a);
            case "out2": return 1f - (1f - a) * (1f - a);
            case "in": return a * a * a;
            default: return a;
        }
    }

    static void move(float[] tx, float[] ty, boolean[] mv, long ms, String mode) throws Exception {
        float[] sx = px.clone(), sy = py.clone();
        long start = SystemClock.uptimeMillis();
        while (true) {
            long now = SystemClock.uptimeMillis();
            float a = ms <= 0 ? 1f : Math.min(1f, (now - start) / (float) ms);
            float e = profile(a, mode);
            for (int i = 0; i < MAXP; i++) {
                if (!mv[i]) continue;
                px[i] = sx[i] + (tx[i] - sx[i]) * e;
                py[i] = sy[i] + (ty[i] - sy[i]) * e;
            }
            send(MotionEvent.ACTION_MOVE, 0);
            if (a >= 1f) break;
            long wait = stepMs - (SystemClock.uptimeMillis() - now);
            if (wait > 0) Thread.sleep(wait);
        }
    }

    static void key(int code) throws Exception {
        long t = SystemClock.uptimeMillis();
        for (int action = 0; action < 2; action++) {
            KeyEvent ev = new KeyEvent(t, SystemClock.uptimeMillis(), action, code, 0, 0, -1, 0, 0, InputDevice.SOURCE_KEYBOARD);
            if (injectArgs == 2) inject.invoke(manager, ev, 0);
            else inject.invoke(manager, ev, 0, -1);
            Thread.sleep(30);
        }
    }

    static void send(int action, int actionId) throws Exception {
        int n = 0;
        int[] ids = new int[MAXP];
        for (int i = 0; i < MAXP; i++) if (act[i]) ids[n++] = i;
        MotionEvent.PointerProperties[] pp = new MotionEvent.PointerProperties[n];
        MotionEvent.PointerCoords[] pc = new MotionEvent.PointerCoords[n];
        int index = 0;
        for (int k = 0; k < n; k++) {
            pp[k] = new MotionEvent.PointerProperties();
            pp[k].id = ids[k];
            pp[k].toolType = MotionEvent.TOOL_TYPE_FINGER;
            pc[k] = new MotionEvent.PointerCoords();
            pc[k].x = px[ids[k]];
            pc[k].y = py[ids[k]];
            pc[k].pressure = 1f;
            pc[k].size = 1f;
            if (ids[k] == actionId) index = k;
        }
        int a = action;
        if (action == MotionEvent.ACTION_POINTER_DOWN || action == MotionEvent.ACTION_POINTER_UP) {
            a = action | (index << MotionEvent.ACTION_POINTER_INDEX_SHIFT);
        }
        long now = SystemClock.uptimeMillis();
        MotionEvent ev = MotionEvent.obtain(downTime, now, a, n, pp, pc, 0, 0, 1f, 1f, deviceId, 0, InputDevice.SOURCE_TOUCHSCREEN, 0);
        if (injectArgs == 2) inject.invoke(manager, ev, 0);
        else inject.invoke(manager, ev, 0, -1);
        ev.recycle();
    }

    static void setup() throws Exception {
        Class<?> cls;
        try {
            cls = Class.forName("android.hardware.input.InputManagerGlobal");
        } catch (Throwable t) {
            cls = Class.forName("android.hardware.input.InputManager");
        }
        manager = cls.getMethod("getInstance").invoke(null);
        for (Method m : cls.getMethods()) {
            if (!m.getName().equals("injectInputEvent")) continue;
            Class<?>[] pt = m.getParameterTypes();
            if (pt.length == 2 && pt[0] == InputEvent.class) {
                inject = m;
                injectArgs = 2;
                break;
            }
            if (pt.length == 3 && pt[0] == InputEvent.class && inject == null) {
                inject = m;
                injectArgs = 3;
            }
        }
        if (inject == null) throw new RuntimeException("injectInputEvent not found");
        deviceId = 0;
        for (int id : InputDevice.getDeviceIds()) {
            InputDevice d = InputDevice.getDevice(id);
            if (d != null && d.supportsSource(InputDevice.SOURCE_TOUCHSCREEN)) {
                deviceId = id;
                break;
            }
        }
    }
}
