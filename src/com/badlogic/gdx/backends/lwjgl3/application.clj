(ns com.badlogic.gdx.backends.lwjgl3.application
  (:require [clojure.java.io :as io])
  (:import (com.badlogic.gdx ApplicationListener
                             Gdx)
           (com.badlogic.gdx.backends.lwjgl3 Lwjgl3Application
                                             Lwjgl3ApplicationConfiguration
                                             Lwjgl3WindowConfiguration)
           (com.badlogic.gdx.utils SharedLibraryLoader
                                   Os)
           (java.awt Taskbar
                     Toolkit)
           (org.lwjgl.system Configuration)))

(def ^:private os-value->keyword
  {Os/Android :android
   Os/IOS     :ios
   Os/Linux   :linux
   Os/MacOsX  :mac
   Os/Windows :windows})

(defn- operating-system []
  (os-value->keyword SharedLibraryLoader/os))

(defn- set-taskbar-icon! [path]
  (.setIconImage (Taskbar/getTaskbar)
                 (.getImage (Toolkit/getDefaultToolkit)
                            (io/resource path))))

(defn- set-window-config-option! [^Lwjgl3WindowConfiguration object k v]
  (case k
    :windowed-mode (.setWindowedMode object (int (:width v)) (int (:height v)))

    :title (.setTitle object (str v))))

(defn- set-glfw-library-name! [str]
  (.set Configuration/GLFW_LIBRARY_NAME str))

(defn- set-mac-os-config!
  [{:keys [glfw-async?
           taskbar-icon]}]
  (when glfw-async?
    (set-glfw-library-name! "glfw_async"))
  (when-let [path taskbar-icon]
    (set-taskbar-icon! path)))

(defn- ->Lwjgl3ApplicationConfiguration [config]
  (let [obj (Lwjgl3ApplicationConfiguration.)]
    (doseq [[k v] config]
      (case k
        :foreground-fps (.setForegroundFPS obj (int v))

        :mac (when (= (operating-system) :mac) (set-mac-os-config! v))

        (set-window-config-option! obj k v)
        ))
    obj))

(defprotocol Listener
  (create [_ context])
  (dispose [_])
  (render [_])
  (resize [_ width height])
  (pause [_])
  (resume [_]))

(defn start! [{:keys [listener config]}]
  (Lwjgl3Application. (reify ApplicationListener
                        (create [_]
                          (create listener {:ctx/app      Gdx/app
                                                        :ctx/audio    Gdx/audio
                                                        :ctx/files    Gdx/files
                                                        :ctx/graphics Gdx/graphics
                                                        :ctx/input    Gdx/input}))
                        (dispose [_]
                          (dispose listener))
                        (render [_]
                          (render listener))
                        (resize [_ width height]
                          (resize listener width height))
                        (pause [_]
                          (pause listener))
                        (resume [_]
                          (resume listener)))
                      (->Lwjgl3ApplicationConfiguration config)))

(require 'com.badlogic.gdx.application
         'com.badlogic.gdx.audio
         'com.badlogic.gdx.files
         'com.badlogic.gdx.graphics
         'com.badlogic.gdx.input
         'com.badlogic.gdx.utils.disposable)
