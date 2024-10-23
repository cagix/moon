(ns app.start
  (:require (app.screens [editor :as property-editor]
                         [main :as main-menu]
                         [map-editor :as map-editor]
                         [world :as world-screen])
            [clojure.java.io :as io]
            [clojure.string :as str]
            [component.db :as db]
            [gdx.assets :as assets]
            [gdx.graphics :as g]
            [gdx.screen :as screen]
            [gdx.ui :as ui])
  (:import (com.badlogic.gdx ApplicationAdapter Gdx)
           (com.badlogic.gdx.backends.lwjgl3 Lwjgl3Application Lwjgl3ApplicationConfiguration)
           (java.awt Taskbar Toolkit)
           (org.lwjgl.system Configuration)
           (com.badlogic.gdx.audio Sound)
           (com.badlogic.gdx.files FileHandle)
           (com.badlogic.gdx.graphics Texture)
           (com.badlogic.gdx.utils SharedLibraryLoader)))

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

(def ^:private cursors
  {:cursors/bag                   ["bag001"       [0   0]]
   :cursors/black-x               ["black_x"      [0   0]]
   :cursors/default               ["default"      [0   0]]
   :cursors/denied                ["denied"       [16 16]]
   :cursors/hand-before-grab      ["hand004"      [4  16]]
   :cursors/hand-before-grab-gray ["hand004_gray" [4  16]]
   :cursors/hand-grab             ["hand003"      [4  16]]
   :cursors/move-window           ["move002"      [16 16]]
   :cursors/no-skill-selected     ["denied003"    [0   0]]
   :cursors/over-button           ["hand002"      [0   0]]
   :cursors/sandclock             ["sandclock"    [16 16]]
   :cursors/skill-not-usable      ["x007"         [0   0]]
   :cursors/use-skill             ["pointer004"   [0   0]]
   :cursors/walking               ["walking"      [16 16]]})

(def ^:private graphics
  {:cursors cursors
   :default-font {:file "fonts/exocet/films.EXL_____.ttf"
                  :size 16
                  :quality-scaling 2}
   :views {:gui-view {:world-width 1440
                      :world-height 900}
           :world-view {:world-width 1440
                        :world-height 900
                        :tile-size 48}}})

(defn- background-image []
  (ui/image->widget (g/image "images/moon_background.png")
                    {:fill-parent? true
                     :scaling :fill
                     :align :center}))

(defn- application-listener []
  (proxy [ApplicationAdapter] []
    (create []
      (assets/load (search-assets "resources/"))
      (g/load! graphics)
      (ui/load! :skin-scale/x1)
      (screen/set-screens! [(main-menu/create background-image)
                            (map-editor/create)
                            (property-editor/screen background-image)
                            (world-screen/create)])
      ((world-screen/start-game-fn :worlds/vampire)))

    (dispose []
      (assets/dispose)
      (g/dispose!)
      (ui/dispose!)
      (screen/dispose-all!))

    (render []
      (g/clear-screen)
      (screen/render! (screen/current)))

    (resize [w h]
      (g/resize! [w h]))))

(defn- set-dock-icon [image-path]
  (let [toolkit (Toolkit/getDefaultToolkit)
        image (.getImage toolkit (io/resource image-path))
        taskbar (Taskbar/getTaskbar)]
    (.setIconImage taskbar image)))

(defn -main []
  (db/load! "properties.edn")
  (when SharedLibraryLoader/isMac
    (set-dock-icon "moon.png")
    (.set Configuration/GLFW_LIBRARY_NAME "glfw_async")
    (.set Configuration/GLFW_CHECK_THREAD0 false))
  (Lwjgl3Application. (application-listener)
                      (doto (Lwjgl3ApplicationConfiguration.)
                        (.setTitle "Moon")
                        (.setForegroundFPS 60)
                        (.setWindowedMode 1440 900))))
