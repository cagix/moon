(ns cdq.graphics.draw.with-line-width
  (:require [cdq.graphics :as graphics]
            [clojure.gdx.graphics.shape-drawer :as sd]))

(defn do!
  [{:keys [graphics/shape-drawer]
    :as graphics}
   width
   draws]
  (sd/with-line-width shape-drawer width
    (graphics/draw! graphics draws)))
