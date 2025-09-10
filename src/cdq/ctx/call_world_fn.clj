(ns cdq.ctx.call-world-fn
  (:require [clojure.gdx.utils.disposable :as disposable]))

(defn do!
  [ctx
   {:keys [tiled-map
           start-position]}]
  (assert tiled-map)
  (assert start-position)

  ; TODO make separate fn dispose world
  ; which is called @ dev menu restart
  ; and not in pipeline of gdx app create
  (when-let [tiled-map (:world/tiled-map (:ctx/world ctx))]
    (disposable/dispose! tiled-map))
  ;

  (assoc ctx :ctx/world {:world/tiled-map tiled-map
                         :world/start-position start-position}))
