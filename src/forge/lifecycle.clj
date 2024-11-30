(ns forge.lifecycle
  (:require [clojure.gdx :as gdx]
            [clojure.gdx.graphics.color :as color]
            [clojure.gdx.utils :as utils :refer [clear-screen]]
            [clojure.java.io :as io]
            [forge.app :as app]
            [forge.app.systems]
            [forge.screens.editor :as editor]
            [forge.screens.main :as main]
            [forge.screens.map-editor :as map-editor]
            [forge.screens.minimap :as minimap]
            [forge.screens.world :as world]
            [forge.assets :as assets]
            [forge.assets.manager :as asset-manager]
            [forge.db :as db]
            [forge.graphics :as graphics]
            [forge.graphics.cursors :as cursors]
            [forge.stage :as stage]
            [forge.ui :as ui]
            [forge.utils.files :as files])
  (:import (com.badlogic.gdx.graphics Pixmap)))

(defn- make-cursors [cursors]
  (mapvals (fn [[file [hotspot-x hotspot-y]]]
             (let [pixmap (Pixmap. (gdx/internal-file (str "cursors/" file ".png")))
                   cursor (gdx/new-cursor pixmap hotspot-x hotspot-y)]
               (utils/dispose pixmap)
               cursor))
           cursors))

(defn- screens []
  {:screens/main-menu  (main/create)
   :screens/map-editor (map-editor/create)
   :screens/editor     (editor/create)
   :screens/minimap    (minimap/create)
   :screens/world      (world/screen)})

(defn create [{:keys [db
                      asset-folder
                      cursors
                      ui-skin-scale]}]
  (db/init db)
  (bind-root #'assets/manager (asset-manager/init
                               (files/search asset-folder
                                             [[com.badlogic.gdx.audio.Sound      #{"wav"}]
                                              [com.badlogic.gdx.graphics.Texture #{"png" "bmp"}]])))
  (bind-root #'cursors/cursors (make-cursors cursors))
  (graphics/init)
  (ui/load! ui-skin-scale)
  (bind-root #'app/screens (mapvals stage/create (screens)))
  (app/change-screen :screens/main-menu))

(defn dispose []
  (.dispose assets/manager)
  (run! utils/dispose (vals cursors/cursors))
  (graphics/dispose)
  (run! app/dispose (vals app/screens))
  (ui/dispose!))

(defn render []
  (clear-screen color/black)
  (app/render (app/current-screen)))

(defn resize [w h]
  (.update graphics/gui-viewport   w h true)
  (.update graphics/world-viewport w h))
