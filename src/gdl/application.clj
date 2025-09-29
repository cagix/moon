(ns gdl.application
  (:require clojure.audio
            clojure.audio.sound
            clojure.disposable
            clojure.files
            clojure.files.file-handle)
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

(defn start! [config]
  (.set Configuration/GLFW_LIBRARY_NAME "glfw_async")
  (Lwjgl3Application. (reify ApplicationListener
                        (create [_]
                          ((:create config) {:ctx/audio    Gdx/audio
                                             :ctx/files    Gdx/files
                                             :ctx/graphics Gdx/graphics
                                             :ctx/input    Gdx/input}))
                        (dispose [_]
                          ((:dispose config)))
                        (render [_]
                          ((:render config)))
                        (resize [_ width height]
                          ((:resize config) width height))
                        (pause [_])
                        (resume [_]))
                      (doto (Lwjgl3ApplicationConfiguration.)
                        (.setWindowedMode (:width (:windowed-mode config))
                                          (:height (:windowed-mode config)))
                        (.setTitle (:title config))
                        (.setForegroundFPS (:foreground-fps config)))))

(defn post-runnable! [f]
  (.postRunnable Gdx/app f))

(extend-type Audio
  clojure.audio/Audio
  (new-sound [this file-handle]
    (.newSound this file-handle)))

(extend-type Sound
  clojure.audio.sound/Sound
  (play! [this]
    (.play this)))

(extend-type Files
  clojure.files/Files
  (internal [this path]
    (.internal this path)))

(extend-type FileHandle
  clojure.files.file-handle/FileHandle
  (list [this]
    (.list this))

  (directory? [this]
    (.isDirectory this))

  (extension [this]
    (.extension this))

  (path [this]
    (.path this)))

(extend-type Disposable
  clojure.disposable/Disposable
  (dispose! [this]
    (.dispose this)))
