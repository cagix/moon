(ns cdq.create.ui-viewport
  (:require [clojure.gdx.graphics.camera :as camera]
            [clojure.gdx.utils.viewport :as viewport]))

(def width 1440)
(def height 900)

(defn do! [ctx]
  (assoc ctx :ctx/ui-viewport (viewport/fit width height (camera/orthographic))))
