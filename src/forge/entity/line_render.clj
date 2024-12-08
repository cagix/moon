(ns forge.entity.line-render
  (:require [forge.app.shape-drawer :as sd]))

(defn render-default [[_ {:keys [thick? end color]}] entity]
  (let [position (:position entity)]
    (if thick?
      (sd/with-line-width 4
        #(sd/line position end color))
      (sd/line position end color))))
