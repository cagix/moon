(ns cdq.create.ui-viewport
  (:require [clojure.gdx.graphics.camera :as camera]
            [clojure.gdx.utils.viewport :as viewport]))

(defn do! [ctx {:keys [width height]}]
  (assoc ctx :ctx/ui-viewport (viewport/fit width height (camera/orthographic))))
