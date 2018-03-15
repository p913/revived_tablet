package ru.revivedtablet.config;


import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import ru.revivedtablet.RevivedTabletApp;
import ru.revivedtablet.weather.PogodaComSource;
import ru.revivedtablet.weather.TerraMeteoSource;
import ru.revivedtablet.widget.BatteryWidget;
import ru.revivedtablet.widget.ClockWidget;
import ru.revivedtablet.widget.PlacedInLineWidget;
import ru.revivedtablet.widget.PressureGraphWidget;
import ru.revivedtablet.widget.ValueWithTitleWidget;
import ru.revivedtablet.widget.WeatherWidget;
import ru.revivedtablet.widget.Widget;

import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaFunction;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.VarArgFunction;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;
import org.luaj.vm2.lib.jse.JsePlatform;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

public class Configuration {
    private static final String LUA_CONFIG_FILE = "config.lua";

    private static final int MSG_LOAD_CONFIG = 1000;
    private static final int MSG_SAVE_CONFIG = 1001;
    private static final int MSG_PROCESS_LUA_CODE = 1002;

    private static final int MAX_LOG_COUNT = 100;

    private static final int INITIAL_TASK_START_DELAY = 3000;

    private volatile List<Widget> widgets = new ArrayList<>();

    private List<TaskWrapper> tasks = new ArrayList<>();

    private List<ConfigurationChangeListener> changeListeners = new Vector<>();

    private Globals globals;
    private volatile String luaCode = "";

    private HandlerThread luaExecThread;
    private Handler handler;

    private boolean firstLoad = false;
    private boolean paused = true;

    private final List<String> log  = new LinkedList<>();
    private SimpleDateFormat fmtLogDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    private static final Configuration instance = new Configuration();

    private Handler.Callback callback = new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_LOAD_CONFIG:
                    luaCode = loadLuaCode();

                    Message msg2 = new Message();
                    msg2.what = MSG_PROCESS_LUA_CODE;
                    msg2.obj = luaCode;
                    handler.sendMessage(msg2);

