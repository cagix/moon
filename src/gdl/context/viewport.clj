(ns gdl.context.viewport
  (:require [clojure.gdx.graphics.camera :as camera]
            [clojure.gdx.utils.viewport :as viewport]
            [gdl.context :as ctx]))

(defn setup [{:keys [width height]}]
  (bind-root ctx/viewport-width  width)
  (bind-root ctx/viewport-height height)
  (bind-root ctx/viewport (viewport/fit width height (camera/orthographic))))
