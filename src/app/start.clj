(ns ^:no-doc app.start
  (:require [app.screens.editor :as editor]
            [app.screens.main :as main]
            [app.screens.map-editor :as map-editor]
            [app.screens.minimap :as minimap]
            [app.world :as world]

            app.systems
            forge.entity.animation
            (mapgen generate uf-caves)

            [clojure.gdx :as gdx]
            [clojure.gdx.backends.lwjgl3 :as lwjgl3]
            [clojure.gdx.graphics.color :as color]
            [clojure.gdx.scene2d.stage :as stage]
            [clojure.gdx.utils :refer [mac? clear-screen]]
            [clojure.java.io :as io]

            [forge.app :as app]
            [forge.assets :as assets]
            [forge.graphics :as graphics]
            [forge.db :as db]
            [forge.graphics.cursors :as cursors]
            [forge.ui :as ui]
            [forge.utils :refer [mapvals]])
  (:import (com.badlogic.gdx ApplicationAdapter)
           (java.awt Taskbar Toolkit)
           (org.lwjgl.system Configuration)))

(defn- set-dock-icon [image-path]
  (let [toolkit (Toolkit/getDefaultToolkit)
        image (.getImage toolkit (io/resource image-path))
        taskbar (Taskbar/getTaskbar)]
    (.setIconImage taskbar image)))

(defrecord StageScreen [stage sub-screen]
  app/Screen
  (enter [_]
    (gdx/set-input-processor stage)
    (app/enter sub-screen))

  (exit [_]
    (gdx/set-input-processor nil)
    (app/exit sub-screen))

  (render [_]
    (stage/act stage)
    (app/render sub-screen)
    (stage/draw stage))

  (dispose [_]
    (gdx/dispose stage)
    (app/dispose sub-screen)))

(defn- stage-create [viewport batch]
  (proxy [com.badlogic.gdx.scenes.scene2d.Stage clojure.lang.ILookup] [viewport batch]
    (valAt
      ([id]
       (ui/find-actor-with-id (.getRoot this) id))
      ([id not-found]
       (or (ui/find-actor-with-id (.getRoot this) id)
           not-found)))))

(defn- stage-screen
  "Actors or screen can be nil."
  [{:keys [actors screen]}]
  (let [stage (stage-create graphics/gui-viewport graphics/batch)]
    (run! #(stage/add stage %) actors)
    (->StageScreen stage screen)))

(defn -main []
  (db/init :schema "schema.edn"
           :properties "properties.edn")
  (set-dock-icon "moon.png")
  (when mac?
    (.set Configuration/GLFW_LIBRARY_NAME "glfw_async")
    (.set Configuration/GLFW_CHECK_THREAD0 false))
  (lwjgl3/application (proxy [ApplicationAdapter] []
                        (create []
                          (assets/init)
                          (cursors/init)
                          (graphics/init)
                          (ui/load! :skin-scale/x1)
                          (.bindRoot #'app/screens
                                     (mapvals stage-screen
                                              {:screens/main-menu  (main/create)
                                               :screens/map-editor (map-editor/create)
                                               :screens/editor     (editor/create)
                                               :screens/minimap    (minimap/create)
                                               :screens/world      (world/screen)}))
                          (app/change-screen :screens/main-menu))

                        (dispose []
                          (assets/dispose)
                          (cursors/dispose)
                          (graphics/dispose)
                          (run! app/dispose (vals app/screens))
                          (ui/dispose!))

                        (render []
                          (clear-screen color/black)
                          (app/render (app/current-screen)))

                        (resize [w h]
                          (.update graphics/gui-viewport   w h true)
                          (.update graphics/world-viewport w h)))
                      (lwjgl3/config {:title "Moon"
                                      :fps 60
                                      :width 1440
                                      :height 900})))
