(ns ^:no-doc moon.widgets.background-image
  (:require [forge.ui :as ui]
            [forge.graphics :refer [image]]))

(defn create []
  (ui/image->widget (image "images/moon_background.png")
                    {:fill-parent? true
                     :scaling :fill
                     :align :center}))
