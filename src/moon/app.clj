(ns moon.app
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [gdl.app :as app]
            [gdl.graphics :refer [clear-screen]]
            [gdl.graphics.shape-drawer :as shape-drawer]
            [gdl.ui :as ui]
            [gdl.utils :refer [dispose]]
            [moon.assets :as assets]
            [moon.db :as db]
            [moon.graphics :as graphics]
            [moon.graphics.shape-drawer :as sd]
            [moon.screen :as screen]))

(defn- background-image [image-path]
  (fn []
    (ui/image->widget (graphics/image image-path)
                      {:fill-parent? true
                       :scaling :fill
                       :align :center})))

(declare ^:private sd-texture)

(defn- app-listener [config]
  (reify app/Listener
    (create [_]
      (assets/load        (:assets   config))
      (graphics/load!     (:graphics config))
      (let [{:keys [shape-drawer
                    shape-drawer-texture]} (shape-drawer/create graphics/batch)]
        (bind-root #'sd/sd shape-drawer)
        (bind-root #'sd-texture shape-drawer-texture))
      (ui/load!           (:ui       config))
      (screen/set-screens (:screens  config)
                          (background-image
                           (:background-image config))))

    (dispose [_]
      (dispose sd-texture)
      (assets/dispose)
      (graphics/dispose!)
      (ui/dispose!)
      (screen/dispose-all))

    (render [_]
      (clear-screen :black)
      (screen/render (screen/current)))

    (resize [_ dimensions]
      (graphics/resize! dimensions))))

(defn -main []
  (let [config (-> "app.edn" io/resource slurp edn/read-string)]
    (run! (comp require symbol #(str "moon." %))
          (:components config))
    (db/load! (:properties config))
    (app/start (:app config)
               (app-listener config))))
