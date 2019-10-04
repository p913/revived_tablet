package ru.revivedtablet;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.graphics.Bitmap;
import android.os.Build;
import android.os.SystemClock;
import android.util.Log;

import ru.revivedtablet.config.Configuration;
import ru.revivedtablet.config.Storages;

import fi.iki.elonen.NanoHTTPD;

public class WebServerImpl extends NanoHTTPD {
	
	private static final String PAGE_INDEX = "index.html";
	private static final String STYLE_SHEETS = "rt.css";
	private static final String PAGE_FILE_MANAGER = "filemgr.html";
	private static final String PAGE_FILE_CONFIG = "config.html";
	private static final String PAGE_FILE_LOG_VIEW = "logview.html";
	private static final String URI_EVENTS = "/events/";

    private static final Pattern SSI_VARIABLE_REGEXP = Pattern.compile("\\$\\{(\\w+)\\.?(\\w*):?([\\w]*)\\}");

	public static final Map<String, Object> vars = new HashMap<>();

	static {
		vars.put("uptime", new Uptime());
		vars.put("storages", Storages.getInstance());

		String appVersion = "";
		try {
			appVersion = RevivedTabletApp.getContext().getPackageManager().getPackageInfo(RevivedTabletApp.getContext().getPackageName(), 0).versionName;
		} catch (Exception e) {
		}
		vars.put("version", appVersion);

		String plVersion = String.format("Android %s (API %d). Product: %s, type: %s, tags: %s. Manufacturer: %s, hardware: %s, model: %s.",
				Build.VERSION.RELEASE, Build.VERSION.SDK_INT, Build.PRODUCT, Build.TYPE, Build.TAGS, Build.MANUFACTURER, Build.HARDWARE, Build.MODEL);
		vars.put("platform", plVersion);
	}
	
	public WebServerImpl() {
		super(8000);
	}

