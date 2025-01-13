(ns cdq.graphics
  (:require clojure.graphics.color
            clojure.graphics.pixmap
            clojure.graphics.texture
            clojure.utils))

(defn white-pixel-texture [_context _config]
  (let [pixmap (doto (clojure.graphics.pixmap/create 1 1 clojure.graphics.pixmap/format-RGBA8888)
                 (clojure.graphics.pixmap/set-color clojure.graphics.color/white)
                 (clojure.graphics.pixmap/draw-pixel 0 0))
        texture (clojure.graphics.texture/create pixmap)]
    (clojure.utils/dispose pixmap)
    texture))

(defn resize-viewports [context width height]
  (com.badlogic.gdx.utils.viewport.Viewport/.update (:clojure.graphics/ui-viewport    context) width height true)
  (com.badlogic.gdx.utils.viewport.Viewport/.update (:clojure.graphics/world-viewport context) width height false))
