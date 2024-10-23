(ns moon.app
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [gdl.app :as app]
            [gdl.ui :as ui]
            [moon.assets :as assets]
            moon.components
            [moon.db :as db]
            [moon.graphics :as g]
            [moon.screen :as screen]
            (moon.screens [editor :as property-editor]
                          [main :as main-menu]
                          [map-editor :as map-editor]
                          [world :as world-screen])))

(def ^:private config (-> "app.edn" io/resource slurp edn/read-string))

(defn- background-image []
  (ui/image->widget (g/image (:background-image config))
                    {:fill-parent? true
                     :scaling :fill
                     :align :center}))

(defn- app-listener []
  (reify app/Listener
    (create [_]
      (assets/load (:assets config))
      (g/load! (:graphics config))
      (ui/load! (:ui config))
      (screen/set-screens! [(main-menu/create background-image)
                            (map-editor/create)
                            (property-editor/screen background-image)
                            (world-screen/create)]))

    (dispose [_]
      (assets/dispose)
      (g/dispose!)
      (ui/dispose!)
      (screen/dispose-all!))

    (render [_]
      (screen/render! (screen/current)))

    (resize [_ dimensions]
      (g/resize! dimensions))))

(defn -main []
  (db/load! (:properties config))
  (app/start (:app config)
             (app-listener)))
