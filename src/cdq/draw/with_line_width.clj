(ns cdq.draw.with-line-width
  (:require [cdq.graphics :as graphics]
            [space.earlygrey.shape-drawer :as sd]))

(defn do!
  [{:keys [graphics/shape-drawer]
    :as graphics}
   width
   draws]
  (sd/with-line-width shape-drawer width
    (fn []
      (graphics/handle-draws! graphics draws))))
