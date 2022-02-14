package ru.revivedtablet.config;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.TwoArgFunction;

import java.util.Iterator;

public class JsonLib extends TwoArgFunction {
    @Override
    public LuaValue call(LuaValue modname, LuaValue env) {
        LuaValue library = tableOf();
        library.set("parse", new ParseMethod());
        library.set("build", new BuildMethod());
        env.set("json", library);
        return library;
    }

    /**
     * Разобрать строку, содержащую Json, и преобразовать ее в таблицу
     */
    static class ParseMethod extends OneArgFunction {

        @Override
        public LuaValue call(LuaValue arg) {
            LuaTable res = LuaTable.tableOf();
            try {
                Object parsed = new JSONTokener(arg.tojstring()).nextValue();
                if (parsed instanceof JSONObject)
                    addObjectToTable(res, (JSONObject)parsed);
            } catch (JSONException e) {
                Log.e("Json parse", e.getMessage());
                return null;
            }
            return res;
        }

        private void addObjectToTable(LuaTable table, JSONObject obj) throws JSONException {
            Iterator<String> keys = obj.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                Object value = obj.get(key);
                if (value instanceof JSONObject) {
                    LuaTable newTable = LuaTable.tableOf();
                    table.set(key, newTable);
                    addObjectToTable(newTable, (JSONObject) value);
                } else if (value instanceof JSONArray) {
                    LuaTable newTable = LuaTable.tableOf();
                    table.set(key, newTable);
                    addObjectToTable(newTable, (JSONArray) value);
                } else if (value instanceof String) {
                    table.set(key, (String) value);
                } else if (value instanceof Boolean) {
                    table.set(key, LuaValue.valueOf((Boolean) value));
                } else if (value instanceof Integer) {
                    table.set(key, (Integer) value);
                } else if (value instanceof Long) {
                    table.set(key, (Long) value);
                } else if (value instanceof Double) {
                    table.set(key, (Double) value);
                } else if (JSONObject.NULL.equals(value)) {
                    table.set(key, LuaValue.NIL);
                }
            }
        }

        private void addObjectToTable(LuaTable table, JSONArray arr) throws JSONException {
            for (int i = 0; i < arr.length(); i++) {
                Object value = arr.get(i);
                if (value instanceof JSONObject) {
                    LuaTable newTable = LuaTable.tableOf();
                    table.set(i, newTable);
                    addObjectToTable(newTable, (JSONObject) value);
                } else if (value instanceof JSONArray) {
                    LuaTable newTable = LuaTable.tableOf();
                    table.set(i, newTable);
                    addObjectToTable(newTable, (JSONArray) value);
                } else if (value instanceof String) {
                    table.set(i, (String) value);
                } else if (value instanceof Boolean) {
                    table.set(i, LuaValue.valueOf((Boolean) value));
                } else if (value instanceof Integer) {
                    table.set(i, LuaValue.valueOf((Integer) value));
                } else if (value instanceof Long) {
                    table.set(i, LuaValue.valueOf((Long) value));
                } else if (value instanceof Double) {
                    table.set(i, LuaValue.valueOf((Double) value));
                } else if (JSONObject.NULL.equals(value)) {
                    table.set(i, LuaValue.NIL);
                }
            }
        }
    }

    /**
     * Преобразовать таблицу в строку Json
     */
    static class BuildMethod extends OneArgFunction {

        @Override
        public LuaValue call(LuaValue arg) {
            try {
                if (arg.istable())
                    return LuaValue.valueOf(toJsonObject(arg).toString());
                else
                    throw new JSONException("Only Lua tables supported in json.build(table)");
            } catch (JSONException e) {
                e.printStackTrace();
            }

            return LuaValue.NIL;
        }

        private Object toJsonObject(LuaValue value) throws JSONException {
            if (value == null || value.isnil()) {
                return JSONObject.NULL;
            } else if (value.istable()) {
                //is array?
                boolean allIndexesInt = true;
                LuaTable table = value.checktable();
                for (LuaValue key : table.keys())
                    if (!key.isinttype()) {
                        allIndexesInt = false;
                        break;
                    }
                if (allIndexesInt) {
                    JSONArray arr = new JSONArray();
                    for (LuaValue key : table.keys())
                        arr.put(toJsonObject(table.get(key)));
                    return arr;
                } else {
                    JSONObject obj = new JSONObject();
                    for (LuaValue key : table.keys())
                        obj.put(key.tojstring(), toJsonObject(table.get(key)));
                    return obj;
                }
            } else if (value.isstring()) {
                return value.tojstring();
            } else if (value.isint()) {
                return value.checkint();
            } else if (value.islong()) {
                return value.checklong();
            } else if (value.isboolean()) {
                return value.checkboolean();
            } else if (value.isnil()) {
                return value.tojstring();
            }
            return JSONObject.NULL;
        }
    }
}
