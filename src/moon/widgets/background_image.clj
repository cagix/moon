(ns moon.widgets.background-image
  (:require [gdl.graphics.image :as img]
            [gdl.ui :as ui]))

(defn create []
  (ui/image->widget (img/image "images/moon_background.png")
                    {:fill-parent? true
                     :scaling :fill
                     :align :center}))
