(ns moon.graphics.view
  (:require [gdl.graphics.batch :as batch]
            [moon.graphics.batch :refer [batch]]
            [moon.graphics.shape-drawer :as sd]))

(def ^:dynamic *unit-scale* 1)

(defn render [{:keys [viewport unit-scale]} draw-fn]
  (batch/draw-on batch
                 viewport
                 (fn []
                   (sd/with-line-width unit-scale
                     #(binding [*unit-scale* unit-scale]
                        (draw-fn))))))
