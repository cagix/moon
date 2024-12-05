(ns forge.lifecycle
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [forge.app.assets]
            [forge.app.cached-map-renderer]
            [forge.app.cursors]
            [forge.app.default-font]
            [forge.app.db]
            [forge.app.gui-viewport]
            [forge.app.shape-drawer]
            [forge.app.screens]
            [forge.app.sprite-batch]
            [forge.app.vis-ui]
            [forge.app.world-viewport]
            [forge.core :refer :all])
  (:import (com.badlogic.gdx ApplicationAdapter)
           (com.badlogic.gdx.backends.lwjgl3 Lwjgl3Application
                                             Lwjgl3ApplicationConfiguration)
           (com.badlogic.gdx.utils SharedLibraryLoader)
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

(def ^:private mac? SharedLibraryLoader/isMac)

(defn- configure-lwjgl [{:keys [glfw-library-name glfw-check-thread0]}]
  (.set Configuration/GLFW_LIBRARY_NAME  glfw-library-name)
  (.set Configuration/GLFW_CHECK_THREAD0 glfw-check-thread0))

(defsystem create)
(defmethod create :default [_])

(defsystem destroy)
(defmethod destroy :default [_])

(defsystem render)
(defmethod render :default [_])

(defsystem resize)
(defmethod resize :default [_ w h])

(defmethods :app/assets
  (create [[_ folder]]
    (forge.app.assets/create folder))
  (destroy [_]
    (forge.app.assets/destroy)))

(defmethods :app/vis-ui
  (create [[_ skin-scale]]
    (forge.app.vis-ui/create skin-scale))
  (destroy [_]
    (forge.app.vis-ui/destroy)))

(defmethods :app/sprite-batch
  (create [_]
    (forge.app.sprite-batch/create))
  (destroy [_]
    (forge.app.sprite-batch/destroy)))

(defmethods :app/shape-drawer
  (create [_]
    (forge.app.shape-drawer/create))
  (destroy [_]
    (forge.app.shape-drawer/destroy)))

(defmethods :app/cursors
  (create [[_ data]]
    (forge.app.cursors/create data))
  (destroy [_]
    (forge.app.cursors/destroy)))

(defmethods :app/gui-viewport
  (create [[_ config]]
    (forge.app.gui-viewport/create config))
  (resize [_ w h]
    (forge.app.gui-viewport/resize w h)))

(defmethods :app/world-viewport
  (create [[_ config]]
    (forge.app.world-viewport/create config))
  (resize [_ w h]
    (forge.app.world-viewport/resize w h)))

(defmethods :app/cached-map-renderer
  (create [_]
    (forge.app.cached-map-renderer/create)))

(defmethods :app/screens
  (create [[_ config]]
    (forge.app.screens/create config))
  (destroy [_]
    (forge.app.screens/destroy))
  (render [_]
    (forge.app.screens/render)))

(defmethods :app/db
  (create [[_ config]]
    (forge.app.db/create config)))

(defmethods :app/default-font
  (create [[_ font]]
    (forge.app.default-font/create font))
  (destroy [_]
    (forge.app.default-font/destroy)))

(defn -main []
  (let [{:keys [components] :as config} (-> "app.edn" io/resource slurp edn/read-string)]
    (run! require (:requires config))
    (set-dock-icon (:dock-icon config))
    (when mac?
      (configure-lwjgl {:glfw-library-name "glfw_async"
                        :glfw-check-thread0 false}))
    (Lwjgl3Application. (proxy [ApplicationAdapter] []
                          (create  []    (run! create          components))
                          (dispose []    (run! destroy         components))
                          (render  []    (run! render          components))
                          (resize  [w h] (run! #(resize % w h) components)))
                        (lwjgl3-config (:lwjgl3 config)))))
