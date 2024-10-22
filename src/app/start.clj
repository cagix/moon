(ns app.start
  (:require [app.config :as config]
            [app.screens.editor :as property-editor]
            [app.screens.main :as main-menu]
            [app.screens.map-editor :as map-editor]
            [app.screens.world :as world-screen]
            [clojure.string :as str]
            [component.db :as db]
            [gdx.assets :as assets]
            [gdx.graphics :as g]
            [gdx.ui :as ui]
            [gdx.screen :as screen]
            [gdx.vis-ui :as vis-ui])
  (:import (com.badlogic.gdx ApplicationAdapter Gdx)
           (com.badlogic.gdx.audio Sound)
           (com.badlogic.gdx.backends.lwjgl3 Lwjgl3Application Lwjgl3ApplicationConfiguration)
           (com.badlogic.gdx.files FileHandle)
           (com.badlogic.gdx.graphics Color Texture)
           (com.badlogic.gdx.utils SharedLibraryLoader ScreenUtils)
           (java.awt Taskbar Toolkit)
           (org.lwjgl.system Configuration))
  #_(:gen-class))

(defn- background-image []
  (ui/image->widget (g/image config/screen-background)
                    {:fill-parent? true
                     :scaling :fill
                     :align :center}))

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

(defn- search-assets [folder]
  (for [[class exts] [[Sound #{"wav"}]
                      [Texture #{"png" "bmp"}]]
        file (map #(str/replace-first % folder "")
                  (recursively-search folder exts))]
    [file class]))

(defn- application-listener []
  (proxy [ApplicationAdapter] []
    (create []
      (assets/load (search-assets config/resources))
      (g/load! config/graphics)
      (vis-ui/load! config/skin-scale)
      (screen/set-screens! [(main-menu/create background-image)
                            (map-editor/create)
                            (property-editor/screen background-image)
                            (world-screen/create)])
      ((world-screen/start-game-fn :worlds/vampire)))

    (dispose []
      (assets/dispose)
      (g/dispose!)
      (vis-ui/dispose!)
      (screen/dispose-all!))

    (render []
      (ScreenUtils/clear Color/BLACK)
      (screen/render! (screen/current)))

    (resize [w h]
      (g/resize! [w h]))))

(defn- set-dock-icon [image-path]
  (let [toolkit (Toolkit/getDefaultToolkit)
        image (.getImage toolkit (clojure.java.io/resource image-path))
        taskbar (Taskbar/getTaskbar)]
    (.setIconImage taskbar image)))

(defn- lwjgl3-config [{:keys [title fps width height]}]
  (doto (Lwjgl3ApplicationConfiguration.)
    (.setTitle title)
    (.setForegroundFPS fps)
    (.setWindowedMode width height)))

(defn -main []
  (db/load! config/properties)
  (when SharedLibraryLoader/isMac
    (set-dock-icon config/dock-icon)
    (.set Configuration/GLFW_LIBRARY_NAME "glfw_async")
    (.set Configuration/GLFW_CHECK_THREAD0 false))
  (Lwjgl3Application. (application-listener)
                      (lwjgl3-config config/lwjgl3)))