	@Override
	public Response serve(IHTTPSession session) {
		Map<String, Object> vars = new HashMap<>();
		vars.putAll(this.vars);
		if (session.getUri().endsWith(STYLE_SHEETS))
			return newFixedLengthResponse(Response.Status.OK, "text/css", prepareHtml(getHtmlFromAssets(STYLE_SHEETS), vars));
		else if (session.getMethod() == Method.GET && session.getUri().endsWith(PAGE_FILE_MANAGER) && session.getParms().containsKey("folder")) {
            File folder = new File(session.getParms().get("folder"));
            if (checkPathSafety(folder)) {
                if (session.getParms().containsKey("mkdir") && !session.getParms().get("mkdir").contains(File.pathSeparator) ) {
                    folder = new File(folder, session.getParms().get("mkdir"));
                    folder.mkdirs();
                }
                vars.put("folder", new Storages.Folder(folder));
                return newFixedLengthResponse(prepareHtml(getHtmlFromAssets(PAGE_FILE_MANAGER), vars));
            }

		} else if (session.getMethod() == Method.POST && session.getUri().endsWith(PAGE_FILE_MANAGER) && session.getParms().containsKey("folder")) {
            File folder = new File(session.getParms().get("folder"));
			Storages.Storage storage = Storages.getInstance().findStorage(folder);
			if (storage != null) {
                Map<String, String> files = new HashMap<>();
                try {
                    session.parseBody(files);
                    for (String fname : files.keySet()) {
                        if (!session.getParms().get(fname).isEmpty()) {
                            Log.d("Uploaded file", session.getParms().get(fname));
                            try {
                                File dest = new File(folder, session.getParms().get(fname).replaceAll("\\.\\w+$", ".jpeg"));
                                resizePicture(new File(files.get(fname)), dest);
                                createThumbnail(dest);
                            } catch (IOException e) {
                                Log.e("Error on receive img", e.getMessage());
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.e("Parse body error", e.getMessage(), e);
                }

				if (session.getParms().containsKey("delconfirm") && session.getParms().get("delconfirm").equals("on"))
					for (String filetodel : session.getParms().keySet())
						if (!filetodel.equals("delconfirm") && session.getParms().get(filetodel).equals("on"))
							deleteFileWithThumbnail(new File(folder, filetodel + ImageUtils.PICTURE_EXTENSION));

				storage.updateUsages();
			}
            vars.put("folder", new Storages.Folder(folder));
            return newFixedLengthResponse(prepareHtml(getHtmlFromAssets(PAGE_FILE_MANAGER), vars));

		} else if (session.getMethod() == Method.GET && session.getUri().endsWith(PAGE_FILE_CONFIG)) {
			vars.put("luacode", Configuration.getInstance().getLuaCode());
			return newFixedLengthResponse(prepareHtml(getHtmlFromAssets(PAGE_FILE_CONFIG), vars));

        } else if (session.getMethod() == Method.POST && session.getUri().endsWith(PAGE_FILE_CONFIG)) {
            try {
                session.parseBody(new HashMap<String, String>());
            } catch (Exception e) {
                Log.e("Parse body error", e.getMessage(), e);
            }
            String config = session.getParms().get("luacode");
            if (config != null) {
                vars.put("luacode", config);
                Configuration.getInstance().setLuaCode(config);
            } else
                vars.put("luacode", Configuration.getInstance().getLuaCode());
            return newFixedLengthResponse(prepareHtml(getHtmlFromAssets(PAGE_FILE_CONFIG), vars));
        } else if (session.getUri().toLowerCase().endsWith(ImageUtils.PICTURE_EXTENSION)) {
			File f = new File(session.getUri() + ImageUtils.THUMBNAIL_EXTENSION);
			if (checkPathSafety(f) && f.exists()) {
				try {
					InputStream fs = new FileInputStream(f);
					return newFixedLengthResponse(Response.Status.OK, "image/jpeg", fs, f.length());
				} catch (IOException e) {
				}
			}
			return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "");
		} else if (session.getUri().endsWith(PAGE_FILE_LOG_VIEW)) {
			vars.put("log", Configuration.getInstance().getLog());
			return newFixedLengthResponse(prepareHtml(getHtmlFromAssets(PAGE_FILE_LOG_VIEW), vars));
		} else if (session.getUri().contains(URI_EVENTS)) {
            try {
                //Необходимо для получения параметров из тела запроса для POST & x-www-form-urlencoded
                session.parseBody(new HashMap<String, String>());
            } catch (Exception e) {
                Log.e("Parse body error", e.getMessage(), e);
            }
			Configuration.getInstance().notifyNewEvent(session.getUri(), session.getMethod().name(),
                    session.getRemoteIpAddress(), session.getRemoteHostName(), session.getParms(), session.getHeaders());
			return newFixedLengthResponse(Response.Status.OK, "text/plain", "");
		}

		return newFixedLengthResponse(prepareHtml(getHtmlFromAssets(PAGE_INDEX), vars));
	}

    private void deleteFileWithThumbnail(File f) {
        if (f.delete()) {
            File th = new File(f.getAbsolutePath() + ImageUtils.THUMBNAIL_EXTENSION);
            th.delete();
        }
    }

	private File createThumbnail(File src) throws IOException {
        File f = new File(src.getAbsolutePath() + ImageUtils.THUMBNAIL_EXTENSION);
        Bitmap th = ImageUtils.decodeSampledBitmap(src.getAbsolutePath(), 100, 100, ImageUtils.PreferedBoundMode.EQUAL_OR_LESS);
        if (th != null) {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            FileOutputStream fs = new FileOutputStream(f);
            th.compress(Bitmap.CompressFormat.JPEG, 99, bytes);
            try {
                bytes.writeTo(fs);
                fs.flush();
                return f;
            } finally {
                fs.close();
            }
        }
        return f;
    }

    private void resizePicture(File src, File dest) throws IOException {
        Bitmap th = ImageUtils.decodeSampledBitmap(src.getAbsolutePath(),
                TabletCanvasUtils.getDisplayWidth(),
                TabletCanvasUtils.getDisplayHeight(),
                ImageUtils.PreferedBoundMode.EQUAL_OR_GREAT);
        if (th != null) {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            FileOutputStream fs = new FileOutputStream(dest);
            th.compress(Bitmap.CompressFormat.JPEG, 90, bytes);
            try {
                bytes.writeTo(fs);
                fs.flush();
            } finally {
                fs.close();
            }
        }
    }

	private static String getHtmlFromAssets(String name) {
		try {
            InputStream is = RevivedTabletApp.getContext().getAssets().open(name);
            
            int size = is.available();
            
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            
            return new String(buffer);
            
        } catch (IOException e) {
            return " \"" + name + "\" not found. ";
        }		
	}
	
	private static String prepareHtml(String html, Map<String, Object> vars) {
		Matcher m = SSI_VARIABLE_REGEXP.matcher(html);
		StringBuffer res = new StringBuffer();
		
		while (m.find()) {
			m.appendReplacement(res, "");
			res.append(eval(m.group(1), m.group(2), m.group(3), vars));
		}
		m.appendTail(res);
		    
		return res.toString();
	}
	
	private static String eval(String key, String prop, String reference, Map<String, Object> vars) {
		Object v = vars.get(key);
		if (v != null) {
			if (prop.isEmpty())
				return v.toString();
			else {
				java.lang.reflect.Method method;
				try {
					String methodName = "get" + prop.substring(0, 1).toUpperCase() + prop.substring(1);
					method = v.getClass().getMethod(methodName);
					Object res = method.invoke(v);
					if (!reference.isEmpty()) {
						if (res.getClass().isArray()) {
                            StringBuilder sb = new StringBuilder();
                            for (Object obj : (Object[]) res) {
                                vars.put(reference, obj);
                                sb.append(prepareHtml(getHtmlFromAssets(reference + ".inc.html"), vars));
                                vars.remove(reference);
                            }
                            return sb.toString();
                        } else if (res instanceof List) {
                                StringBuilder sb = new StringBuilder();
                                for (Object obj: (List)res) {
                                    vars.put(reference, obj);
                                    sb.append(prepareHtml(getHtmlFromAssets(reference + ".inc.html"), vars));
                                    vars.remove(reference);
                                }
                                return sb.toString();

                            } else {
							vars.put(reference, res);
							String html = prepareHtml(getHtmlFromAssets(reference + ".inc.html"), vars);
							vars.remove(reference);
							return html;
						}
					}
					return res.toString();
				}
				catch (Exception e) {
					Log.d("Prop access error: " + prop, e.toString());
				}
			}
		}

		return "";
	}

	private boolean checkPathSafety(File path) {
		return Storages.getInstance().findStorage(path) != null;
	}
	
	private static class Uptime {
		@Override
		public String toString() {
			long secUptime = SystemClock.elapsedRealtime() / 1000;
			long secIdle = secUptime - SystemClock.uptimeMillis() / 1000;
			String fmt = "%dд %d:%02d:%02d";
			return ((new StringBuilder())
					.append("Время работы: ")
					.append(String.format(fmt, secUptime / 86400, (secUptime % 86400) / 3600, (secUptime % 3600) / 60, (secUptime % 60)))
					.append(" В спячке: ")
					.append(String.format(fmt, secIdle / 86400, (secIdle % 86400) / 3600, (secIdle % 3600) / 60, (secIdle % 60)))
			).toString();
		}
	}


}

