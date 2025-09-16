(ns cdq.draw.with-line-width
  (:require [cdq.ctx.graphics :as graphics]
            [clojure.earlygrey.shape-drawer :as sd]))

(defn do!
  [{:keys [ctx/shape-drawer]
    :as graphics}
   width
   draws]
  (sd/with-line-width shape-drawer width
    (fn []
      (graphics/handle-draws! graphics draws))))
