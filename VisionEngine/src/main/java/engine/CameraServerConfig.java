package engine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import edu.wpi.cscore.MjpegServer;
import edu.wpi.cscore.UsbCamera;
import edu.wpi.cscore.VideoSource;
import edu.wpi.first.cameraserver.CameraServer;
import edu.wpi.first.networktables.EntryListenerFlags;
import edu.wpi.first.networktables.NetworkTableInstance;

public class CameraServerConfig {

	/**
	 * Configuration file path
	 */
	public static String configFile = "/boot/frc.json";

	/**
	 * Named settings for camera configuration
	 */
	@SuppressWarnings("MemberName")
	public static class CameraConfig {
		public String name;
		public String path;
		public JsonObject config;
		public JsonElement streamConfig;
	}

	/**
	 * Named settings for switched camera configuration
	 */
	@SuppressWarnings("MemberName")
	public static class SwitchedCameraConfig {
		public String name;
		public String key;
	};

	/**
	 * Team number
	 */
	public static int team;

	/**
	 * Flag used to determine if program needs to run network tables server
	 * or connect to a robot server
	 */
	public static boolean server;

	/**
	 * Camera configuration files
	 */
	public static List<CameraConfig> cameraConfigs = new ArrayList<>();

	/**
	 * Switched camera configuration files
	 */
	public static List<SwitchedCameraConfig> switchedCameraConfigs = new ArrayList<>();

	/**
	 * Camera video sources
	 */
	public static List<VideoSource> cameras = new ArrayList<>();

	/**
	 * Report parse error.
	 */
	public static void parseError(String str) {
		System.err.println("config error in '" + configFile + "': " + str);
	}

	/**
	 * Read single camera configuration.
	 */
	public static boolean readCameraConfig(JsonObject config) {
		CameraConfig cam = new CameraConfig();

		// name
		JsonElement nameElement = config.get("name");
		if (nameElement == null) {
			parseError("could not read camera name");
			return false;
		}
		cam.name = nameElement.getAsString();

		// path
		JsonElement pathElement = config.get("path");
		if (pathElement == null) {
			parseError("camera '" + cam.name + "': could not read path");
			return false;
		}
		cam.path = pathElement.getAsString();

		// stream properties
		cam.streamConfig = config.get("stream");

		cam.config = config;

		cameraConfigs.add(cam);
		return true;
	}

	/**
	 * Read single switched camera configuration.
	 */
	public static boolean readSwitchedCameraConfig(JsonObject config) {
		SwitchedCameraConfig cam = new SwitchedCameraConfig();

		// name
		JsonElement nameElement = config.get("name");
		if (nameElement == null) {
			parseError("could not read switched camera name");
			return false;
		}
		cam.name = nameElement.getAsString();

		// path
		JsonElement keyElement = config.get("key");
		if (keyElement == null) {
			parseError("switched camera '" + cam.name + "': could not read key");
			return false;
		}
		cam.key = keyElement.getAsString();

		switchedCameraConfigs.add(cam);
		return true;
	}

	/**
	 * Read configuration file.
	 */
	@SuppressWarnings("PMD.CyclomaticComplexity")
	public static boolean readConfig() {
		// parse file
		JsonElement top;
		try {
			top = new JsonParser().parse(Files.newBufferedReader(Paths.get(configFile)));
		} catch (IOException ex) {
			System.err.println("could not open '" + configFile + "': " + ex);
			return false;
		}

		// top level must be an object
		if (!top.isJsonObject()) {
			parseError("must be JSON object");
			return false;
		}
		JsonObject obj = top.getAsJsonObject();

		// team number
		JsonElement teamElement = obj.get("team");
		if (teamElement == null) {
			parseError("could not read team number");
			return false;
		}
		team = teamElement.getAsInt();

		// ntmode (optional)
		if (obj.has("ntmode")) {
			String str = obj.get("ntmode").getAsString();
			if ("client".equalsIgnoreCase(str)) {
				server = false;
			} else if ("server".equalsIgnoreCase(str)) {
				server = true;
			} else {
				parseError("could not understand ntmode value '" + str + "'");
			}
		}

		// cameras
		JsonElement camerasElement = obj.get("cameras");
		if (camerasElement == null) {
			parseError("could not read cameras");
			return false;
		}
		JsonArray cameras = camerasElement.getAsJsonArray();
		for (JsonElement camera : cameras) {
			if (!readCameraConfig(camera.getAsJsonObject())) {
				return false;
			}
		}

		if (obj.has("switched cameras")) {
			JsonArray switchedCameras = obj.get("switched cameras").getAsJsonArray();
			for (JsonElement camera : switchedCameras) {
				if (!readSwitchedCameraConfig(camera.getAsJsonObject())) {
					return false;
				}
			}
		}

		return true;
	}

	/**
	 * Start running the camera.
	 */
	public static VideoSource startCamera(CameraConfig config) {
		System.out.println("Starting camera '" + config.name + "' on " + config.path);
		CameraServer inst = CameraServer.getInstance();
		UsbCamera camera = new UsbCamera(config.name, config.path);
		MjpegServer server = inst.startAutomaticCapture(camera);

		Gson gson = new GsonBuilder().create();

		camera.setConfigJson(gson.toJson(config.config));
		camera.setConnectionStrategy(VideoSource.ConnectionStrategy.kKeepOpen);

		if (config.streamConfig != null) {
			server.setConfigJson(gson.toJson(config.streamConfig));
		}

		return camera;
	}

	/**
	 * Start running the switched camera.
	 */
	public static MjpegServer startSwitchedCamera(SwitchedCameraConfig config) {
		System.out.println("Starting switched camera '" + config.name + "' on " + config.key);
		MjpegServer server = CameraServer.getInstance().addSwitchedCamera(config.name);

		NetworkTableInstance.getDefault().getEntry(config.key).addListener(event -> {
			if (event.value.isDouble()) {
				int i = (int) event.value.getDouble();
				if (i >= 0 && i < cameras.size()) {
					server.setSource(cameras.get(i));
				}
			} else if (event.value.isString()) {
				String str = event.value.getString();
				for (int i = 0; i < cameraConfigs.size(); i++) {
					if (str.equals(cameraConfigs.get(i).name)) {
						server.setSource(cameras.get(i));
						break;
					}
				}
			}
		}, EntryListenerFlags.kImmediate | EntryListenerFlags.kNew | EntryListenerFlags.kUpdate);

		return server;
	}
	
}
