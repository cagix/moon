(ns cdq.draw.with-line-width
  (:require [cdq.graphics :as graphics]
            [cdq.gdx.graphics]))

(defn do!
  [ctx width draws]
  (cdq.gdx.graphics/with-line-width ctx width
    (fn []
      (graphics/handle-draws! ctx draws))))
