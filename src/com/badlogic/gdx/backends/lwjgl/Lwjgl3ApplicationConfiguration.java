/*******************************************************************************
 * Copyright 2011 See AUTHORS file.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.badlogic.gdx.backends.lwjgl;

import java.io.PrintStream;
import java.nio.IntBuffer;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.LifecycleListener;
import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.glfw.GLFWVidMode.Buffer;

import com.badlogic.gdx.Audio;
import com.badlogic.gdx.Files;
import com.badlogic.gdx.Files.FileType;
import com.badlogic.gdx.Graphics.DisplayMode;
import com.badlogic.gdx.Graphics.Monitor;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.backends.lwjgl.Lwjgl3Graphics.Lwjgl3Monitor;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.HdpiMode;
import com.badlogic.gdx.graphics.glutils.HdpiUtils;
import com.badlogic.gdx.math.GridPoint2;

public class Lwjgl3ApplicationConfiguration  {

	/** @return the currently active {@link DisplayMode} of the primary monitor */
	public static DisplayMode getDisplayMode () {
		GLFWVidMode videoMode = GLFW.glfwGetVideoMode(GLFW.glfwGetPrimaryMonitor());
		return new Lwjgl3Graphics.Lwjgl3DisplayMode(GLFW.glfwGetPrimaryMonitor(), videoMode.width(), videoMode.height(),
			videoMode.refreshRate(), videoMode.redBits() + videoMode.greenBits() + videoMode.blueBits());
	}

	/** @return the currently active {@link DisplayMode} of the given monitor */
	public static DisplayMode getDisplayMode (Monitor monitor) {
		GLFWVidMode videoMode = GLFW.glfwGetVideoMode(((Lwjgl3Monitor)monitor).monitorHandle);
		return new Lwjgl3Graphics.Lwjgl3DisplayMode(((Lwjgl3Monitor)monitor).monitorHandle, videoMode.width(), videoMode.height(),
			videoMode.refreshRate(), videoMode.redBits() + videoMode.greenBits() + videoMode.blueBits());
	}

	/** @return the available {@link DisplayMode}s of the primary monitor */
	public static DisplayMode[] getDisplayModes () {
		Buffer videoModes = GLFW.glfwGetVideoModes(GLFW.glfwGetPrimaryMonitor());
		DisplayMode[] result = new DisplayMode[videoModes.limit()];
		for (int i = 0; i < result.length; i++) {
			GLFWVidMode videoMode = videoModes.get(i);
			result[i] = new Lwjgl3Graphics.Lwjgl3DisplayMode(GLFW.glfwGetPrimaryMonitor(), videoMode.width(), videoMode.height(),
				videoMode.refreshRate(), videoMode.redBits() + videoMode.greenBits() + videoMode.blueBits());
		}
		return result;
	}

	/** @return the available {@link DisplayMode}s of the given {@link Monitor} */
	public static DisplayMode[] getDisplayModes (Monitor monitor) {
		Buffer videoModes = GLFW.glfwGetVideoModes(((Lwjgl3Monitor)monitor).monitorHandle);
		DisplayMode[] result = new DisplayMode[videoModes.limit()];
		for (int i = 0; i < result.length; i++) {
			GLFWVidMode videoMode = videoModes.get(i);
			result[i] = new Lwjgl3Graphics.Lwjgl3DisplayMode(((Lwjgl3Monitor)monitor).monitorHandle, videoMode.width(),
				videoMode.height(), videoMode.refreshRate(), videoMode.redBits() + videoMode.greenBits() + videoMode.blueBits());
		}
		return result;
	}

	/** @return the primary {@link Monitor} */
	public static Monitor getPrimaryMonitor () {
		return toLwjgl3Monitor(GLFW.glfwGetPrimaryMonitor());
	}

	/** @return the connected {@link Monitor}s */
	public static Monitor[] getMonitors () {
		PointerBuffer glfwMonitors = GLFW.glfwGetMonitors();
		Monitor[] monitors = new Monitor[glfwMonitors.limit()];
		for (int i = 0; i < glfwMonitors.limit(); i++) {
			monitors[i] = toLwjgl3Monitor(glfwMonitors.get(i));
		}
		return monitors;
	}

	public static Lwjgl3Monitor toLwjgl3Monitor (long glfwMonitor) {
		IntBuffer tmp = BufferUtils.createIntBuffer(1);
		IntBuffer tmp2 = BufferUtils.createIntBuffer(1);
		GLFW.glfwGetMonitorPos(glfwMonitor, tmp, tmp2);
		int virtualX = tmp.get(0);
		int virtualY = tmp2.get(0);
		String name = GLFW.glfwGetMonitorName(glfwMonitor);
		return new Lwjgl3Monitor(glfwMonitor, virtualX, virtualY, name);
	}

	public static GridPoint2 calculateCenteredWindowPosition (Lwjgl3Monitor monitor, int newWidth, int newHeight) {
		IntBuffer tmp = BufferUtils.createIntBuffer(1);
		IntBuffer tmp2 = BufferUtils.createIntBuffer(1);
		IntBuffer tmp3 = BufferUtils.createIntBuffer(1);
		IntBuffer tmp4 = BufferUtils.createIntBuffer(1);

		DisplayMode displayMode = getDisplayMode(monitor);

		GLFW.glfwGetMonitorWorkarea(monitor.monitorHandle, tmp, tmp2, tmp3, tmp4);
		int workareaWidth = tmp3.get(0);
		int workareaHeight = tmp4.get(0);

		int minX, minY, maxX, maxY;

		// If the new width is greater than the working area, we have to ignore stuff like the taskbar for centering and use the
		// whole monitor's size
		if (newWidth > workareaWidth) {
			minX = monitor.virtualX;
			maxX = displayMode.width;
		} else {
			minX = tmp.get(0);
			maxX = workareaWidth;
		}
		// The same is true for height
		if (newHeight > workareaHeight) {
			minY = monitor.virtualY;
			maxY = displayMode.height;
		} else {
			minY = tmp2.get(0);
			maxY = workareaHeight;
		}

		return new GridPoint2(Math.max(minX, minX + (maxX - newWidth) / 2), Math.max(minY, minY + (maxY - newHeight) / 2));
	}
}
