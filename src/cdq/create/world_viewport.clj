(ns cdq.create.world-viewport
  (:require [clojure.gdx.graphics.camera :as camera]
            [clojure.gdx.utils.viewport :as viewport]))

(defn do!
  [{:keys [ctx/world-unit-scale]
    :as ctx}
   {:keys [width height]}]
  (assoc ctx :ctx/world-viewport (let [world-width  (* width world-unit-scale)
                                       world-height (* height world-unit-scale)]
                                   (viewport/fit world-width
                                                 world-height
                                                 (camera/orthographic :y-down? false
                                                                      :world-width world-width
                                                                      :world-height world-height)))))
