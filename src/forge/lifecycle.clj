(ns forge.lifecycle
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [forge.component :refer [defsystem defmethods]]
            [forge.core :refer [batch]]
            [forge.assets :as assets])
  (:import (com.badlogic.gdx Gdx ApplicationAdapter)
           (com.badlogic.gdx.backends.lwjgl3 Lwjgl3Application Lwjgl3ApplicationConfiguration)
           (com.badlogic.gdx.files FileHandle)
           (com.badlogic.gdx.graphics.g2d SpriteBatch)
           (com.badlogic.gdx.utils SharedLibraryLoader)
           (com.kotcrab.vis.ui VisUI VisUI$SkinScale)
           (com.kotcrab.vis.ui.widget Tooltip)
           (org.lwjgl.system Configuration)
           (java.awt Taskbar Toolkit)))

(defn- set-dock-icon [resource]
  (.setIconImage (Taskbar/getTaskbar)
                 (.getImage (Toolkit/getDefaultToolkit)
                            (io/resource resource))))

(defn- lwjgl3-config [{:keys [title fps width height]}]
  (doto (Lwjgl3ApplicationConfiguration.)
    (.setTitle title)
    (.setForegroundFPS fps)
    (.setWindowedMode width height)))

(def mac? SharedLibraryLoader/isMac)

(defn- configure-lwjgl [{:keys [glfw-library-name glfw-check-thread0]}]
  (.set Configuration/GLFW_LIBRARY_NAME  glfw-library-name)
  (.set Configuration/GLFW_CHECK_THREAD0 glfw-check-thread0))

(defn- recursively-search [folder extensions]
  (loop [[^FileHandle file & remaining] (.list (.internal Gdx/files folder))
         result []]
    (cond (nil? file)
          result

          (.isDirectory file)
          (recur (concat remaining (.list file)) result)

          (extensions (.extension file))
          (recur remaining (conj result (.path file)))

          :else
          (recur remaining result))))

(defn- asset-descriptons [folder]
  (for [[class exts] [[com.badlogic.gdx.audio.Sound      #{"wav"}]
                      [com.badlogic.gdx.graphics.Texture #{"png" "bmp"}]]
        file (map #(str/replace-first % folder "")
                  (recursively-search folder exts))]
    [file class]))

(defsystem create)
(defmethod create :default [_])

(defsystem dispose)
(defmethod dispose :default [_])

(defsystem render)
(defmethod render :default [_])

(defsystem resize)
(defmethod resize :default [_ w h])

(defmethods :app/assets
  (create [[_ folder]]
    (assets/load-all (asset-descriptons folder)))
  (dispose [_]
    (assets/dispose)))

(defmethods :app/vis-ui
  (create [[_ skin-scale]]
    ; app crashes during startup before VisUI/dispose and we do clojure.tools.namespace.refresh-> gui elements not showing.
    ; => actually there is a deeper issue at play
    ; we need to dispose ALL resources which were loaded already ...
    (when (VisUI/isLoaded)
      (VisUI/dispose))
    (VisUI/load (case skin-scale
                  :skin-scale/x1 VisUI$SkinScale/X1
                  :skin-scale/x2 VisUI$SkinScale/X2))
    (-> (VisUI/getSkin)
        (.getFont "default-font")
        .getData
        .markupEnabled
        (set! true))
    ;(set! Tooltip/DEFAULT_FADE_TIME (float 0.3))
    ;Controls whether to fade out tooltip when mouse was moved. (default false)
    ;(set! Tooltip/MOUSE_MOVED_FADEOUT true)
    (set! Tooltip/DEFAULT_APPEAR_DELAY_TIME (float 0)))
  (dispose [_]
    (VisUI/dispose)))

(defmethods :app/sprite-batch
  (create [_]
    (.bindRoot #'batch (SpriteBatch.)))
  (dispose [_]
    (SpriteBatch/.dispose batch)))

(defn -main []
  (let [{:keys [components] :as config} (-> "app.edn" io/resource slurp edn/read-string)]
    (run! require (:requires config))
    (set-dock-icon (:dock-icon config))
    (when mac?
      (configure-lwjgl {:glfw-library-name "glfw_async"
                        :glfw-check-thread0 false}))
    (Lwjgl3Application. (proxy [ApplicationAdapter] []
                          (create  []    (run! create          components))
                          (dispose []    (run! dispose         components))
                          (render  []    (run! render          components))
                          (resize  [w h] (run! #(resize % w h) components)))
                        (lwjgl3-config (:lwjgl3 config)))))
