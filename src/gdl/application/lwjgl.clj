(ns gdl.application.lwjgl
  (:require gdl.audio
            gdl.audio.sound
            gdl.files
            gdl.files.file-handle
            gdl.disposable)
  (:import (com.badlogic.gdx ApplicationListener
                             Audio
                             Files
                             Gdx)
           (com.badlogic.gdx.audio Sound)
           (com.badlogic.gdx.backends.lwjgl3 Lwjgl3Application
                                             Lwjgl3ApplicationConfiguration)
           (com.badlogic.gdx.files FileHandle)
           (com.badlogic.gdx.utils Disposable)
           (org.lwjgl.system Configuration)))

(defprotocol Listener
  (create [_ context])
  (dispose [_])
  (render [_])
  (resize [_ width height])
  (pause [_])
  (resume [_]))

(defn start! [listener config]
  (.set Configuration/GLFW_LIBRARY_NAME "glfw_async")
  (Lwjgl3Application. (reify ApplicationListener
                        (create [_]
                          (let [state {:ctx/audio    Gdx/audio
                                       :ctx/files    Gdx/files
                                       :ctx/graphics Gdx/graphics
                                       :ctx/input    Gdx/input}]
                            (create listener state)))
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
                      (doto (Lwjgl3ApplicationConfiguration.)
                        (.setWindowedMode (:width (:windowed-mode config))
                                          (:height (:windowed-mode config)))
                        (.setTitle (:title config))
                        (.setForegroundFPS (:foreground-fps config)))))


(extend-type Audio
  gdl.audio/Audio
  (new-sound [this file-handle]
    (.newSound this file-handle)))

(extend-type Sound
  gdl.audio.sound/Sound
  (play! [this]
    (.play this)))

(extend-type Files
  gdl.files/Files
  (internal [this path]
    (.internal this path)))

(extend FileHandle
  gdl.files.file-handle/FileHandle
  {:list       FileHandle/.list
   :directory? FileHandle/.isDirectory
   :extension  FileHandle/.extension
   :path       FileHandle/.path})

(extend Disposable
  gdl.disposable/Disposable
  {:dispose! Disposable/.dispose})