                    break;
                case MSG_SAVE_CONFIG:
                    saveLuaCode((String)msg.obj);
                    break;
                case MSG_PROCESS_LUA_CODE:
                    processLuaCode((String)msg.obj);
                    break;
            }

            return false;
        }
    };

    protected Configuration() {
        luaExecThread = new HandlerThread("Configuration script running thread") {
            @Override
            protected void onLooperPrepared() {
                globals = JsePlatform.standardGlobals();
                globals.set("widgetsFactory", CoerceJavaToLua.coerce(new WidgetsFactory()));
                globals.set("log", new LuaToLogBridge());
            }
        };
        luaExecThread.start();

        handler = new Handler(luaExecThread.getLooper(), callback);
    }

    private String loadLuaCode() {
        File f = new File(RevivedTabletApp.getContext().getFilesDir(), LUA_CONFIG_FILE);
        if (f.exists()) {
            StringBuilder contentBuilder = new StringBuilder();
            BufferedReader br = null;
            try {
                br = new BufferedReader(new FileReader(f));
                String sCurrentLine;
                while ((sCurrentLine = br.readLine()) != null) {
                    contentBuilder.append(sCurrentLine).append("\n");
                }
                return contentBuilder.toString();
            } catch (IOException e) {
                Log.e("Load lua config error", e.getMessage());
            } finally {
                if (br != null)
                    try {
                        br.close();
                    } catch (IOException e) {
                    }
            }
        }

        return "";
    }

    private void saveLuaCode(String code) {
        File f = new File(RevivedTabletApp.getContext().getFilesDir(), LUA_CONFIG_FILE);
        try {
            FileOutputStream fos = new FileOutputStream(f);
            fos.write(code.getBytes());
        } catch (IOException e) {
            Log.e("Save lua config error", e.getMessage());
        }
    }

    private void processLuaCode(String code) {
        log("Выполняется Lua-скрипт конфигурации");

        List<Widget> tempWidgets = new ArrayList<>();
        List<Widget> empty = new ArrayList<>();

        LuaValue sms = globals.get("sms").istable() ? globals.get("sms").checktable() : LuaValue.NIL;
        if (!sms.isnil()) {
            sms.set("receiver", LuaValue.NIL);
        }
        for (TaskWrapper t: this.tasks) {
            handler.removeCallbacks(t);
        }
        this.tasks.clear();
        notifyConfigurationChange(this.widgets, empty);
        this.widgets = empty;

        try {
            LuaValue chunk = globals.load(code);
            LuaTable cfg = chunk.call().checktable();
            if (!cfg.get("widgets").isnil()) {
                LuaTable widgets = cfg.get("widgets").checktable();

                for (LuaValue v: widgets.keys()) {
                    try {
                        LuaValue vv = widgets.get(v);
                        if (vv.isuserdata()) {
                            Widget w = (Widget) vv.checkuserdata(Widget.class);
                            tempWidgets.add(w);
                        }
                    } catch (LuaError e) {
                        log("Ошибка при создании виджета", e);
                        Log.e("Lua new widget error", e.getMessage());
                    }
                }
                //Сортируем виджеты в порядке создания, т.к. порядок индексов в таблице widgets не соответсвует порядку, как они добавлялись в скрипте
                Collections.sort(tempWidgets, new Comparator<Widget>() {
                    @Override
                    public int compare(Widget o1, Widget o2) {
                        long c1 = (o1 instanceof PlacedInLineWidget) ? ((PlacedInLineWidget) o1).getCreationOrder() : 0;
                        long c2 = (o2 instanceof PlacedInLineWidget) ? ((PlacedInLineWidget) o2).getCreationOrder() : 0;
                        return (int)(c1 - c2);
                    }
                });
            } else
                log("Нет таблицы виджетов widgets={...}");

            if (!cfg.get("tasks").isnil()) {
                LuaTable tasks = cfg.get("tasks").checktable();

                for (LuaValue v : tasks.keys()) {
                    if (v.isint() && tasks.get(v).isfunction()) {
                        log(String.format("Создано задание с интервалом %d сек", v.checkint()));
                        this.tasks.add(new TaskWrapper(v.checkint(), tasks.get(v).checkfunction()));
                    }
                }
            } else
                log("Нет таблицы планировщика задач tasks={...}");

            this.widgets = tempWidgets;
            notifyConfigurationChange(empty, this.widgets);

            if (!paused)
                for (TaskWrapper t: this.tasks) {
                    handler.postDelayed(t, INITIAL_TASK_START_DELAY);
                }
        } catch (LuaError e) {
            log("Ошибка при выполнении Lua-скрипта", e);
            Log.e("Lua exec error", e.getMessage());
        }
    }

    public static Configuration getInstance() {
        return instance;
    }

    public List<Widget> getWidgets() {
        return widgets;
    }

    public void setLuaCode(String code) {
        luaCode = code;

        Message msg = new Message();
        msg.what = MSG_SAVE_CONFIG;
        msg.obj = code;
        handler.sendMessageAtFrontOfQueue(msg);

        Message msg2 = new Message();
        msg2.what = MSG_PROCESS_LUA_CODE;
        msg2.obj = code;
        handler.sendMessageAtFrontOfQueue(msg2);
    }

    public String getLuaCode() {
        return luaCode;
    }

    public void pause() {
        paused = true;
        for (TaskWrapper t: this.tasks) {
            handler.removeCallbacks(t);
        }
    }

    public void resume() {
        paused = false;
        if (!firstLoad) {
            firstLoad = true;
            handler.sendEmptyMessage(MSG_LOAD_CONFIG);
        } else {
            for (TaskWrapper t : this.tasks) {
                handler.postDelayed(t, INITIAL_TASK_START_DELAY);
            }
        }
    }

    private void log(String msg) {
        synchronized (log) {
            while (log.size() >= MAX_LOG_COUNT)
                log.remove(0);
            log.add(String.format("[%s] %s<br>", fmtLogDate.format(new Date()), msg));
        }
    }

    private void log(String msg, Exception e) {
        StringBuilder sb = new StringBuilder();
        synchronized (log) {
            while (log.size() >= MAX_LOG_COUNT)
                log.remove(0);
            sb.append("[")
                    .append(fmtLogDate.format(new Date()))
                    .append("] ")
                    .append(msg)
                    .append(". ")
                    .append(e.getMessage())
                    .append(" <pre>");
            for (StackTraceElement s: e.getStackTrace())
                sb.append(s.toString()).append("<br>");
            sb.append("</pre>");
            log.add(sb.toString());
        }
    }

    public String getLog() {
        StringBuilder sb = new StringBuilder();
        synchronized (log) {
            for (String d: log) {
                sb.append(d);
            }
        }
        return sb.toString();
    }

    public interface ConfigurationChangeListener {
        void onChange(List<Widget> old, List<Widget> neww);
    }

    public void addChangeListener(ConfigurationChangeListener l) {
        changeListeners.add(l);
        l.onChange(new ArrayList<Widget>(), widgets);
    }

    public void removeChangeListener(ConfigurationChangeListener l) {
        changeListeners.remove(l);
    }

    private void notifyConfigurationChange(List<Widget> old, List<Widget> neww) {
        for (ConfigurationChangeListener l: changeListeners)
            l.onChange(old, neww);
    }

    public void notifySmsReceived(final String phoneNumber, final String text) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                LuaValue sms = globals.get("sms").istable() ? globals.get("sms").checktable() : LuaValue.NIL;
                if (!sms.isnil()) {
                    LuaValue receiver =
                            sms.get("receiver").isfunction() ? sms.get("receiver").checkfunction() : LuaValue.NIL;
                    if (!receiver.isnil())
                        receiver.call(LuaValue.valueOf(phoneNumber), LuaValue.valueOf(text));
                }
            }
        });
    }

    private class WidgetsFactory {
        public Widget create(String type, String param1, String param2, String param3, String param4) {
            if (type.equals("battery")) {
                log("Создан виджет состояния питания");
                return new BatteryWidget();
            } else if (type.equals("clock")) {
                log("Создан виджет часов");
                return new ClockWidget();
            } else if (type.equals("terrameteo") && param1 != null && !param1.isEmpty()) {
                log("Создан виджет прогноза погоды, источник: проект ТерраМетео");
                return new WeatherWidget(new TerraMeteoSource(param1));
            } else if (type.equals("pogoda.com") && param1 != null && !param1.isEmpty() && param2 != null && !param2.isEmpty() ) {
                log("Создан виджет прогноза погоды, источник: pogoda.com");
                return new WeatherWidget(new PogodaComSource(param1, param2));
            } else if (type.equals("inline") && param1 != null && !param1.isEmpty()) {
                int line = 0;
                try {
                    line = Integer.parseInt(param2 == null ? "0" : param2);
                } catch (NumberFormatException ex) {
                }
                log(String.format("Создан виджет '%s' на линии %d", param1, line));
                return new ValueWithTitleWidget(line, param1, "???");
            } else if (type.equals("pressure-inline") && param1 != null && !param1.isEmpty()) {
                int line = 0;
                try {
                    line = Integer.parseInt(param2 == null ? "0" : param1);
                } catch (NumberFormatException ex) {
                }
                log(String.format("Создан виджет графика давления на линии %d", line));
                return new PressureGraphWidget(line, param1);
            }

            return null;
        }

    }

    private class TaskWrapper implements Runnable {
        private int interval;

        private LuaFunction function;

        public TaskWrapper(int interval, LuaFunction func) {
            this.interval = interval;
            this.function = func;
        }

        @Override
        public void run() {
            try {
                function.call();
            } catch (LuaError e) {
                log(String.format("Ошибка при выполнении задания с интервалом %d сек", interval), e);
                Log.e("Lua fail task function", e.getMessage());
            }
            handler.postDelayed(this, interval * 1000);
        }
    }

    public class LuaToLogBridge extends VarArgFunction {

        @Override
        public LuaValue invoke(Varargs args) {
            StringBuilder sb = new StringBuilder();
            logLuaValue(sb, args);
            log(sb.toString());
            return NIL;
        }

        private void logLuaValue(StringBuilder sb, Varargs values) {
            for (int i = 1; i <= values.narg(); i++) {
                LuaValue value = values.arg(i);
                if (value.istable()) {
                    sb.append("{");
                    LuaTable table = value.checktable();
                    for (LuaValue v : table.keys()) {
                        logLuaValue(sb.append(v.tojstring()).append("="), table.get(v));
                    }
                    sb.append("}");
                } else
                    sb.append(value.tojstring());

                sb.append(" ");
            }
        }

    }



}
