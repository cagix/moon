(ns cdq.render.tick-world
  (:require [cdq.stacktrace :as stacktrace]
            [cdq.ui.widget :as widget]
            [cdq.world :as world]
            [clojure.gdx.graphics :as graphics]
            [clojure.gdx.scenes.scene2d.stage :as stage]))

(defn- update-time
  [{:keys [ctx/graphics
           ctx/max-delta]
    :as ctx}]
  (let [delta-ms (min (graphics/delta-time graphics) max-delta)]
    (-> ctx
        (assoc :ctx/delta-time delta-ms)
        (update :ctx/elapsed-time + delta-ms))))

(defn update-potential-fields!
  [ctx]
  (world/update-potential-fields! ctx)
  ctx)

(defn- tick-entities!
  [{:keys [ctx/stage]
    :as ctx}]
  (try
   (world/tick-entities! ctx)
   (catch Throwable t
     (stacktrace/pretty-print t)
     (stage/add! stage (widget/error-window t))
     #_(bind-root ::error t)))
  ctx)

(defn do!
  [ctx]
  (if (:ctx/paused? ctx)
    ctx
    (-> ctx
        update-time
        update-potential-fields!
        tick-entities!)))
