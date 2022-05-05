package org.dolphinemu.dolphinemu.utils;

import java.io.File;

import android.util.Log;
import android.content.Context;
import android.os.Environment;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;

import org.dolphinemu.dolphinemu.R;
import org.dolphinemu.dolphinemu.features.settings.model.Settings;
import org.dolphinemu.dolphinemu.features.settings.utils.SettingsFile;
import org.dolphinemu.dolphinemu.model.UpdaterData;
import org.dolphinemu.dolphinemu.dialogs.UpdaterDialog;
import org.dolphinemu.dolphinemu.features.settings.model.BooleanSetting;

public class UpdaterUtils
{
	public static final String URL = "https://api.github.com/repos/Bankaimaster999/Dolphin-MMJR/releases";
	public static final String LATEST = "/latest";

	public static void openUpdaterWindow(Context context, UpdaterData data)
	{
		FragmentManager fm = ((FragmentActivity) context).getSupportFragmentManager();
		UpdaterDialog updaterDialog = UpdaterDialog.newInstance(data);
		updaterDialog.show(fm, "fragment_updater");
	}

	public static void checkUpdatesInit(Context context)
	{
		if (DirectoryInitialization.isReady())
		{
			Settings settings = new Settings();
			settings.loadSettings(null);

			cleanDownloadFolder(context);

			BooleanSetting checkUpdatesSetting =
				(BooleanSetting) settings.getSection(Settings.SECTION_INI_INTERFACE)
					.getSetting(SettingsFile.KEY_CHECK_UPDATES);
			boolean checkUpdates = checkUpdatesSetting == null || checkUpdatesSetting.getValue();
			if (checkUpdates)
			{
				checkUpdates(context);
			}
		}
	}

	private static void checkUpdates(Context context)
	{
		makeDataRequest(new LoadCallback<UpdaterData>()
		{
			@Override
			public void onLoad(UpdaterData data)
			{
				VersionCode version = getBuildVersion();
				if (version.compareTo(data.version) < 0)
				{
					showUpdateMessage(context, data);
				}
			}

			@Override
			public void onLoadError() {}
		});
	}

	private static void showUpdateMessage(Context context, UpdaterData data)
	{
		new AlertDialog.Builder(context)
			.setTitle(context.getString(R.string.updates_alert))
			.setMessage(context.getString(R.string.updater_alert_body))
			.setPositiveButton(android.R.string.yes, (dialogInterface, i) ->
				openUpdaterWindow(context, data))
			.setNeutralButton(android.R.string.no,
				((dialogInterface, i) -> dialogInterface.dismiss()))
			.show();
	}

	public static void makeDataRequest(LoadCallback<UpdaterData> listener)
	{
		JsonObjectRequest jsonRequest = new JsonObjectRequest(Request.Method.GET, URL + LATEST, null,
			response ->
			{
				try
				{
					UpdaterData data = new UpdaterData(response);
					listener.onLoad(data);
				}
				catch (Exception e)
				{
					Log.e(UpdaterUtils.class.getSimpleName(), e.toString());
					listener.onLoadError();
				}
			},
			error -> listener.onLoadError());
		VolleyUtil.getQueue().add(jsonRequest);
	}

	public static void makeChangelogRequest(String format, LoadCallback<String> listener)
	{
		JsonArrayRequest jsonRequest = new JsonArrayRequest(Request.Method.GET, URL, null,
			response ->
			{
				try
				{
					StringBuilder changelog = new StringBuilder();

					for (int i = 0; i < response.length(); i++)
					{
						changelog.append(String.format(format,
							response.getJSONObject(i).getString("tag_name"),
							response.getJSONObject(i).getString("published_at").substring(0, 10),
							response.getJSONObject(i).getString("body")));
					}
					changelog.setLength(Math.max(changelog.length() - 1, 0));
					listener.onLoad(changelog.toString());
				}
				catch (Exception e)
				{
					Log.e(UpdaterUtils.class.getSimpleName(), e.toString());
					listener.onLoadError();
				}
			},
			error -> listener.onLoadError());
		VolleyUtil.getQueue().add(jsonRequest);
	}

	public static void cleanDownloadFolder(Context context)
	{
		File[] files = getDownloadFolder(context).listFiles();
		if (files != null)
		{
			for (File file : files)
				file.delete();
		}
	}

	public static File getDownloadFolder(Context context)
	{
		return context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
	}

	/**
	 * This function must never fail, versionName scheme in build.gradle must be correct!
	 */
	public static VersionCode getBuildVersion()
	{
		return VersionCode.create("1.0-11500");
	}
}
