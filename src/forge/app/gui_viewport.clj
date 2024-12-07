(ns forge.app.gui-viewport
  (:require [clojure.gdx.graphics :as g]
            [clojure.gdx.utils.viewport :as vp :refer [fit-viewport]]
            [clojure.utils :refer [bind-root]]))

(declare gui-viewport
         gui-viewport-width
         gui-viewport-height)

(defn create [[_ [width height]]]
  (bind-root gui-viewport-width  width)
  (bind-root gui-viewport-height height)
  (bind-root gui-viewport (fit-viewport width height (g/orthographic-camera))))

(defn resize [_ w h]
  (vp/update gui-viewport w h :center-camera? true))

(defn gui-mouse-position []
  ; TODO mapv int needed?
  (mapv int (vp/unproject-mouse-position gui-viewport)))
