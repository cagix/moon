(ns moon.widgets.background-image
  (:require [gdl.ui :as ui]
            [moon.graphics.image :as img]))

(defn create []
  (ui/image->widget (img/image "images/moon_background.png")
                    {:fill-parent? true
                     :scaling :fill
                     :align :center}))
