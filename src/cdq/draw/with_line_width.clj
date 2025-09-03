(ns cdq.draw.with-line-width
  (:require [cdq.graphics :as graphics]
            [cdq.gdx.graphics.shape-drawer :as sd]))

(defn draw! [[_ width draws]
             {:keys [shape-drawer] :as graphics}]
  (sd/with-line-width shape-drawer width
    (fn []
      (graphics/handle-draws! graphics draws))))
