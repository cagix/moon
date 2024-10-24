(ns moon.app
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [gdl.app :as app]
            [gdl.ui :as ui]
            [moon.assets :as assets]
            [moon.db :as db]
            [moon.graphics :as graphics]
            [moon.screen :as screen]))

(defn- background-image [config]
  (fn []
    (ui/image->widget (graphics/image (:background-image config))
                      {:fill-parent? true
                       :scaling :fill
                       :align :center})))

(defn- app-listener [config]
  (reify app/Listener
    (create [_]
      (assets/load (:assets config))
      (graphics/load! (:graphics config))
      (ui/load! (:ui config))
      (screen/set-screens! (:screens config)
                           (background-image config)))

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
  (let [config (-> "app.edn" io/resource slurp edn/read-string)]
    (run! (comp require symbol #(str "moon." %))
          (:components config))
    (db/load! (:properties config))
    (app/start (:app config)
               (app-listener config))))
