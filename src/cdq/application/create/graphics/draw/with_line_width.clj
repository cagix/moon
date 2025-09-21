(ns cdq.application.create.graphics.draw.with-line-width
  (:require [cdq.graphics :as graphics]
            [gdl.graphics.shape-drawer :as sd]))

(defn do!
  [{:keys [graphics/shape-drawer]
    :as graphics}
   width
   draws]
  (sd/with-line-width shape-drawer width
    (fn []
      (graphics/handle-draws! graphics draws))))
