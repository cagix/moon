(ns forge.lifecycle
  (:require [clojure.gdx :as gdx]
            [clojure.gdx.graphics.color :as color]
            [clojure.gdx.utils :refer [clear-screen]]
            [clojure.java.io :as io]
            [clojure.string :as str]
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
            [forge.stage :as stage]
            [forge.ui :as ui]
            [forge.utils.files :as files]))

(defn- search
  "Returns a collection of `[file-path class]` after recursively searching `folder` and matching file extensions with class as of `asset-description`, a collcetion of `[class file-extensions-set]`."
  [folder asset-description]
  (for [[class exts] asset-description
        file (map #(str/replace-first % folder "")
                  (files/recursively-search (gdx/internal-file folder) exts))]
    [file class]))

(defn- screens []
  {:screens/main-menu  (main/create)
   :screens/map-editor (map-editor/create)
   :screens/editor     (editor/create)
   :screens/minimap    (minimap/create)
   :screens/world      (world/screen)})

(defn create [{:keys [db
                      asset-folder
                      graphics
                      ui-skin-scale]}]
  (db/init db)
  (bind-root #'assets/manager (asset-manager/init
                               (search asset-folder
                                       [[com.badlogic.gdx.audio.Sound      #{"wav"}]
                                        [com.badlogic.gdx.graphics.Texture #{"png" "bmp"}]])))
  (graphics/init graphics)
  (ui/load! ui-skin-scale)
  (bind-root #'app/screens (mapvals stage/create (screens)))
  (app/change-screen :screens/main-menu))

(defn dispose []
  (.dispose assets/manager)
  (graphics/dispose)
  (run! app/dispose (vals app/screens))
  (ui/dispose!))

(defn render []
  (clear-screen color/black)
  (app/render (app/current-screen)))

(defn resize [w h]
  (.update graphics/gui-viewport   w h true)
  (.update graphics/world-viewport w h))
