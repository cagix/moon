(ns cdq.draw.with-line-width
  (:require [cdq.graphics :as graphics]
            [clojure.earlygrey.shape-drawer :as sd]))

(defn do!
  [[_ width draws]
   {:keys [ctx/shape-drawer] :as ctx}]
  (sd/with-line-width shape-drawer width
    (fn []
      (graphics/handle-draws! ctx draws))))
