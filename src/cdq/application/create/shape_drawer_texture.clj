(ns cdq.application.create.shape-drawer-texture
  (:require cdq.graphics.color
            cdq.graphics.pixmap
            cdq.graphics.texture
            cdq.utils))

(defn create [_context]
  (let [pixmap (doto (cdq.graphics.pixmap/create 1 1 cdq.graphics.pixmap/format-RGBA8888)
                 (cdq.graphics.pixmap/set-color cdq.graphics.color/white)
                 (cdq.graphics.pixmap/draw-pixel 0 0))
        texture (cdq.graphics.texture/create pixmap)]
    (cdq.utils/dispose pixmap)
    texture))
