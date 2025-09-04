(ns cdq.draw.with-line-width
  (:require [cdq.ctx :as ctx]
            [cdq.graphics.shape-drawer :as sd]))

(defn draw!
  [[_ width draws]
   {:keys [ctx/shape-drawer] :as ctx}]
  (sd/with-line-width shape-drawer width
    (fn []
      (ctx/handle-draws! ctx draws))))
