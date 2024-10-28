(ns moon.app
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [gdl.app :as app]
            [gdl.graphics :refer [clear-screen]]
            [gdl.graphics.shape-drawer :as shape-drawer]
            [gdl.graphics.viewport :as vp]
            [gdl.ui :as ui]
            [gdl.utils :refer [dispose]]
            [moon.assets :as assets]
            [moon.component :as component]
            [moon.graphics :as graphics]
            [moon.graphics.cursors :as cursors]
            [moon.graphics.gui-view :as gui-view]
            [moon.graphics.shape-drawer :as sd]
            [moon.graphics.tiled :as tiled-map-renderer]
            [moon.graphics.world-view :as world-view]
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
      (gui-view/init   (:gui-view   (:views (:graphics config))))
      (world-view/init (:world-view (:views (:graphics config))))
      (tiled-map-renderer/init)
      (cursors/init (:cursors (:graphics config)))
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
      (cursors/dispose)
      (assets/dispose)
      (graphics/dispose!)
      (ui/dispose!)
      (screen/dispose-all))

    (render [_]
      (clear-screen :black)
      (screen/render (screen/current)))

    (resize [_ dimensions]
      (vp/update (gui-view/viewport)   dimensions :center-camera? true)
      (vp/update (world-view/viewport) dimensions))))

(defn- load-components [components]
  (doseq [component components
          :let [[ns-sym v] (if (symbol? component)
                             [component nil]
                             component)]]
    (require (symbol (str "moon." ns-sym)))
    (component/on-load [(keyword (str "moon." (str ns-sym))) v])))

(defn -main []
  (let [config (-> "app.edn" io/resource slurp edn/read-string)]
    (load-components (:components config))
    (app/start (:app config)
               (app-listener config))))
