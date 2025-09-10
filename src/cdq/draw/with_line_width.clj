(ns cdq.draw.with-line-width
  (:require [cdq.graphics :as graphics]
            [cdq.gdx.graphics]))

(defn do!
  [graphics width draws]
  (cdq.gdx.graphics/with-line-width graphics width
    (fn []
      (graphics/handle-draws! graphics draws))))
