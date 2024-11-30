(ns forge.lifecycle
  (:require [forge.app :as app]
            [forge.assets :as assets]
            [forge.db :as db]
            [forge.graphics :as graphics]
            [forge.stage :as stage]
            [forge.ui :as ui]))

(defn create [{:keys [db assets graphics ui]} screens first-screen]
  (db/init db)
  (assets/init assets)
  (graphics/init graphics)
  (ui/init ui)
  (bind-root #'app/screens (mapvals stage/create (screens)))
  (app/change-screen first-screen))

(defn dispose []
  (assets/dispose)
  (graphics/dispose)
  (run! app/dispose (vals app/screens))
  (ui/dispose))

(defn render []
  (graphics/clear-screen)
  (app/render-current-screen))

(defn resize [w h]
  (graphics/resize w h))
