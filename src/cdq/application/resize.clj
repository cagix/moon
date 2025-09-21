(ns cdq.application.resize
  (:require [cdq.application :as application]
            [cdq.graphics :as graphics]))

(defn- do!* [{:keys [ctx/graphics]} width height]
  (graphics/update-viewports! graphics width height))

(defn do! [width height]
  (do!* @application/state width height))
