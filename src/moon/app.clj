(ns moon.app
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [gdl.app :as app]
            [gdl.ui :as ui]
            [moon.assets :as assets]
            [moon.db :as db]
            [moon.graphics :as graphics]
            [moon.screen :as screen]
            moon.components))

(def ^:private config (-> "app.edn" io/resource slurp edn/read-string))

(defn- background-image []
  (ui/image->widget (graphics/image (:background-image config))
                    {:fill-parent? true
                     :scaling :fill
                     :align :center}))

(defn- app-listener []
  (reify app/Listener
    (create [_]
      (assets/load (:assets config))
      (graphics/load! (:graphics config))
      (ui/load! (:ui config))
      (screen/set-screens! (:screens config)
                           background-image))

    (dispose [_]
      (assets/dispose)
      (graphics/dispose!)
      (ui/dispose!)
      (screen/dispose-all!))

    (render [_]
      (screen/render! (screen/current)))

    (resize [_ dimensions]
      (graphics/resize! dimensions))))

(defn -main []
  (db/load! (:properties config))
  (app/start (:app config)
             (app-listener)))
