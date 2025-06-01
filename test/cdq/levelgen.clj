(ns cdq.levelgen
  (:require [cdq.create.assets]
            [cdq.create.db]
            [cdq.level.modules :as modules]
            [clojure.gdx.utils.disposable :as disposable]
            [gdl.graphics :as graphics]
            [gdl.graphics.color :as color]
            [gdl.graphics.tiled-map-renderer :as tm-renderer]
            [gdl.viewport :as viewport]))

(def tile-size 48)

(defrecord Context [])

(def state (atom nil))

(defn create! [config]
  (let [ctx (->Context)
        ctx (assoc ctx :ctx/config {:db {:schemas "schema.edn"
                                         :properties "properties.edn"}
                                    :assets {:folder "resources/"
                                             :asset-type-extensions {:texture #{"png" "bmp"}}}})
        ctx (cdq.create.db/do!     ctx)
        ctx (cdq.create.assets/do! ctx)
        level (modules/create ctx)
        world-unit-scale (float (/ tile-size))
        ctx (assoc ctx :ctx/world-viewport (graphics/world-viewport world-unit-scale
                                                                    {:width 1440
                                                                     :height 900}))
        tiled-map (:tiled-map level)
        batch (graphics/sprite-batch)
        tm-renderer (tm-renderer/create tiled-map
                                        world-unit-scale
                                        batch)
        ctx (assoc ctx
                   :ctx/tm-renderer tm-renderer
                   :ctx/tiled-map tiled-map)]
    (reset! state ctx)
    (println level)))

(defn dispose! []
  (let [{:keys [ctx/assets]} @state]
    (disposable/dispose! assets)))

(defn render! []
  (let [{:keys [ctx/tm-renderer
                ctx/tiled-map
                ctx/world-viewport]} @state]
    (tm-renderer/draw! tm-renderer
                       tiled-map
                       (constantly color/white)
                       (:camera world-viewport))))

(defn resize! [width height]
  (let [{:keys [ctx/world-viewport]} @state]
    (viewport/resize! world-viewport width height)))
