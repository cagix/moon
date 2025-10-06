(ns cdq.graphics.draw.with-line-width
  (:require [cdq.graphics.draws :as draws]
            [space.earlygrey.shape-drawer :as sd]))

(defn do!
  [{:keys [graphics/shape-drawer]
    :as graphics}
   width
   draws]
  (sd/with-line-width shape-drawer width
    (draws/handle! graphics draws)))
