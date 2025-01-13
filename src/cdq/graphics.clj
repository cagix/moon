(ns cdq.graphics)

(defn resize-viewports [context width height]
  (com.badlogic.gdx.utils.viewport.Viewport/.update (:clojure.graphics/ui-viewport    context) width height true)
  (com.badlogic.gdx.utils.viewport.Viewport/.update (:clojure.graphics/world-viewport context) width height false))
