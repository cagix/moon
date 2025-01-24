(ns cdq.create.shape-drawer-texture
  (:require cdq.graphics.pixmap
            cdq.graphics.texture
            [clojure.gdx.graphics.color :as color]
            clojure.gdx.utils))

(defn create [_context]
  (let [pixmap (doto (cdq.graphics.pixmap/create 1 1 cdq.graphics.pixmap/format-RGBA8888)
                 (cdq.graphics.pixmap/set-color color/white)
                 (cdq.graphics.pixmap/draw-pixel 0 0))
        texture (cdq.graphics.texture/create pixmap)]
    (clojure.gdx.utils/dispose pixmap)
    texture))
