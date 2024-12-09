(ns forge.app.gui-viewport
  (:require [anvil.graphics :refer [gui-viewport-width gui-viewport-height gui-viewport]]
            [clojure.gdx.graphics :as g]
            [clojure.gdx.utils.viewport :as vp :refer [fit-viewport]]
            [clojure.utils :refer [bind-root]]))

(defn create [[_ [width height]]]
  (bind-root gui-viewport-width  width)
  (bind-root gui-viewport-height height)
  (bind-root gui-viewport (fit-viewport width height (g/orthographic-camera))))

(defn resize [_ w h]
  (vp/update gui-viewport w h :center-camera? true))
