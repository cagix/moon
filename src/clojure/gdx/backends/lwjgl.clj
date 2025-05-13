(ns clojure.gdx.backends.lwjgl
  (:require [clojure.gdx :as gdx]
            clojure.gdx.app
            clojure.gdx.files
            clojure.gdx.graphics
            [clojure.gdx.interop :as interop]
            clojure.gdx.input
            [clojure.java.io :as io]
            [clojure.utils :refer [bind-root]])
  (:import (com.badlogic.gdx Application
                             ApplicationAdapter
                             Files
                             Gdx
                             Graphics
                             Input)
           (com.badlogic.gdx.backends.lwjgl3 Lwjgl3Application
                                             Lwjgl3ApplicationConfiguration)
           (com.badlogic.gdx.utils SharedLibraryLoader
                                   Os)
           (java.awt Taskbar
                     Toolkit)
           (org.lwjgl.system Configuration)))

(defn- ->application [^Application application]
  (reify clojure.gdx.app/Application
    (post-runnable!* [_ runnable]
      (.postRunnable application runnable))))

(defn- ->files [^Files files]
  (reify clojure.gdx.files/Files
    (internal [_ path]
      (.internal files path))))

(defn- ->graphics [^Graphics graphics]
  (reify clojure.gdx.graphics/Graphics
    (delta-time [_]
      (.getDeltaTime graphics))
    (cursor [_ pixmap hotspot-x hotspot-y]
      (.newCursor graphics pixmap hotspot-x hotspot-y))
    (set-cursor! [_ cursor]
      (.setCursor graphics cursor))))

(defn- ->input [^Input input]
  (reify clojure.gdx.input/Input
    (x [_]
      (.getX input))
    (y [_]
      (.getY input))
    (button-just-pressed? [_ button]
      (.isButtonJustPressed input (interop/k->input-button button)))
    (key-just-pressed? [_ key]
      (.isKeyJustPressed input (interop/k->input-key key)))
    (key-pressed? [_ key]
      (.isKeyPressed input (interop/k->input-key key)))))

(defn- set-context! []
  (bind-root #'gdx/app      (->application Gdx/app))
  (bind-root #'gdx/files    (->files       Gdx/files))
  (bind-root #'gdx/graphics (->graphics    Gdx/graphics))
  (bind-root #'gdx/input    (->input       Gdx/input)))

(defn- application-listener [{:keys [create! dispose! render! resize!]}]
  (proxy [ApplicationAdapter] []
    (create []
      (set-context!)
      (when create! (create!)))

    (dispose []
      (when dispose! (dispose!)))

    (render []
      (when render! (render!)))

    (resize [width height]
      (when resize! (resize! width height)))))

(defn application! [{:keys [title
                            windowed-mode
                            foreground-fps
                            dock-icon]}
                    listener]
  (when (= SharedLibraryLoader/os Os/MacOsX)
    (.setIconImage (Taskbar/getTaskbar)
                   (.getImage (Toolkit/getDefaultToolkit)
                              (io/resource dock-icon)))
    (.set Configuration/GLFW_LIBRARY_NAME "glfw_async"))
  (Lwjgl3Application. (application-listener listener)
                      (doto (Lwjgl3ApplicationConfiguration.)
                        (.setTitle title)
                        (.setWindowedMode (:width  windowed-mode)
                                          (:height windowed-mode))
                        (.setForegroundFPS foreground-fps))))
