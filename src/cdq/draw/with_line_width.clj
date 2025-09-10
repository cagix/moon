(ns cdq.draw.with-line-width
  (:require [cdq.graphics :as graphics]
            [clojure.earlygrey.shape-drawer :as sd]))

(defn do!
  [{:keys [ctx/shape-drawer] :as ctx}
   width draws]
  (sd/with-line-width shape-drawer width
    (fn []
      (graphics/handle-draws! ctx draws))))
