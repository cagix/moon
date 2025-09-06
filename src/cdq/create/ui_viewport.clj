(ns cdq.create.ui-viewport
  (:require [clojure.gdx.graphics.camera :as camera]
            [clojure.gdx.utils.viewport :as viewport]))

(defn do! [{:keys [ctx/config] :as ctx}]
  (assoc ctx :ctx/ui-viewport (viewport/fit (:width  (:ui-viewport config))
                                            (:height (:ui-viewport config))
                                            (camera/orthographic))))
