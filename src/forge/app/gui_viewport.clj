(ns forge.app.gui-viewport
  (:require [clojure.gdx.graphics :as g]
            [forge.core :refer [bind-root
                                gui-viewport-width
                                gui-viewport-height
                                gui-viewport]]))

(defn create [[_ [width height]]]
  (bind-root gui-viewport-width  width)
  (bind-root gui-viewport-height height)
  (bind-root gui-viewport (g/fit-viewport width height (g/orthographic-camera))))

(defn resize [_ w h]
  (.update gui-viewport w h true))
